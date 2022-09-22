package com.openlattice.chronicle.services.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.openlattice.chronicle.R
import com.openlattice.chronicle.preferences.EnrollmentSettings

class NotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {

        // When notification to identify user has been posted, unassign current user. If user ignores notification,
        // subsequent usage events will be assumed to belong to an 'unidentified user'
        if (sbn?.id == applicationContext.resources.getInteger(R.integer.identify_user_notification_id)) {
            Log.i(javaClass.name, "User identification notification posted.")
            EnrollmentSettings(applicationContext).setTargetUser(applicationContext.getString(R.string.user_unassigned))
        }
    }
}