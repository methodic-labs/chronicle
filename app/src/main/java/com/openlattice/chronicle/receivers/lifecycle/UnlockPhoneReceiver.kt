package com.openlattice.chronicle.receivers.lifecycle

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.openlattice.chronicle.R
import com.openlattice.chronicle.UserIdentificationActivity
import com.openlattice.chronicle.preferences.EnrollmentSettings
import com.openlattice.chronicle.services.notifications.CHANNEL_ID
import com.openlattice.chronicle.services.notifications.NOTIFICATION_DELETED_ACTION

class UnlockPhoneReceiver : BroadcastReceiver() {

    private lateinit var settings: EnrollmentSettings
    private lateinit var appContext: Context

    override fun onReceive(context: Context, intent: Intent) {

        appContext = context
        settings = EnrollmentSettings(context)
        if (!settings.isUserIdentificationEnabled()) {
            return
        }

        // show notification to prompt user to select current device user
        if (intent.action.equals(Intent.ACTION_USER_PRESENT)) {

            // only show notification if user has enabled setting
            val layout = RemoteViews(context.packageName, R.layout.notification)
            layout.setTextViewText(
                R.id.timestamp,
                DateUtils.formatDateTime(
                    context,
                    System.currentTimeMillis(),
                    DateUtils.FORMAT_SHOW_TIME
                )
            )
            layout.setTextViewText(
                R.id.notification_title,
                context.getString(R.string.on_wake_notification_title)
            )
            layout.setTextViewText(
                R.id.notification_message,
                context.getString(R.string.on_wake_notification_message)
            )

            // create intent to start UserIdentificationActivity
            val intent = Intent(context, UserIdentificationActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val pendingIntent =
                PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

            val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                .setCustomContentView(layout)
                .setCustomBigContentView(layout)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setDeleteIntent(createOnDismissedIntent())
                .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
                .setAutoCancel(true) // remove when user taps on notification

            with(NotificationManagerCompat.from(context)) {
                notify(1, notificationBuilder.build())
            }
        }
    }

    private fun createOnDismissedIntent(): PendingIntent {
        val intent = Intent(NOTIFICATION_DELETED_ACTION)
        val resources = appContext.resources
        val notificationId = resources.getInteger(R.integer.dismiss_target_user_notification_id)
        intent.putExtra(appContext.getString(R.string.notification_id), notificationId)

        return PendingIntent.getBroadcast(appContext, 0, intent, 0)
    }
}


