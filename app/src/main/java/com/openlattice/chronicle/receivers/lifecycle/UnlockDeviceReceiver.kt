package com.openlattice.chronicle.receivers.lifecycle

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.openlattice.chronicle.R
import com.openlattice.chronicle.UserIdentificationActivity
import com.openlattice.chronicle.preferences.EnrollmentSettings
import com.openlattice.chronicle.services.notifications.CHANNEL_ID
import com.openlattice.chronicle.services.notifications.NOTIFICATION_DELETED_ACTION
import com.openlattice.chronicle.utils.Utils.getPendingIntentMutabilityFlag

class UnlockDeviceReceiver : BroadcastReceiver() {

    private lateinit var appContext: Context

    companion object  {
        fun getValidReceiverActions(context: Context): Set<String> {
            return setOf(Intent.ACTION_USER_PRESENT, Intent.ACTION_SCREEN_ON, context.getString(R.string.action_identify_after_reboot))
        }
    }

    override fun onReceive(context: Context, intent: Intent) {

        appContext = context

        val settings = EnrollmentSettings(context)
        if (!settings.isUserIdentificationEnabled()) {
            return
        }

        val action = intent.action
        if (!getValidReceiverActions(context).contains(action)) {
            return
        }

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
        val userIdentificationIntent =
            Intent(context, UserIdentificationActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        val pendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                userIdentificationIntent,
                getPendingIntentMutabilityFlag(PendingIntent.FLAG_UPDATE_CURRENT)
            )

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
            .setCustomContentView(layout)
            .setCustomBigContentView(layout)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)
            .setDeleteIntent(createOnDismissedIntent())
//            .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
            .setAutoCancel(true) // remove when user taps on notification

        with(NotificationManagerCompat.from(context)) {
            notify(1, notificationBuilder.build())
        }

    }

    private fun createOnDismissedIntent(): PendingIntent {
        val intent = Intent(NOTIFICATION_DELETED_ACTION)
        val resources = appContext.resources
        val notificationId = resources.getInteger(R.integer.dismiss_target_user_notification_id)
        intent.putExtra(appContext.getString(R.string.notification_id), notificationId)

        return PendingIntent.getBroadcast(appContext, 0, intent, getPendingIntentMutabilityFlag(0))
    }
}
