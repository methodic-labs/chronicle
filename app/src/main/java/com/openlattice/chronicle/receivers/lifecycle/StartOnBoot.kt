package com.openlattice.chronicle.receivers.lifecycle

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_BOOT_COMPLETED
import android.util.Log
import com.openlattice.chronicle.services.upload.UploadJobService
import com.openlattice.chronicle.services.usage.UsageService

val PERIOD_MILLIS = 10 * 1000L;

class StartOnBoot : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent != null) {
            if (intent.action.equals(ACTION_BOOT_COMPLETED)) {
                startUsageService(context)
                Log.i(javaClass.canonicalName, "Started usage service at boot.")
                scheduleUploadJob(context)
                Log.i(javaClass.canonicalName, "Scheduled upload job at boot.")
            }
        } else {
            Log.e(javaClass.canonicalName, "Unable to start Usage Service at Boot.")
        }
    }

    fun startUsageService(context: Context) {
        context.startService(Intent(context, UsageService::class.java))
    }
}

fun scheduleUploadJob(context: Context) {
    val serviceComponent = ComponentName(context, UploadJobService::class.java)
    val jobBuilder = JobInfo.Builder(0, serviceComponent)
    jobBuilder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
    jobBuilder.setPeriodic(PERIOD_MILLIS)
    val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
    jobScheduler.schedule(jobBuilder.build())
}
