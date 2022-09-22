package com.openlattice.chronicle.services.upload

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.room.Room
import androidx.work.*
import com.google.common.base.Stopwatch
import com.google.common.collect.SetMultimap
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.ktx.Firebase
import com.openlattice.chronicle.android.ChronicleSample
import com.openlattice.chronicle.android.ChronicleUsageEvent
import com.openlattice.chronicle.constants.FirebaseAnalyticsEvents
import com.openlattice.chronicle.data.ParticipationStatus
import com.openlattice.chronicle.models.ExtractedUsageEvent
import com.openlattice.chronicle.preferences.*
import com.openlattice.chronicle.sensors.*
import com.openlattice.chronicle.serialization.JsonSerializer
import com.openlattice.chronicle.services.sinks.BrokerDataSink
import com.openlattice.chronicle.services.sinks.ConsoleSink
import com.openlattice.chronicle.services.sinks.MethodicSink
import com.openlattice.chronicle.storage.ChronicleDb
import com.openlattice.chronicle.study.StudyApi
import com.openlattice.chronicle.utils.Utils.createRetrofitAdapter
import com.openlattice.chronicle.utils.Utils.setLastUpload
import org.apache.olingo.commons.api.edm.FullQualifiedName
import java.io.IOException
import java.time.OffsetDateTime
import java.util.*
import java.util.concurrent.TimeUnit

const val LAST_UPLOADED_PLACEHOLDER = "Never"
const val PRODUCTION = "https://api.getmethodic.com"
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
    private lateinit var propertyTypeIds: Map<FullQualifiedName, UUID>

    override fun doWork(): Result {
        try {
            chronicleDb =
                Room.databaseBuilder(applicationContext, ChronicleDb::class.java, "chronicle")
                    .build()
            deviceId = getDeviceId(applicationContext)
            settings = EnrollmentSettings(applicationContext)
            firebaseAnalytics = Firebase.analytics
            crashlytics = FirebaseCrashlytics.getInstance()
            propertyTypeIds = settings.getPropertyTypeIds()

            studyId = settings.getStudyId()
            participantId = settings.getParticipantId()
            participationStatus = settings.getParticipationStatus()

            dataSink = BrokerDataSink(
                mutableSetOf(
                    MethodicSink(studyId, participantId, deviceId, studyApi),
                    ConsoleSink()
                )
            )

            uploadData()
            closeDb()
        } catch (e: Exception) {
            closeDb()
            crashlytics.recordException(e)
            firebaseAnalytics.logEvent(FirebaseAnalyticsEvents.UPLOAD_FAILURE, Bundle().apply {
                putString(PARTICIPANT_ID, participantId)
                putString(STUDY_ID, studyId.toString())
            })
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
        firebaseAnalytics.logEvent(FirebaseAnalyticsEvents.UPLOAD_START, Bundle().apply {
            putString(PARTICIPANT_ID, participantId)
            putString(STUDY_ID, studyId.toString())
        })

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
                .map { qe ->
                    //Attempt to deserialize as legacy queue entry on exception. 
                    try {
                        JsonSerializer.deserializeQueueEntry(qe)
                    } catch (ex: IOException) {
                        mapLegacyQueueEntry(JsonSerializer.deserializeLegacyQueueEntry(qe))
                    }
                }
                .map { qe -> mapToModel(qe) }
                .flatten()
            Log.i(
                TAG,
                "Processing ${data.size} items from queue took ${w.elapsed(TimeUnit.MILLISECONDS)} millis"
            )
            w.reset()
            w.start()
            if (dataSink.submit(data)[MethodicSink::class.java.name] == true) {
                setLastUpload(applicationContext)
                Log.i(
                    TAG,
                    "Successfully uploaded ${data.size} items in ${w.elapsed(TimeUnit.MILLISECONDS)} millis "
                )
                queue.deleteEntries(nextEntries)
                nextEntries = queue.getNextEntries(BATCH_SIZE)
                notEmptied = nextEntries.size == BATCH_SIZE

                firebaseAnalytics.logEvent(FirebaseAnalyticsEvents.UPLOAD_SUCCESS, Bundle().apply {
                    putString(PARTICIPANT_ID, participantId)
                    putString(STUDY_ID, studyId.toString())
                    putInt("size", data.size)
                })

            } else {
                throw Exception("exception when uploading usage data")
            }
        }
    }

    private fun mapLegacyQueueEntry(data: List<SetMultimap<UUID, Any>>): List<ChronicleSample> {
        return data.mapNotNull { datum ->
            val appPackageName = getFirstValueOrNull(datum, GENERAL_NAME)
            val interactionType = getFirstValueOrNull(datum, IMPORTANCE)
            val timestamp = getFirstValueOrNull(datum, TIMESTAMP)
            val timezone = getFirstValueOrNull(datum, TIMEZONE)
            val user = getFirstValueOrNull(datum, USER)
            val applicationLabel = getFirstValueOrNull(datum, APP_NAME)

            if (appPackageName != null && interactionType != null && timestamp != null && timezone != null && user != null && applicationLabel != null) {
                ExtractedUsageEvent(
                    appPackageName,
                    interactionType,
                    OffsetDateTime.parse(timestamp),
                    timezone,
                    user,
                    applicationLabel
                )
            } else null
        }
    }

    private fun mapToModel(data: List<ChronicleSample>): List<ChronicleSample> {
        return data.mapNotNull { datum ->
            when (datum) {
                is ExtractedUsageEvent -> ChronicleUsageEvent(
                    studyId = studyId,
                    participantId = participantId,
                    appPackageName = datum.appPackageName,
                    applicationLabel = datum.applicationLabel,
                    timezone = datum.timezone,
                    timestamp = datum.timestamp,
                    user = datum.user,
                    interactionType = datum.interactionType
                )
                else -> null
            }
        }
    }

    private fun getFirstValueOrNull(
        entity: SetMultimap<UUID, Any>,
        fqn: FullQualifiedName
    ): String? {
        val ptId = propertyTypeIds.getValue(fqn)
        entity[ptId]?.iterator()?.let {
            if (it.hasNext()) return it.next().toString()
        }
        return null
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
