package com.methodic.chronicle.services.notifications


import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.openlattice.chronicle.MainActivity
import com.openlattice.chronicle.R
import com.openlattice.chronicle.preferences.EnrollmentSettings
import com.openlattice.chronicle.preferences.getDevice
import com.openlattice.chronicle.preferences.getDeviceId
import com.openlattice.chronicle.services.upload.PRODUCTION
import com.openlattice.chronicle.services.upload.TAG
import com.openlattice.chronicle.study.StudyApi
import com.openlattice.chronicle.utils.Utils
import java.util.UUID



class ChronicleFirebaseMessagingService : FirebaseMessagingService() {
    private val studyApi = Utils.createRetrofitAdapter(PRODUCTION).create(StudyApi::class.java)
    private val crashlytics = FirebaseCrashlytics.getInstance()

    override fun onNewToken(token: String) {
        val settings = EnrollmentSettings(applicationContext)
        Log.d(TAG, "Refreshed token: $token")

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // FCM registration token to your app server.
        if (settings.isEnrolled())
            sendRegistrationToServer(token)
        else
            Log.w(TAG, "Received new token while not enrolled.")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

        val notificationSpec = NotificationCompat.Builder(this.applicationContext, getString(R.string.channel_name))
            .setContentText("Uploads do not seem to be active.")
            .setSmallIcon(R.drawable.ic_ol_icon)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .addAction(R.drawable.ic_ol_icon, "Make Active", pendingIntent)
            .setAutoCancel(true)
        val notificationMgr = NotificationManagerCompat.from(applicationContext)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Shouldn't be possible because notification permissions are required.")
            return
        }
        notificationMgr.notify(1, notificationSpec.build())

    }

    private fun sendRegistrationToServer(token: String) {
        val deviceId = getDeviceId(applicationContext)
        val settings = EnrollmentSettings(applicationContext)
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

