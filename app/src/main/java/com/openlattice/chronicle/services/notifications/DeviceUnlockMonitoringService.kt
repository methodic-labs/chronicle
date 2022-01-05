package com.openlattice.chronicle.services.notifications

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.openlattice.chronicle.R
import com.openlattice.chronicle.SettingsActivity
import com.openlattice.chronicle.receivers.lifecycle.NotificationDismissedReceiver
import com.openlattice.chronicle.receivers.lifecycle.UnlockDeviceReceiver

// A "forever running" service to monitor device unlock. A workaround for devices running version >= 8.0
// since we can no longer register ACTION_USER_PRESENT intent in manifest
class DeviceUnlockMonitoringService : Service() {

    private var unlockDeviceReceiver = UnlockDeviceReceiver()
    private var notificationDismissedReceiver = NotificationDismissedReceiver()

    companion object {
        private const val RESTART_ON_BOOT_KEY = "restartOnBoot"

        fun startService(context: Context, restartOnBoot: Boolean? = false) {

            val intent = Intent(context, DeviceUnlockMonitoringService::class.java).apply {
                putExtra(RESTART_ON_BOOT_KEY, restartOnBoot)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, DeviceUnlockMonitoringService::class.java)
            context.stopService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }


    override fun onCreate() {
        startForeground()
        registerReceivers()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val restartOnBoot = intent?.getBooleanExtra(RESTART_ON_BOOT_KEY, false)

        Log.i(javaClass.name, "unlock monitoring service started with restartOnBoot = $restartOnBoot")

        if (restartOnBoot == true) {
            Intent().also {
                intent.action = applicationContext.getString(R.string.action_identify_after_reboot)
                applicationContext.sendBroadcast(it)
            }
        }

        // if the service is killed after starting, the system will try to re-create the service later
        return START_STICKY
    }

    private fun startForeground() {

        val pendingIntent: PendingIntent =
            Intent(applicationContext, SettingsActivity::class.java).let {
                PendingIntent.getActivity(applicationContext, 0, it, 0)
            }

        val channelId = getString(R.string.channel_name)
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(getString(R.string.interactivity_monitoring_notification_title))
            .setContentText(getString(R.string.interactivity_monitoring_notification_message))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setColor(ContextCompat.getColor(applicationContext, R.color.colorPrimary))
            .setContentIntent(pendingIntent)
            .build()

        val notificationId =
            applicationContext.resources.getInteger(R.integer.unlock_phone_monitoring_notification_id)
        startForeground(notificationId, notification)
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
            createReceiverIntentFilter(
                UnlockDeviceReceiver.getValidReceiverActions(
                    applicationContext
                )
            )
        applicationContext.registerReceiver(unlockDeviceReceiver, intentFilter)
        Log.i(javaClass.name, "${UnlockDeviceReceiver::class.java.canonicalName} is registered")

        intentFilter = createReceiverIntentFilter(setOf(NOTIFICATION_DELETED_ACTION))
        applicationContext.registerReceiver(notificationDismissedReceiver, intentFilter)
        Log.i(
            javaClass.name,
            "${NotificationDismissedReceiver::class.java.canonicalName} is registered"
        )
    }

    override fun onDestroy() {
        Log.i(javaClass.name, "unlock monitoring service stopped. Unregistering receivers")
        applicationContext.unregisterReceiver(unlockDeviceReceiver)
        applicationContext.unregisterReceiver(notificationDismissedReceiver)
    }
}