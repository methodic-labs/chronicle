package com.openlattice.chronicle.services.usage

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.room.Room
import androidx.work.*
import com.google.common.base.Stopwatch
import com.google.common.collect.ImmutableMap
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.ktx.Firebase
import com.openlattice.chronicle.api.ChronicleApi
import com.openlattice.chronicle.data.ParticipationStatus
import com.openlattice.chronicle.preferences.EnrollmentSettings
import com.openlattice.chronicle.preferences.ORGANIZATION_ID
import com.openlattice.chronicle.preferences.PARTICIPANT_ID
import com.openlattice.chronicle.preferences.STUDY_ID
import com.openlattice.chronicle.sensors.ChronicleSensor
import com.openlattice.chronicle.sensors.PROPERTY_TYPES
import com.openlattice.chronicle.sensors.UsageEventsChronicleSensor
import com.openlattice.chronicle.serialization.JsonSerializer
import com.openlattice.chronicle.services.upload.PRODUCTION
import com.openlattice.chronicle.storage.ChronicleDb
import com.openlattice.chronicle.storage.QueueEntry
import com.openlattice.chronicle.storage.StorageQueue
import com.openlattice.chronicle.utils.Utils
import kotlinx.coroutines.coroutineScope
import org.apache.olingo.commons.api.edm.FullQualifiedName
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

val TAG = UsageMonitoringWorker::class.java.simpleName

class UsageMonitoringWorker(context: Context, workerParameters: WorkerParameters) : Worker(context, workerParameters) {
    private val sw = Stopwatch.createStarted()
    private val rand = Random()
    private val serviceId = rand.nextLong()

    private val crashlytics = FirebaseCrashlytics.getInstance()

    private var chronicleApi = Utils.createRetrofitAdapter(PRODUCTION).create(ChronicleApi::class.java)

    private lateinit var propertyTypeIds: Map<FullQualifiedName, UUID>
    private lateinit var chronicleDb: ChronicleDb
    private lateinit var storageQueue: StorageQueue
    private lateinit var sensors: Set<ChronicleSensor>
    private lateinit var settings: EnrollmentSettings

    override fun doWork(): Result {
        try {
            settings = EnrollmentSettings(applicationContext)
            propertyTypeIds = getPropertyTypeIds()
            chronicleDb = Room.databaseBuilder(applicationContext, ChronicleDb::class.java, "chronicle").build()
            storageQueue = chronicleDb.queueEntryData()
            sensors = mutableSetOf(
                    UsageEventsChronicleSensor(applicationContext))

            Log.i(TAG, "usage monitoring worker initialized")

            monitorUsage()

            closeDb()

        } catch (e: Exception) {
            crashlytics.recordException(e)
            Log.i(TAG, "usage monitoring worker failed with an exception")
            closeDb()
            return Result.failure()
        }
        return Result.success()
    }

    override fun onStopped() {
        super.onStopped()
        closeDb()
    }

    private fun closeDb() {
        if (chronicleDb.isOpen) {
            chronicleDb.close()
        }
    }

    private fun monitorUsage() {

        if (settings.getParticipationStatus() != ParticipationStatus.ENROLLED) {
            Log.i(TAG,"participant not enrolled. exiting usage monitoring")
            return
        }

        if (propertyTypeIds.isEmpty()) {
            return
        }

        Log.d(javaClass.name, "Collecting Usage Information. Service ${serviceId} has been running for ${sw.elapsed(TimeUnit.SECONDS)} seconds.")

        val w = Stopwatch.createStarted()
        val queueEntry = sensors
                .flatMap { it.poll(propertyTypeIds) }
                .filter { !it.isEmpty } //Filter out any empty write entries.
        queueEntry.asSequence().chunked(1000).forEach { chunk ->
            storageQueue.insertEntry(QueueEntry(System.currentTimeMillis(), rand.nextLong(), JsonSerializer.serializeQueueEntry(chunk)))
            Log.d(javaClass.name, "Persisting ${chunk.size} usage information elements took ${w.elapsed(TimeUnit.MILLISECONDS)} millis.")
        }
    }

    private fun getPropertyTypeIds(): Map<FullQualifiedName, UUID> {
        return chronicleApi.getPropertyTypeIds(PROPERTY_TYPES) ?: ImmutableMap.of()
    }
}


fun scheduleUsageMonitoringWork(context: Context) {

    val workRequest: PeriodicWorkRequest =
            PeriodicWorkRequestBuilder<UsageMonitoringWorker>(15, TimeUnit.MINUTES)
                    .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork("usage",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
    )
}


