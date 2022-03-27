package com.openlattice.chronicle.services.upload

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.room.Room
import androidx.work.*
import com.google.common.base.Stopwatch
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.ktx.Firebase
import com.openlattice.chronicle.constants.FirebaseAnalyticsEvents
import com.openlattice.chronicle.data.ParticipationStatus
import com.openlattice.chronicle.preferences.EnrollmentSettings
import com.openlattice.chronicle.preferences.getDevice
import com.openlattice.chronicle.preferences.getDeviceId
import com.openlattice.chronicle.serialization.JsonSerializer
import com.openlattice.chronicle.services.sinks.BrokerDataSink
import com.openlattice.chronicle.services.sinks.ConsoleSink
import com.openlattice.chronicle.services.sinks.OpenLatticeSink
import com.openlattice.chronicle.storage.ChronicleDb
import com.openlattice.chronicle.study.StudyApi
import com.openlattice.chronicle.utils.Utils.createRetrofitAdapter
import com.openlattice.chronicle.utils.Utils.setLastUpload
import java.util.*
import java.util.concurrent.TimeUnit

const val LAST_UPLOADED_PLACEHOLDER = "Never"
const val PRODUCTION = "http://192.168.1.64:8080"
const val BATCH_SIZE = 10
const val LAST_UPDATED_SETTING = "com.openlattice.chronicle.upload.LastUpdated"
const val UPLOAD_INTERVAL_MIN = 15L

val TAG = UploadWorker::class.java.simpleName

class UploadWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    private val limiter = com.google.common.util.concurrent.RateLimiter.create(10.0)

    private var studyApi = createRetrofitAdapter(PRODUCTION).create(StudyApi::class.java)

    private lateinit var crashlytics: FirebaseCrashlytics
    private lateinit var chronicleDb: ChronicleDb
    private lateinit var settings: EnrollmentSettings
    private lateinit var studyId: UUID
    private lateinit var participantId: String
    private lateinit var participationStatus: ParticipationStatus
    private lateinit var deviceId: String
    private lateinit var dataSink: BrokerDataSink
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun doWork(): Result {
        try {
            chronicleDb =
                Room.databaseBuilder(applicationContext, ChronicleDb::class.java, "chronicle")
                    .build()
            deviceId = getDeviceId(applicationContext)
            settings = EnrollmentSettings(applicationContext)
            firebaseAnalytics = Firebase.analytics
            crashlytics = FirebaseCrashlytics.getInstance()

            studyId = settings.getStudyId()
            participantId = settings.getParticipantId()
            participationStatus = settings.getParticipationStatus()

            dataSink = BrokerDataSink(
                mutableSetOf(
                    OpenLatticeSink(studyId, participantId, deviceId, studyApi),
                    ConsoleSink()
                )
            )

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

    private fun uploadData() {

        Log.i(TAG, "usage upload worker started")
        firebaseAnalytics.logEvent(FirebaseAnalyticsEvents.UPLOAD_START, null)

        // If studyApi.enroll(...) fails
        val chronicleId: UUID =
            studyApi.enroll(studyId, participantId, deviceId, getDevice(deviceId))
        Log.i(TAG, "deviceId: $chronicleId")

        //Only run the upload job if the device is already enrolled or we are able to properly enroll.
        val queue = chronicleDb.queueEntryData()
        var nextEntries = queue.getNextEntries(BATCH_SIZE)
        var notEmptied = nextEntries.isNotEmpty()
        while (notEmptied) {
            limiter.acquire()
            val w = Stopwatch.createStarted()
            val data = nextEntries
                .map { qe -> qe.data }
                .map { qe -> JsonSerializer.deserializeQueueEntry(qe) }
                .flatMap { it }
            Log.i(
                TAG,
                "Loading ${data.size} items from queue took ${w.elapsed(TimeUnit.MILLISECONDS)} millis"
            )
            w.reset()
            w.start()
            if (dataSink.submit(data)[OpenLatticeSink::class.java.name] == true) {
                setLastUpload(applicationContext)
                Log.i(
                    TAG,
                    "Successfully uploaded ${data.size} items in ${w.elapsed(TimeUnit.MILLISECONDS)} millis "
                )
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
    }
}


fun scheduleUploadWork(context: Context) {

    val workRequest: PeriodicWorkRequest =
        PeriodicWorkRequestBuilder<UploadWorker>(UPLOAD_INTERVAL_MIN, TimeUnit.MINUTES)
            .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "upload",
        ExistingPeriodicWorkPolicy.REPLACE,
        workRequest
    )
}
