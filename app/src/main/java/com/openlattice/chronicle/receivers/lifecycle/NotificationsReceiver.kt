package com.openlattice.chronicle.receivers.lifecycle

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.content.ContextCompat
import android.text.format.DateUtils
import android.widget.RemoteViews
import com.openlattice.chronicle.R
import com.openlattice.chronicle.preferences.EnrollmentSettings
import com.openlattice.chronicle.services.notifications.CHANNEL_ID

const val NOTIFICATION_ID = 5;

class NotificationsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        val enrollmentSettings = EnrollmentSettings(context)

        val participantId = enrollmentSettings.getParticipantId()
        val studyId = enrollmentSettings.getStudyId().toString()

        // create url based on this intent
        val uriBuilder: Uri.Builder = Uri.Builder()
                .scheme("https")
                .encodedAuthority("openlattice.com")
                .appendPath("chronicle")
                .appendEncodedPath("#")
                .appendPath("survey")
                .appendQueryParameter("studyId", studyId)
                .appendQueryParameter("participantId", participantId)

        // intent to launch survey in browser
        val notifyIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uriBuilder.build().toString()))
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        // layout to use in custom notification
        val notificationLayout = RemoteViews(context.packageName, R.layout.notification)
        notificationLayout.setTextViewText(R.id.timestamp, DateUtils.formatDateTime(context, System.currentTimeMillis(), DateUtils.FORMAT_SHOW_TIME))

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setColor(ContextCompat.getColor(context, R.color.purple_dark))
                .setCustomContentView(notificationLayout)
                .setCustomBigContentView(notificationLayout)
                .setPriority(NotificationCompat.PRIORITY_HIGH) //support android 7.1
                .setContentIntent(pendingIntent)
                .setDefaults(Notification.DEFAULT_VIBRATE)
                .setAutoCancel(true) // remove when user taps on notification

        val notificationSound: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        builder.setSound(notificationSound)

        with(NotificationManagerCompat.from(context)) {
            notify(NOTIFICATION_ID, builder.build())
        }
    }
}