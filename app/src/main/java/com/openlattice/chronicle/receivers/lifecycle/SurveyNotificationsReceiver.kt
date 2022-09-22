package com.openlattice.chronicle.receivers.lifecycle

import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.text.format.DateUtils
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.methodic.chronicle.R
import com.openlattice.chronicle.preferences.PARTICIPANT_ID
import com.openlattice.chronicle.preferences.STUDY_ID
import com.openlattice.chronicle.services.notifications.CHANNEL_ID
import com.openlattice.chronicle.services.notifications.NOTIFICATION_DETAILS
import com.openlattice.chronicle.services.notifications.NotificationDetails
import com.openlattice.chronicle.services.notifications.SURVEY_NOTIFICATION_ACTION
import com.openlattice.chronicle.utils.Utils.createNotificationTargetUrl
import com.openlattice.chronicle.utils.Utils.getPendingIntentMutabilityFlag

class SurveyNotificationsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action != SURVEY_NOTIFICATION_ACTION) {
            return
        }

        Log.i(javaClass.name, "Received survey notification intent")

        val participantId = intent.getStringExtra(PARTICIPANT_ID)
        val studyId = intent.getStringExtra(STUDY_ID)

        if (participantId.isNullOrBlank() || studyId.isNullOrBlank()) {
            return
        }
        val notificationEntry = intent.getStringExtra(NOTIFICATION_DETAILS)

        val notification = try {
            Gson().fromJson(
                notificationEntry,
                NotificationDetails::class.java
            )
        } catch (e: JsonSyntaxException) {
            Log.e(javaClass.name, "invalid json", e)
            null
        } ?: return

        // intent to launch survey in browser
        val targetUrl =
            createNotificationTargetUrl(notification, studyId, participantId)
        val notifyIntent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(context, 0, notifyIntent, getPendingIntentMutabilityFlag(PendingIntent.FLAG_UPDATE_CURRENT))

        // layout to use in custom notification
        val notificationLayout = RemoteViews(context.packageName, R.layout.notification)
        notificationLayout.setTextViewText(
            R.id.timestamp,
            DateUtils.formatDateTime(
                context,
                System.currentTimeMillis(),
                DateUtils.FORMAT_SHOW_TIME
            )
        )
        notificationLayout.setTextViewText(R.id.notification_title, notification.title)
        notificationLayout.setTextViewText(R.id.notification_message, notification.message)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .setCustomContentView(notificationLayout)
            .setCustomBigContentView(notificationLayout)
            .setPriority(NotificationCompat.PRIORITY_HIGH) //support android 7.1
            .setContentIntent(pendingIntent)
            .setDefaults(Notification.DEFAULT_VIBRATE)
            .setAutoCancel(true) // remove when user taps on notification

        val notificationSound: Uri =
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        builder.setSound(notificationSound)

        with(NotificationManagerCompat.from(context)) {
            notify(notification.hashCode(), builder.build())
        }
    }
}