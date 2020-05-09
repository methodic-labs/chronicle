package com.openlattice.chronicle.receivers.lifecycle

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_BOOT_COMPLETED
import android.util.Log
import com.openlattice.chronicle.services.notifications.scheduleNotificationJobService
import com.openlattice.chronicle.services.upload.scheduleUploadJob
import com.openlattice.chronicle.services.usage.scheduleUsageMonitoringJob

class StartOnBoot : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent != null) {
            if (intent.action.equals(ACTION_BOOT_COMPLETED)) {
                scheduleUsageMonitoringJob(context)
                Log.i(javaClass.canonicalName, "Started usage service at boot.")
                scheduleUploadJob(context)
                Log.i(javaClass.canonicalName, "Scheduled upload job at boot.")
                scheduleNotificationJobService(context)
                Log.i(javaClass.name, "Scheduled notification service at boot")
            }
        } else {
            Log.e(javaClass.canonicalName, "Unable to start Usage Service at Boot.")
        }
    }
}

