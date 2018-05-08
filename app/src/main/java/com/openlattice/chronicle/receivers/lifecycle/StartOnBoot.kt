package com.openlattice.chronicle.receivers.lifecycle

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_BOOT_COMPLETED
import android.util.Log
import com.openlattice.chronicle.services.upload.scheduleUploadJob
import com.openlattice.chronicle.services.usage.scheduleUsageEventsJob

class StartOnBoot : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent != null) {
            if (intent.action.equals(ACTION_BOOT_COMPLETED)) {
                scheduleUsageEventsJob(context)
                Log.i(javaClass.canonicalName, "Started usage service at boot.")
                scheduleUploadJob(context)
                Log.i(javaClass.canonicalName, "Scheduled upload job at boot.")
            }
        } else {
            Log.e(javaClass.canonicalName, "Unable to start Usage Service at Boot.")
        }
    }
}

