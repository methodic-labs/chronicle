package com.openlattice.chronicle.receivers.lifecycle

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.openlattice.chronicle.services.usage.scheduleUsageMonitoringJob

class UsageCollectionAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i(javaClass.name, "Usage collection alarm trigger.")
//        scheduleUsageMonitoringJob(context!!)
    }
}