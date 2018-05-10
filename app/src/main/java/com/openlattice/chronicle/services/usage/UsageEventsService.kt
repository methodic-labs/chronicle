package com.openlattice.chronicle.services.usage

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.arch.persistence.room.Room
import android.content.ComponentName
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Handler
import android.util.Log
import android.view.Display
import com.google.common.base.Stopwatch
import com.google.common.collect.ImmutableMap
import com.openlattice.chronicle.ChronicleApi
import com.openlattice.chronicle.sensors.ChronicleSensor
import com.openlattice.chronicle.sensors.PROPERTY_TYPES
import com.openlattice.chronicle.sensors.UsageEventsChronicleSensor
import com.openlattice.chronicle.sensors.UsageStatsChronicleSensor
import com.openlattice.chronicle.serialization.JsonSerializer
import com.openlattice.chronicle.services.upload.PRODUCTION
import com.openlattice.chronicle.services.upload.createRetrofitAdapter
import com.openlattice.chronicle.storage.ChronicleDb
import com.openlattice.chronicle.storage.QueueEntry
import com.openlattice.chronicle.storage.StorageQueue
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

const val USAGE_SERVICE_JOB_ID = 1
const val USAGE_PERIOD_MILLIS = 60 * 1000L //This is how long the service will run in the background before re-scheduling itself
const val POLL_INTERVAL = 5 * 1000L  //This is how long frequently the service will poll.

class UsageEventsService : JobService() {
    private val executor = Executors.newSingleThreadExecutor()
    private val sw = Stopwatch.createStarted()
    private val handler = Handler()
    private val latch = CountDownLatch(1);
    private val chronicleApi = createRetrofitAdapter(PRODUCTION).create(ChronicleApi::class.java)
    private val rand = Random()
    private val serviceId = rand.nextLong()
    private val startTime = System.currentTimeMillis()

    private lateinit var propertyTypeIds: Map<String, UUID>
    private lateinit var chronicleDb: ChronicleDb
    private lateinit var storageQueue: StorageQueue
    private lateinit var sensors: Set<ChronicleSensor>

    override fun onStartJob(params: JobParameters?): Boolean {
        if (isScreenOff()) {
            jobFinished(params, false)
            return true
        }

        executor.execute {
            propertyTypeIds = getPropertyTypeIds()
            chronicleDb = Room.databaseBuilder(applicationContext, ChronicleDb::class.java, "chronicle").build()
            storageQueue = chronicleDb.queueEntryData()
            sensors = mutableSetOf(
                    UsageStatsChronicleSensor(applicationContext),
                    UsageEventsChronicleSensor(applicationContext)
            )
            Log.i(javaClass.name, "Usage service is initialized")
            latch.countDown()
        }

        latch.await()

        if (propertyTypeIds == null) {
            propertyTypeIds = getPropertyTypeIds()
            if (propertyTypeIds == null) {
                //Server was unable do exponential backoff
                jobFinished(params, true)
                return false;
            }
        }

        Log.i(javaClass.name, "Usage service is running.")
        doWork(params)
        return true
    }

    private fun isScreenOff(): Boolean {
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
        executor.shutdown()
        executor.awaitTermination(1, TimeUnit.MINUTES)
        chronicleDb.close()
        Log.i(javaClass.name, "Usage collection gracefully shutdown.")
        scheduleUsageEventsJob(applicationContext)
        Log.i(javaClass.name, "Schedule next events usage job.")
        return true
    }

    private fun doWork(params: JobParameters?) {
        Log.d(javaClass.name, "Collecting Usage Information. Service ${serviceId} has been running for ${sw.elapsed(TimeUnit.SECONDS)} seconds.")

        //Since this is running on the main thread we shouldn't have worry that shutdown will be called
        if (!executor.isShutdown) {
            executor.execute {
                Log.i(javaClass.name, "Starting usage information collection. ")
                val w = Stopwatch.createStarted()
                sensors
                        .map { it.poll(propertyTypeIds) }
                        .filter { it.isNotEmpty() }
                        .forEach {
                            storageQueue.insertEntry(QueueEntry(System.currentTimeMillis(), rand.nextLong(), JsonSerializer.serializeQueueEntry(it)))
                        }
                Log.d(javaClass.name, "Persisting usage information took ${w.elapsed(TimeUnit.MILLISECONDS)} millis.")
                if ((System.currentTimeMillis() - startTime) < USAGE_PERIOD_MILLIS) {
                    handler.postDelayed({ doWork(params) }, POLL_INTERVAL)
                } else {
                    handler.post {
                        chronicleDb.close()
                        jobFinished(params, false)
                    }
                }
            }
        }
    }

    private fun getPropertyTypeIds(): Map<String, UUID> {
        return chronicleApi.getPropertyTypeIds(PROPERTY_TYPES) ?: ImmutableMap.of()
    }
}

fun scheduleUsageEventsJob(context: Context) {
    val serviceComponent = ComponentName(context, UsageEventsService::class.java)
    val jobBuilder = JobInfo.Builder(USAGE_SERVICE_JOB_ID, serviceComponent)
    jobBuilder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
    jobBuilder.setPersisted(true)
    jobBuilder.setOverrideDeadline(POLL_INTERVAL)
    val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
    jobScheduler.schedule(jobBuilder.build())
}

