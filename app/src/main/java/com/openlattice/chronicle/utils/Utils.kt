package com.openlattice.chronicle.utils

import android.app.job.JobScheduler
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import com.crashlytics.android.Crashlytics
import com.openlattice.chronicle.constants.NotificationType
import com.openlattice.chronicle.services.notifications.NotificationEntry
import java.util.*

object Utils {

    fun isValidUUID(possibleUUID: String): Boolean = try {
        val uuid = UUID.fromString(possibleUUID)
        val uuidAsString = uuid.toString()
        uuidAsString == possibleUUID
    } catch (e: IllegalArgumentException) {
        Crashlytics.logException(e)
        false
    }

    fun getAppFullName(context: Context, packageName: String): String {
        return try {
            val packageManager = context.applicationContext.packageManager
            val applicationInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: Exception) {
            packageName;
        }

    }

    // Return true if job service is running. In API 24 the solution would be: scheduler.getPendingJob(JOB_ID) != null
    fun isJobServiceScheduled(context: Context, jobId: Number): Boolean {
        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        for (jobInfo in jobScheduler.allPendingJobs) {
            return jobInfo.id == jobId
        }
        return false
    }

    fun createNotificationTargetUrl(notificationEntry: NotificationEntry, studyId: String, participantId: String) :String{
        val uriBuilder: Uri.Builder = Uri.Builder()
        uriBuilder
                .scheme("https")
                .encodedAuthority("openlattice.com")
                .appendPath("chronicle")
                .appendEncodedPath("#")

        if (notificationEntry.type === NotificationType.QUESTIONNAIRE) {
            uriBuilder
                    .appendPath("questionnaire")
                    .appendQueryParameter("studyId", studyId)
                    .appendQueryParameter("participantId", participantId)
                    .appendQueryParameter("questionnaireId", notificationEntry.id)
        }
        if (notificationEntry.type === NotificationType.AWARENESS) {
            uriBuilder
                    .appendPath("survey")
                    .appendQueryParameter("studyId", studyId)
                    .appendQueryParameter("participantId", participantId)
        }

        return uriBuilder.build().toString()
    }
}
