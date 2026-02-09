package com.openlattice.chronicle.services.enrollment

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.openlattice.chronicle.ChronicleStudyApi
import com.openlattice.chronicle.constants.FirebaseAnalyticsEvents
import com.openlattice.chronicle.data.ParticipationStatus
import com.openlattice.chronicle.preferences.EnrollmentSettings
import com.openlattice.chronicle.preferences.PARTICIPANT_ID
import com.openlattice.chronicle.preferences.STUDY_ID
import com.openlattice.chronicle.services.upload.PRODUCTION
import com.openlattice.chronicle.utils.Utils
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val ENROLLMENT_MONITOR_INTERVAL_MIN = 15L
private const val UNIQUE_WORK_NAME = "enrollment_monitor"

private val TAG = EnrollmentMonitoringWorker::class.java.simpleName

/**
 * Periodically refreshes [ParticipationStatus] from the Chronicle API and persists it in [EnrollmentSettings].
 *
 * This worker intentionally has a narrow responsibility: fetch + persist status.
 */
class EnrollmentMonitoringWorker(
    context: Context,
    workerParameters: WorkerParameters
) : Worker(context, workerParameters) {

    private val chronicleApi =
        Utils.createRetrofitAdapter(PRODUCTION).create(ChronicleStudyApi::class.java)

    private lateinit var crashlytics: FirebaseCrashlytics
    private lateinit var analytics: FirebaseAnalytics
    private lateinit var settings: EnrollmentSettings
    private lateinit var studyId: UUID
    private lateinit var participantId: String

    override fun doWork(): Result {
        crashlytics = FirebaseCrashlytics.getInstance()
        analytics = Firebase.analytics

        return try {
            settings = EnrollmentSettings(applicationContext)
            studyId = settings.getStudyId()
            participantId = settings.getParticipantId()

            val participationStatus =
                chronicleApi.getParticipationStatus(studyId, participantId) ?: ParticipationStatus.UNKNOWN

            settings.setParticipationStatus(participationStatus)

            Log.i(TAG, "Updated participation status: $participationStatus")
            analytics.logEvent(FirebaseAnalyticsEvents.ENROLLMENT_MONITOR_SUCCESS, Bundle().apply {
                putString(PARTICIPANT_ID, participantId)
                putString(STUDY_ID, studyId.toString())
            })
            Result.success()
        } catch (e: Exception) {
            Log.i(TAG, "Enrollment monitoring failed", e)
            crashlytics.recordException(e)
            analytics.logEvent(FirebaseAnalyticsEvents.ENROLLMENT_MONITOR_FAILURE, Bundle().apply {
                // Best-effort; these may not be initialized if failure happened early.
                if (this@EnrollmentMonitoringWorker::participantId.isInitialized) {
                    putString(PARTICIPANT_ID, participantId)
                }
                if (this@EnrollmentMonitoringWorker::studyId.isInitialized) {
                    putString(STUDY_ID, studyId.toString())
                }
            })
            Result.failure()
        }
    }
}

fun scheduleEnrollmentMonitoringWork(context: Context) {
    val workRequest: PeriodicWorkRequest =
        PeriodicWorkRequestBuilder<EnrollmentMonitoringWorker>(
            ENROLLMENT_MONITOR_INTERVAL_MIN,
            TimeUnit.MINUTES
        ).build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        UNIQUE_WORK_NAME,
        ExistingPeriodicWorkPolicy.REPLACE,
        workRequest
    )
}

