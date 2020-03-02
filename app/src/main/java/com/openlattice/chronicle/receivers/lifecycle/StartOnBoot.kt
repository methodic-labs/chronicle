package com.openlattice.chronicle.receivers.lifecycle

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_BOOT_COMPLETED
import android.util.Log
import com.openlattice.chronicle.preferences.EnrollmentSettings
import com.openlattice.chronicle.preferences.PARTICIPANT_ID
import com.openlattice.chronicle.preferences.STUDY_ID
import com.openlattice.chronicle.services.upload.scheduleUploadJob
import com.openlattice.chronicle.services.usage.scheduleUsageMonitoringJob
import com.openlattice.chronicle.services.notifications.NotificationsService

class StartOnBoot : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent != null) {
            if (intent.action.equals(ACTION_BOOT_COMPLETED)) {
                scheduleUsageMonitoringJob(context)
                Log.i(javaClass.canonicalName, "Started usage service at boot.")
                scheduleUploadJob(context)
                Log.i(javaClass.canonicalName, "Scheduled upload job at boot.")

                resetNotifications(context)
            }
        } else {
            Log.e(javaClass.canonicalName, "Unable to start Usage Service at Boot.")
        }
    }

    // by default, alarms are cancelled when a device shuts down.
    // thus, need to restart the notifications alarm when the user reboots the device

    private fun resetNotifications(context: Context) {
        val enrollment = EnrollmentSettings(context)

        if (enrollment.enrolled && enrollment.getNotificationsEnabled()) {
            context.startService(Intent(context, NotificationsService::class.java))
            Log.i(javaClass.canonicalName, "Restart notifications at boot")
        }
    }
}

