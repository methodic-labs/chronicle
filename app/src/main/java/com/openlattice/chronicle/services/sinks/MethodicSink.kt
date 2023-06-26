package com.openlattice.chronicle.services.sinks

import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.openlattice.chronicle.android.ChronicleData
import com.openlattice.chronicle.android.ChronicleSample
import com.methodic.chronicle.constants.FirebaseAnalyticsEvents
import com.openlattice.chronicle.preferences.PARTICIPANT_ID
import com.openlattice.chronicle.preferences.STUDY_ID
import com.openlattice.chronicle.study.StudyApi
import java.util.*

class MethodicSink(
    private val studyId: UUID,
    private val participantId: String,
    private val deviceId: String,
    private val studyApi: StudyApi,
    private val crashlytics: FirebaseCrashlytics,
    private val firebaseAnalytics: FirebaseAnalytics,
) : DataSink {
    override fun submit(data: List<ChronicleSample>): Map<String, Boolean> {
        val written = try {
            studyApi.uploadAndroidUsageEventData(
                studyId,
                participantId,
                deviceId,
                ChronicleData(data)
            )
        } catch (e: Exception) {
            crashlytics.recordException(e)
            firebaseAnalytics.logEvent(FirebaseAnalyticsEvents.SUBMIT_FAILURE, Bundle().apply {
                putString(PARTICIPANT_ID, participantId)
                putString(STUDY_ID, studyId.toString())
            })
            Log.i(javaClass.name, "Exception when uploading data", e)
            0
        }
        return mapOf(
            MethodicSink::class.java.name to (written > 0)
        )
    }
}