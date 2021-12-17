package com.openlattice.chronicle.services.notifications

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.work.*
import com.openlattice.chronicle.R
import com.openlattice.chronicle.receivers.lifecycle.NotificationDismissedReceiver
import com.openlattice.chronicle.receivers.lifecycle.UnlockDeviceReceiver
import java.util.concurrent.TimeUnit

// Schedule work requests to context register the receiver that listen for "user present" intent
class InteractivityMonitoringWorker(context: Context, workerParameters: WorkerParameters) :
    Worker(context, workerParameters) {

    private var unlockDeviceReceiver = UnlockDeviceReceiver()
    private var notificationDismissedReceiver = NotificationDismissedReceiver()

    private val appContext = context

    companion object {
        private const val REPEAT_INTERVAL = 15L
        private const val RESTART_ON_BOOT_KEY = "restartOnBoot"

        private fun scheduleRequest(
            context: Context,
            delayInMinutes: Long = 0L,
            data: Data = workDataOf()
        ) {

            val workRequest: WorkRequest =
                OneTimeWorkRequestBuilder<InteractivityMonitoringWorker>().apply {
                    setInitialDelay(delayInMinutes, TimeUnit.MINUTES)
                    setInputData(data)
                }.build()

            WorkManager.getInstance(context).enqueue(workRequest)
        }

        fun startWorker(context: Context, restartOnBoot: Boolean = false) {
            val data = workDataOf(RESTART_ON_BOOT_KEY to restartOnBoot)
            scheduleRequest(context, data = data)
        }
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
            createReceiverIntentFilter(UnlockDeviceReceiver.getValidReceiverActions(appContext))
        applicationContext.registerReceiver(unlockDeviceReceiver, intentFilter)
        Log.i(javaClass.name, "${UnlockDeviceReceiver::class.java.canonicalName} is registered")

        intentFilter = createReceiverIntentFilter(setOf(NOTIFICATION_DELETED_ACTION))
        applicationContext.registerReceiver(notificationDismissedReceiver, intentFilter)
        Log.i(javaClass.name, "${NotificationDismissedReceiver::class.java.canonicalName} is registered")
    }

    override fun doWork(): Result {
        unlockDeviceReceiver = UnlockDeviceReceiver()
        notificationDismissedReceiver = NotificationDismissedReceiver()

        // register receivers
        registerReceivers()

        val restartOnBoot = inputData.getBoolean(RESTART_ON_BOOT_KEY, false)
        if (restartOnBoot) {
            Intent().also { intent ->
                intent.action = appContext.getString(R.string.action_identify_after_reboot)
                appContext.sendBroadcast(intent)
            }
        }

        // re-schedule worker before returning
        scheduleRequest(appContext, REPEAT_INTERVAL)
        return Result.success()
    }

    override fun onStopped() {

        // unregister receivers
        appContext.unregisterReceiver(notificationDismissedReceiver)
        appContext.unregisterReceiver(notificationDismissedReceiver)
    }
}


