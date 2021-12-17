package com.openlattice.chronicle.receivers.lifecycle

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_BOOT_COMPLETED
import android.util.Log
import com.openlattice.chronicle.data.ParticipationStatus
import com.openlattice.chronicle.preferences.EnrollmentSettings
import com.openlattice.chronicle.services.notifications.InteractivityMonitoringWorker
import com.openlattice.chronicle.services.notifications.scheduleNotificationsWorker
import com.openlattice.chronicle.services.upload.scheduleUploadWork
import com.openlattice.chronicle.services.usage.scheduleUsageMonitoringWork

val TAG = StartOnBoot::class.java.simpleName

class StartOnBoot : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent != null) {
            if (intent.action.equals(ACTION_BOOT_COMPLETED)) {
                val settings = EnrollmentSettings(context)
                if (settings.getParticipationStatus() == ParticipationStatus.ENROLLED) {

                    // start workers
                    scheduleNotificationsWorker(context)
                    Log.i(TAG, "started notifications worker at boot")

                    scheduleUploadWork(context)
                    Log.i(TAG, "started upload worker at boot.")

                    scheduleUsageMonitoringWork(context)
                    Log.i(TAG, "started usage monitoring worker at boot")

                    InteractivityMonitoringWorker.startWorker(context, restartOnBoot = true)
                }
            }
        } else {
            Log.e(javaClass.canonicalName, "Unable to start Usage Service at Boot.")
        }
    }
}
