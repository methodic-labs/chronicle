package com.openlattice.chronicle.utils

import android.app.job.JobScheduler
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.openlattice.chronicle.constants.NotificationType
import com.openlattice.chronicle.preferences.INVALID_ORG_ID
import com.openlattice.chronicle.services.notifications.NotificationEntry
import java.util.*

object Utils {

    fun isValidUUID(possibleUUID: String): Boolean = try {
        val uuid = UUID.fromString(possibleUUID)
        val uuidAsString = uuid.toString()
        uuidAsString == possibleUUID
    } catch (e: IllegalArgumentException) {
        FirebaseCrashlytics.getInstance().recordException(e)
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

    fun createNotificationTargetUrl(notificationEntry: NotificationEntry, orgIdStr: String, studyId: String, participantId: String) :String{
        val orgId = UUID.fromString(orgIdStr)
        val uriBuilder: Uri.Builder = Uri.Builder()

        uriBuilder
                .scheme("https")
                .encodedAuthority("openlattice.com")
                .appendPath("chronicle")
                .appendEncodedPath("#")

        if (notificationEntry.type === NotificationType.QUESTIONNAIRE) {
            uriBuilder.appendPath("questionnaire")

            if (orgId != INVALID_ORG_ID) {
                uriBuilder.appendQueryParameter("organizationId", orgIdStr)
            }
            uriBuilder
                    .appendQueryParameter("studyId", studyId)
                    .appendQueryParameter("participantId", participantId)
                    .appendQueryParameter("questionnaireId", notificationEntry.id)
        }
        if (notificationEntry.type === NotificationType.AWARENESS) {
            uriBuilder.appendPath("survey")

            if (orgId != INVALID_ORG_ID) {
                uriBuilder.appendQueryParameter("organizationId", orgIdStr)
            }

            uriBuilder
                    .appendQueryParameter("studyId", studyId)
                    .appendQueryParameter("participantId", participantId)
        }

        return uriBuilder.build().toString()
    }
}
