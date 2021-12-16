package com.openlattice.chronicle.services.notifications

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.text.format.DateUtils
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.openlattice.chronicle.R
import com.openlattice.chronicle.SettingsActivity
import com.openlattice.chronicle.receivers.lifecycle.NotificationDismissedReceiver
import com.openlattice.chronicle.receivers.lifecycle.UnlockDeviceReceiver
import com.openlattice.chronicle.utils.Utils.createNotificationChannel
import kotlin.Exception

// This service runs in the foreground to keep alive a broadcast receiver that listens to ACTION_USER_PRESENT intent broadcasts
class InteractivityMonitoringService : Service() {

    private lateinit var unlockDeviceReceiver: UnlockDeviceReceiver
    private lateinit var notificationDismissedReceiver: NotificationDismissedReceiver

    companion object {
        fun startOrStopUnlockPhoneService(stop: Boolean, context: Context) {
            val intent = Intent(context, InteractivityMonitoringService::class.java)

            if (stop) {
                Log.i(
                    InteractivityMonitoringService::class.java.name,
                    "User identification disabled. Stopping ${InteractivityMonitoringService::class.java.name}"
                )
                context.stopService(intent)
            } else {
                ContextCompat.startForegroundService(context, intent)
            }
        }
    }

    override fun onCreate() {
        unlockDeviceReceiver = UnlockDeviceReceiver()
        notificationDismissedReceiver = NotificationDismissedReceiver()
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    private fun createReceiverIntentFilter(
        actions: Set<String>,
        priority: Int = IntentFilter.SYSTEM_HIGH_PRIORITY
    ): IntentFilter {
        return IntentFilter().also {
            it.priority = priority
            for (action in actions) {
                it.addAction(action)
            }
        }
    }

    private fun registerReceivers() {
        var intentFilter =
            createReceiverIntentFilter(setOf(Intent.ACTION_USER_PRESENT, Intent.ACTION_SCREEN_ON))
        applicationContext.registerReceiver(unlockDeviceReceiver, intentFilter)
        Log.i(javaClass.name, "${UnlockDeviceReceiver::class.java.name} is registered")

        intentFilter = createReceiverIntentFilter(setOf(NOTIFICATION_DELETED_ACTION))
        applicationContext.registerReceiver(notificationDismissedReceiver, intentFilter)
        Log.i(javaClass.name, "${NotificationDismissedReceiver::class.java.name} is registered")
    }

    private fun createForegroundNotification(): Notification {
        val layout = RemoteViews(this.packageName, R.layout.notification)
        layout.setTextViewText(
            R.id.timestamp,
            DateUtils.formatDateTime(
                this,
                System.currentTimeMillis(),
                DateUtils.FORMAT_SHOW_TIME
            )
        )
        layout.setTextViewText(
            R.id.notification_title,
            this.getString(R.string.interactivity_monitoring_notification_title)
        )
        layout.setTextViewText(
            R.id.notification_message,
            this.getString(R.string.interactivity_monitoring_notification_message)
        )

        val intent = Intent(this, SettingsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val settingsPendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setCustomContentView(layout)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(getString(R.string.interactivity_monitoring_notification_title))
            .setContentText(getString(R.string.interactivity_monitoring_notification_message))
            .setStyle(NotificationCompat.BigTextStyle().bigText(getString(R.string.interactivity_monitoring_notification_message)))
            .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_menu_preferences, getString(R.string.settings), settingsPendingIntent)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(javaClass.name, "Interactivity monitoring service started")

        createNotificationChannel(this)
        registerReceivers()

        startForeground(R.integer.unlock_phone_monitoring__notification_id, createForegroundNotification())
        // if service is killed after returning, restart
        return START_STICKY
    }

    override fun onDestroy() {
        // try catch here is necessary since receivers may not have been registered when onDestroy is invoked
        try {
            this.unregisterReceiver(unlockDeviceReceiver)
            this.unregisterReceiver(notificationDismissedReceiver)
        } catch (e: Exception) {
            Log.i(javaClass.name, "Exception when unregistering receivers")
        }
    }
}