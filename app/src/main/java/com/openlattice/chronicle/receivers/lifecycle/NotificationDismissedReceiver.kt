package com.openlattice.chronicle.receivers.lifecycle

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.methodic.chronicle.R
import com.openlattice.chronicle.preferences.EnrollmentSettings
import com.openlattice.chronicle.services.usage.scheduleUsageMonitoringWork

class NotificationDismissedReceiver : BroadcastReceiver() {
    private lateinit var settings: EnrollmentSettings

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) {
            return
        }
        settings = EnrollmentSettings(context)
        val notificationId = intent.let {
            it.extras?.getInt(context.getString(R.string.notification_id))
        }

        val dismissNotificationId =
            context.resources?.getInteger(R.integer.dismiss_target_user_notification_id)
        if (dismissNotificationId == notificationId) {
            settings.setTargetUser(context.getString(R.string.user_unassigned))
            scheduleUsageMonitoringWork(context)
        }
    }
}
