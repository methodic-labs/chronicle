package com.openlattice.chronicle.services.notifications

import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit

// Repeatedly attempts to start DeviceUnlockMonitoringService if the service is not running
class DeviceUnlockMonitoringWorker(context: Context, workerParameters: WorkerParameters) :
    Worker(context, workerParameters) {

    private val appContext = context

    companion object {
        private const val DELAY_DURATION = 15L

        fun startWorker(context: Context) {

            val workRequest =
                PeriodicWorkRequestBuilder<DeviceUnlockMonitoringWorker>(DELAY_DURATION, TimeUnit.MINUTES).build()

            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }

    override fun doWork(): Result {
        Log.i(javaClass.name, "device unlock monitoring worker started")

        DeviceUnlockMonitoringService.startService(appContext)
        return Result.success()
    }
}

