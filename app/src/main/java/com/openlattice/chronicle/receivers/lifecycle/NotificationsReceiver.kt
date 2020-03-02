package com.openlattice.chronicle.receivers.lifecycle

import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import com.openlattice.chronicle.R
import com.openlattice.chronicle.preferences.EnrollmentSettings
import com.openlattice.chronicle.preferences.PARTICIPANT_ID
import com.openlattice.chronicle.preferences.STUDY_ID
import com.openlattice.chronicle.services.notifications.CHANNEL_ID


class NotificationsReceiver : BroadcastReceiver () {
    override fun onReceive(context: Context, intent: Intent) {

        var enrollmentSettings = EnrollmentSettings(context)

        val participantId = enrollmentSettings.getParticipantId()
        val studyId = enrollmentSettings.getStudyId().toString()

        // create url based on this intent
        var uriBuilder :Uri.Builder = Uri.Builder()
                .scheme("https")
                .encodedAuthority("openlattice.com")
                .appendPath("chronicle")
                .appendEncodedPath("#")
                .appendPath("survey")
                .appendQueryParameter("studyId", studyId)
                .appendQueryParameter("participantId", participantId)


        // intent to launch survey in browser
        val notifyIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uriBuilder.build().toString()))
        val pendingIntent :PendingIntent = PendingIntent.getActivity(context, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setContentText("Tap to complete survey")
                .setContentTitle("Chronicle Survey")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT) //support android 7.1
                .setContentIntent(pendingIntent)
                .setDefaults(Notification.DEFAULT_VIBRATE)
                .setAutoCancel(true) // remove when user taps on notification

        val notificationSound :Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        builder.setSound(notificationSound)


        // Ideally the id should be unique for each notification, but since we are not updating / removing the notification later, we can use a generic Id
        with(NotificationManagerCompat.from(context)) {
            notify(1, builder.build())
        }
    }

}