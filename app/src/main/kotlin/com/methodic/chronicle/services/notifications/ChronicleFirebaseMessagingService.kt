package com.methodic.chronicle.services.notifications

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessagingService
import com.openlattice.chronicle.preferences.EnrollmentSettings
import com.openlattice.chronicle.preferences.getDevice
import com.openlattice.chronicle.preferences.getDeviceId
import com.openlattice.chronicle.services.upload.PRODUCTION
import com.openlattice.chronicle.services.upload.TAG
import com.openlattice.chronicle.study.StudyApi
import com.openlattice.chronicle.utils.Utils
import java.util.UUID

class ChronicleFirebaseMessagingService : FirebaseMessagingService() {
    val studyApi = Utils.createRetrofitAdapter(PRODUCTION).create(StudyApi::class.java)
    val deviceId = getDeviceId(applicationContext)
    val settings = EnrollmentSettings(applicationContext)
    private val crashlytics = FirebaseCrashlytics.getInstance()

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // FCM registration token to your app server.
        if (settings.isEnrolled())
            sendRegistrationToServer(token)
        else
            Log.w(TAG,"Received new token while not enrolled.")
    }

    private fun sendRegistrationToServer(token: String) {
        var chronicleId: UUID? = null
        val studyId = settings.getStudyId()
        val participantId = settings.getParticipantId()
        try {
            //Generally okay if this calls fails as enroll is called on every upload as well.
            chronicleId = studyApi.enroll(
                studyId,
                participantId,
                deviceId,
                getDevice(deviceId, token)
            )
        } catch (e: Exception) {
            crashlytics.log("Exception updating fcm token - studyId: \"$studyId\" ; participantId: \"$participantId\"")
            FirebaseCrashlytics.getInstance().recordException(e)
        }
    }
}

