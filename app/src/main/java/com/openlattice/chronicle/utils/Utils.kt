package com.openlattice.chronicle.utils

import android.app.job.JobScheduler
import android.content.Context
import com.crashlytics.android.Crashlytics

import java.util.UUID

object Utils {

    fun isValidUUID(possibleUUID: String): Boolean = try {
        val uuid = UUID.fromString(possibleUUID)
        val uuidAsString = uuid.toString()
        uuidAsString == possibleUUID
    }
    catch (e: IllegalArgumentException) {
        Crashlytics.logException(e)
        false
    }

    // Return true if job service is running. In API 24 the solution would be: scheduler.getPendingJob(JOB_ID) != null
    fun isJobServiceScheduled(context: Context, jobId :Number) :Boolean {
        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        for (jobInfo in jobScheduler.allPendingJobs) {
            return jobInfo.id == jobId
        }
        return false
    }
}
