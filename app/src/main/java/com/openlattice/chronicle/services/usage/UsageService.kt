package com.openlattice.chronicle.services.usage

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.arch.persistence.room.Room
import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.common.base.Stopwatch
import com.openlattice.chronicle.ChronicleApi
import com.openlattice.chronicle.sensors.ActivityManagerChronicleSensor
import com.openlattice.chronicle.sensors.ChronicleSensor
import com.openlattice.chronicle.sensors.FQNS
import com.openlattice.chronicle.serialization.JsonSerializer
import com.openlattice.chronicle.services.upload.PRODUCTION
import com.openlattice.chronicle.services.upload.createRetrofitAdapter
import com.openlattice.chronicle.storage.ChronicleDb
import com.openlattice.chronicle.storage.QueueEntry
import com.openlattice.chronicle.storage.StorageQueue
import com.openlattice.chronicle.util.RetrofitBuilders
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import android.view.Display
import android.content.Context.DISPLAY_SERVICE
import android.hardware.display.DisplayManager
import android.os.Build
import com.openlattice.chronicle.receivers.lifecycle.USAGE_PERIOD_MILLIS
import com.openlattice.chronicle.sensors.UsageStatsChronicleSensor


const val LIFETIME = 60 * 1000
const val USAGE_SERVICE_JOB_ID = 1

class UsageService : JobService() {
    private val executor = Executors.newSingleThreadExecutor()
    private val sw = Stopwatch.createStarted()
    private val handler = Handler()
    private val latch = CountDownLatch(1);
    private val shutdownLatch = CountDownLatch(1);

    private var id = 0L
    private var initialized = false
    private var running = false;
    private lateinit var propertyTypeIds: Map<String, UUID>
    private lateinit var chronicleDb: ChronicleDb
    private lateinit var storageQueue: StorageQueue

    lateinit var sensors: Set<ChronicleSensor>

    override fun onStartJob(params: JobParameters?): Boolean {
        if (isScreenOff()) {
            return true
        }

        executor.execute {
            propertyTypeIds = createRetrofitAdapter(PRODUCTION).create(ChronicleApi::class.java).getPropertyTypeIds(FQNS)
            chronicleDb = Room.databaseBuilder(applicationContext, ChronicleDb::class.java, "chronicle").build()
            storageQueue = chronicleDb.queueEntryData()
            sensors = mutableSetOf<ChronicleSensor>(UsageStatsChronicleSensor(applicationContext))
            Log.i(javaClass.name, "Usage service is initialized")
            latch.countDown()
        }

        Log.i(javaClass.name, "Usage service is running.")
        running = true
        handler.post(this::doWork)
        jobFinished(params,false)
        return true
    }

    fun isScreenOff(): Boolean {
        val dm = applicationContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        for (display in dm.displays) {
            if (display.state == Display.STATE_ON) {
                return false
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        Log.i(javaClass.name, "Stop requested after ${sw.elapsed(TimeUnit.SECONDS)}")
        running = false
        chronicleDb.close()
        shutdownLatch.await()
        executor.shutdown()
        executor.awaitTermination(1, TimeUnit.MINUTES)
        Log.i(javaClass.name, "Usage collection gracefully shutdown.")
        scheduleUsageJob(applicationContext)
        return true
    }

    fun doWork() {
        //Wait until initialization of service finishes.
        if (!initialized) {
            latch.await()
            initialized = true
        }

        if (running) {
            executor.execute {
                Log.i(javaClass.name, "Starting usage information collection. ")
                val w = Stopwatch.createStarted()
                sensors
                        .map { it.poll(propertyTypeIds) }
                        .forEach { storageQueue.insertEntry(QueueEntry(System.currentTimeMillis(), id++, JsonSerializer.serializeQueueEntry(it))) }
                Log.d(javaClass.name, "Persisting usage information took . Sampling a single entry took ${w.elapsed(TimeUnit.MILLISECONDS)} millis.")
            }

            Log.i(javaClass.name, "Collecting Usage Information. This service has been running for ${sw.elapsed(TimeUnit.SECONDS)} seconds.")

            handler.postDelayed(this::doWork, 1000)
        } else {
            shutdownLatch.countDown()
        }
    }
}

fun scheduleUsageJob(context: Context) {
    val serviceComponent = ComponentName(context, UsageService::class.java)
    val jobBuilder = JobInfo.Builder(USAGE_SERVICE_JOB_ID, serviceComponent)
    jobBuilder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
    jobBuilder.setPersisted(true)
    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        jobBuilder.setOverrideDeadline(1000)
    } else {
        jobBuilder.setPeriodic(USAGE_PERIOD_MILLIS)
    }

    val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
    jobScheduler.schedule(jobBuilder.build())
}
