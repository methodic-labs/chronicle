package com.openlattice.chronicle.services.notifications

import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log
import com.openlattice.chronicle.receivers.lifecycle.NotificationDismissedReceiver
import com.openlattice.chronicle.receivers.lifecycle.UnlockPhoneReceiver
const val NOTIFICATION_DELETED_ACTION = "NOTIFICATION_DELETED"
class UnlockPhoneService : Service() {
    private lateinit var unlockPhoneReceiver: UnlockPhoneReceiver
    private lateinit var notificationDismissedReceiver: NotificationDismissedReceiver

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        unlockPhoneReceiver = UnlockPhoneReceiver()
        notificationDismissedReceiver = NotificationDismissedReceiver()

        var intentFilter = IntentFilter(Intent.ACTION_USER_PRESENT)
        intentFilter.priority = IntentFilter.SYSTEM_HIGH_PRIORITY

        registerReceiver(unlockPhoneReceiver, intentFilter)
        Log.i(javaClass.name, "onCreate: PhoneUnlockedReceiver is registered")

        intentFilter = IntentFilter(NOTIFICATION_DELETED_ACTION)
        registerReceiver(notificationDismissedReceiver, intentFilter)
        Log.i(javaClass.name, "onCreate: NotificationDismissedReceiver is registered")
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(unlockPhoneReceiver)
        Log.i(javaClass.name, "onDestroy: PhoneUnlockedReceiver is unregistered")

        unregisterReceiver(notificationDismissedReceiver)
        Log.i(javaClass.name, "onDestroy: NotificationDismissedReceiver is unregistered")
    }
}