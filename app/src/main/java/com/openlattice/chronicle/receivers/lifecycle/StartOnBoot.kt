package com.openlattice.chronicle.receivers.lifecycle

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_BOOT_COMPLETED
import android.util.Log
import com.openlattice.chronicle.data.ParticipationStatus
import com.openlattice.chronicle.preferences.EnrollmentSettings
import com.openlattice.chronicle.services.notifications.scheduleNotificationJobService
import com.openlattice.chronicle.services.status.scheduleParticipationStatusJob
import com.openlattice.chronicle.services.upload.scheduleUploadJob
import com.openlattice.chronicle.services.usage.scheduleUsageMonitoringJob

class StartOnBoot : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent != null) {
            if (intent.action.equals(ACTION_BOOT_COMPLETED)) {
                val settings = EnrollmentSettings(context)
                if (settings.getParticipationStatus() == ParticipationStatus.ENROLLED) {
                    scheduleUsageMonitoringJob(context)
                    Log.i(javaClass.canonicalName, "Started usage service at boot.")

                    scheduleUploadJob(context)
                    Log.i(javaClass.canonicalName, "Scheduled upload job at boot.")
                }
                scheduleNotificationJobService(context)
                Log.i(javaClass.name, "Scheduled notification service at boot")

                scheduleParticipationStatusJob(context)
                Log.i(javaClass.name, "Scheduled participation status service at boot")
            }
        } else {
            Log.e(javaClass.canonicalName, "Unable to start Usage Service at Boot.")
        }
    }
}
