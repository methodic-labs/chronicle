package com.methodic.chronicle.services.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationPermissionListener : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (NotificationManager.ACTION_APP_BLOCK_STATE_CHANGED == intent?.action) {
            val blocked = intent.getBooleanExtra(NotificationManager.EXTRA_BLOCKED_STATE, false)

            if( !blocked) {
//                NotificationPermissionActivity.currentPermissionActivity?.granted()
            }
        }
    }
}