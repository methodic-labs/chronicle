package com.openlattice.chronicle.receivers.lifecycle

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.openlattice.chronicle.services.usage.UsageService

val REQUEST_CODE = 0

class UsageCollectionAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val usageServiceIntent = Intent( context, UsageService::class.java )

        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}