package com.openlattice.chronicle.services.usage

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.arch.persistence.room.Room
import android.content.ComponentName
import android.content.Context
import android.hardware.display.DisplayManager
import android.util.Log
import android.view.Display
import com.crashlytics.android.Crashlytics
import com.google.common.base.Stopwatch
import com.google.common.collect.ImmutableMap
import com.openlattice.chronicle.ChronicleApi
import com.openlattice.chronicle.sensors.ChronicleSensor
import com.openlattice.chronicle.sensors.PROPERTY_TYPES
import com.openlattice.chronicle.constants.Jobs.MONITOR_USAGE_JOB_ID
import com.openlattice.chronicle.sensors.UsageEventsChronicleSensor
import com.openlattice.chronicle.sensors.UsageStatsChronicleSensor
import com.openlattice.chronicle.serialization.JsonSerializer.serializeQueueEntry
import com.openlattice.chronicle.services.upload.PRODUCTION
import com.openlattice.chronicle.services.upload.createRetrofitAdapter
import com.openlattice.chronicle.storage.ChronicleDb
import com.openlattice.chronicle.storage.QueueEntry
import com.openlattice.chronicle.storage.StorageQueue
import com.openlattice.chronicle.utils.Utils.isJobServiceScheduled
import io.fabric.sdk.android.Fabric
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

const val UPLOAD_PERIOD_MILLIS = 15 * 60 * 1000L

class UsageMonitoringJobService : JobService() {
    private val executor = Executors.newSingleThreadExecutor()
    private val sw = Stopwatch.createStarted()
    private val latch = CountDownLatch(1);
    private val chronicleApi = createRetrofitAdapter(PRODUCTION).create(ChronicleApi::class.java)
    private val rand = Random()
    private val serviceId = rand.nextLong()

    private lateinit var propertyTypeIds: Map<String, UUID>
    private lateinit var chronicleDb: ChronicleDb
    private lateinit var storageQueue: StorageQueue
    private lateinit var sensors: Set<ChronicleSensor>

    override fun onCreate() {
        super.onCreate()
        Fabric.with(this, Crashlytics())
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        executor.execute {
            propertyTypeIds = getPropertyTypeIds()
            chronicleDb = Room.databaseBuilder(applicationContext, ChronicleDb::class.java, "chronicle").build()
            storageQueue = chronicleDb.queueEntryData()
            sensors = mutableSetOf(
                    // UsageStatsChronicleSensor(applicationContext),
                    UsageEventsChronicleSensor(applicationContext)
            )
            Log.i(javaClass.name, "Usage service is initialized")
            latch.countDown()
        }
        latch.await()
        Log.i(javaClass.name, "Usage service is running.")

        doWork()

        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        Log.i(javaClass.name, "Destroy requested after ${sw.elapsed(TimeUnit.SECONDS)}")
        executor.shutdown()
        executor.awaitTermination(1, TimeUnit.MINUTES)
        chronicleDb.close()
        Log.i(javaClass.name, "Usage events collection gracefully shutdown.")
        return true
    }

    private fun doWork() {
        if (propertyTypeIds == null) {
            propertyTypeIds = getPropertyTypeIds()
            if (propertyTypeIds == null) {
                return
            }
        }

        Log.d(javaClass.name, "Collecting Usage Information. Service ${serviceId} has been running for ${sw.elapsed(TimeUnit.SECONDS)} seconds.")

        //Since this is running on the main thread we shouldn't have worry that shutdown will be called
        if (!executor.isShutdown) {
            executor.execute {
                Log.i(javaClass.name, "Starting usage information collection. ")
                val w = Stopwatch.createStarted()
                val queueEntry = sensors
                        .flatMap { it.poll(propertyTypeIds) }
                        .filter { !it.isEmpty } //Filter out any empty write entries.
                queueEntry.asSequence().chunked(1000).forEach { chunk ->
                    storageQueue.insertEntry(QueueEntry(System.currentTimeMillis(), rand.nextLong(), serializeQueueEntry(chunk)))
                    Log.d(javaClass.name, "Persisting ${chunk.size} usage information elements took ${w.elapsed(TimeUnit.MILLISECONDS)} millis.")
                }
            }
        }
    }

    private fun getPropertyTypeIds(): Map<String, UUID> {
        return chronicleApi.getPropertyTypeIds(PROPERTY_TYPES) ?: ImmutableMap.of()
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
}

fun scheduleUsageMonitoringJob(context: Context) {
    if (!isJobServiceScheduled(context, MONITOR_USAGE_JOB_ID.id)) {
        val serviceComponent = ComponentName(context, UsageMonitoringJobService::class.java)
        val jobBuilder = JobInfo.Builder(MONITOR_USAGE_JOB_ID.id, serviceComponent)
        jobBuilder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        jobBuilder.setPeriodic(UPLOAD_PERIOD_MILLIS)
        jobBuilder.setPersisted(true)
        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        jobScheduler.schedule(jobBuilder.build())
    }
}

fun cancelUsageMonitoringJobScheduler(context :Context) {
    val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
    jobScheduler.cancel(MONITOR_USAGE_JOB_ID.id)
}
