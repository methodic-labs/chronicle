package com.openlattice.chronicle.services.upload

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.room.Room
import androidx.work.*
import com.google.common.base.Optional
import com.google.common.base.Stopwatch
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.ktx.Firebase
import com.openlattice.chronicle.ChronicleStudyApi
import com.openlattice.chronicle.api.ChronicleApi
import com.openlattice.chronicle.constants.FirebaseAnalyticsEvents
import com.openlattice.chronicle.data.ParticipationStatus
import com.openlattice.chronicle.preferences.EnrollmentSettings
import com.openlattice.chronicle.preferences.INVALID_ORG_ID
import com.openlattice.chronicle.preferences.getDevice
import com.openlattice.chronicle.preferences.getDeviceId
import com.openlattice.chronicle.serialization.JsonSerializer
import com.openlattice.chronicle.services.sinks.BrokerDataSink
import com.openlattice.chronicle.services.sinks.ConsoleSink
import com.openlattice.chronicle.services.sinks.OpenLatticeSink
import com.openlattice.chronicle.storage.ChronicleDb
import com.openlattice.chronicle.utils.Utils.createRetrofitAdapter
import com.openlattice.chronicle.utils.Utils.setLastUpload
import java.util.*
import java.util.concurrent.TimeUnit

const val LAST_UPLOADED_PLACEHOLDER = "Never"
const val PRODUCTION = "https://api.openlattice.com/"
const val BATCH_SIZE = 10 // 24 * 60 * 60 / 5 //17280
const val LAST_UPDATED_SETTING = "com.openlattice.chronicle.upload.LastUpdated"
const val UPLOAD_INTERVAL_MIN = 15L

val TAG = UploadWorker::class.java.simpleName

class UploadWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    private val limiter = com.google.common.util.concurrent.RateLimiter.create(10.0)

    private var chronicleApi = createRetrofitAdapter(PRODUCTION).create(ChronicleApi::class.java)
    private var legacyChronicleApi = createRetrofitAdapter(PRODUCTION).create(com.openlattice.chronicle.ChronicleApi::class.java)
    private var legacyChronicleStudyApi = createRetrofitAdapter(PRODUCTION).create(ChronicleStudyApi::class.java)

    private lateinit var crashlytics: FirebaseCrashlytics
    private lateinit var chronicleDb: ChronicleDb
    private lateinit var settings: EnrollmentSettings
    private lateinit var orgId: UUID
    private lateinit var studyId: UUID
    private lateinit var participantId: String
    private lateinit var participationStatus: ParticipationStatus
    private lateinit var deviceId: String
    private lateinit var dataSink: BrokerDataSink
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun doWork(): Result {
        try {
            chronicleDb = Room.databaseBuilder(applicationContext, ChronicleDb::class.java, "chronicle").build()
            deviceId = getDeviceId(applicationContext)
            settings = EnrollmentSettings(applicationContext)
            firebaseAnalytics = Firebase.analytics
            crashlytics = FirebaseCrashlytics.getInstance()

            orgId = settings.getOrganizationId()
            studyId = settings.getStudyId()
            participantId = settings.getParticipantId()
            participationStatus = settings.getParticipationStatus()

            dataSink = when (orgId) {
                INVALID_ORG_ID -> BrokerDataSink(
                        mutableSetOf(
                                OpenLatticeSink(studyId, participantId, deviceId, legacyChronicleApi),
                                ConsoleSink()))
                else -> BrokerDataSink(
                        mutableSetOf(
                                OpenLatticeSink(orgId, studyId, participantId, deviceId, chronicleApi),
                                ConsoleSink()))
            }

            uploadData()
            closeDb()
        } catch (e: Exception) {
            closeDb()
            crashlytics.recordException(e)
            firebaseAnalytics.logEvent(FirebaseAnalyticsEvents.UPLOAD_FAILURE, null)
            Log.i(TAG, "usage upload failed")
            return Result.failure()
        }

        return Result.success()
    }

    override fun onStopped() {
        super.onStopped()
        closeDb()
    }

    private fun closeDb() {
        if (this::chronicleDb.isInitialized && chronicleDb.isOpen) {
            chronicleDb.close()
        }
    }

    private fun enrollDevice() :UUID? {
        return if (orgId == INVALID_ORG_ID) {
            legacyChronicleStudyApi.enrollSource(studyId, participantId, deviceId, Optional.of(getDevice(deviceId)))
        } else {
            chronicleApi.enroll(orgId, studyId, participantId, deviceId, Optional.of(getDevice(deviceId)))
        }
    }

    private fun uploadData() {

        Log.i(TAG, "usage upload worker started")
        firebaseAnalytics.logEvent(FirebaseAnalyticsEvents.UPLOAD_START, null)

        if (participationStatus != ParticipationStatus.ENROLLED) {
            Log.i(TAG, "participant not enrolled. exiting data upload.")
            return
        }

        if (chronicleApi.isRunning == true) {

            val chronicleId: UUID? = enrollDevice()

            //Only run the upload job if the device is already enrolled or we are able to properly enroll.
            if (chronicleId != null) {

                val queue = chronicleDb.queueEntryData()
                var nextEntries = queue.getNextEntries(BATCH_SIZE)
                var notEmptied = nextEntries.isNotEmpty()
                while (notEmptied && chronicleApi.isRunning) {
                    limiter.acquire()
                    val w = Stopwatch.createStarted()
                    val data = nextEntries
                            .map { qe -> qe.data }
                            .map { qe -> JsonSerializer.deserializeQueueEntry(qe) }
                            .flatMap { it }
                    Log.i(TAG, "Loading ${data.size} items from queue took ${w.elapsed(TimeUnit.MILLISECONDS)} millis")
                    w.reset()
                    w.start()
                    if (dataSink.submit(data)[OpenLatticeSink::class.java.name] == true) {
                        setLastUpload(applicationContext)
                        Log.i(TAG, "Successfully uploaded ${data.size} items in ${w.elapsed(TimeUnit.MILLISECONDS)} millis ")
                        queue.deleteEntries(nextEntries)
                        nextEntries = queue.getNextEntries(BATCH_SIZE)
                        notEmptied = nextEntries.size == BATCH_SIZE

                        firebaseAnalytics.logEvent(FirebaseAnalyticsEvents.UPLOAD_SUCCESS, Bundle().apply {
                            putInt("size", data.size)
                        })

                    } else {
                        throw Exception("exception when uploading usage data")
                    }
                }
            } else {
                throw Exception("unable to enroll device")
            }
        }
    }
}


fun scheduleUploadWork(context: Context) {

    val workRequest: PeriodicWorkRequest =
            PeriodicWorkRequestBuilder<UploadWorker>(UPLOAD_INTERVAL_MIN, TimeUnit.MINUTES)
                    .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork("upload",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
    )
}
