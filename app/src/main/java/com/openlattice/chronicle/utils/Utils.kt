package com.openlattice.chronicle.utils

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobScheduler
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.preference.PreferenceManager
import com.fasterxml.jackson.databind.SerializationFeature
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.openlattice.chronicle.R
import com.methodic.chronicle.constants.NotificationType
import com.openlattice.chronicle.services.notifications.CHANNEL_ID
import com.openlattice.chronicle.services.notifications.NotificationDetails
import com.openlattice.chronicle.services.upload.LAST_UPDATED_SETTING
import com.openlattice.chronicle.services.upload.LAST_UPLOADED_PLACEHOLDER
import com.openlattice.chronicle.util.RetrofitBuilders
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import retrofit2.Retrofit
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

object Utils {
    fun offsetDateTimeFromEpochMillis( epochMillis: Long ) : OffsetDateTime {
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneOffset.UTC)
    }

    fun isValidUUID(possibleUUID: String): Boolean {
        try {
            if (possibleUUID.isEmpty()) {
                return false
            }
            UUID.fromString(possibleUUID)
        } catch (e: IllegalArgumentException) {
            FirebaseCrashlytics.getInstance().recordException(e)
            return false
        }
        return true
    }

    fun getAppFullName(context: Context, packageName: String): String {
        return try {
            val packageManager = context.applicationContext.packageManager
            val applicationInfo =
                packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            packageManager.getApplicationLabel(applicationInfo).toString()
        } catch (e: Exception) {
            packageName
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

    fun createNotificationTargetUrl(
        notificationDetails: NotificationDetails,
        studyId: String,
        participantId: String
    ): String {
        val uriBuilder: Uri.Builder = Uri.Builder()

        uriBuilder
            .scheme("https")
            .encodedAuthority("app.getmethodic.com")
            .appendPath("chronicle")
            .appendEncodedPath("#")

        if (notificationDetails.type === NotificationType.QUESTIONNAIRE) {
            uriBuilder.appendPath("questionnaire")

            uriBuilder
                .appendQueryParameter("studyId", studyId)
                .appendQueryParameter("participantId", participantId)
                .appendQueryParameter("questionnaireId", notificationDetails.id)
        }
        if (notificationDetails.type === NotificationType.AWARENESS) {
            uriBuilder.appendPath("survey")
            uriBuilder
                .appendQueryParameter("studyId", studyId)
                .appendQueryParameter("participantId", participantId)
        }

        return uriBuilder.build().toString()
    }


    fun setLastUpload(context: Context) {
        val settings = PreferenceManager.getDefaultSharedPreferences(context)
        with(settings.edit()) {
            putString(LAST_UPDATED_SETTING, DateTime.now().toString())
            apply()
        }
    }

    fun getLastUpload(context: Context): String {

        val settings = PreferenceManager.getDefaultSharedPreferences(context)
        val lastUpdated = settings.getString(LAST_UPDATED_SETTING, LAST_UPLOADED_PLACEHOLDER)

        if (lastUpdated != LAST_UPLOADED_PLACEHOLDER) {
            return DateTime.parse(lastUpdated).toString(DateTimeFormat.mediumDateTime())
        }

        return lastUpdated
    }

    fun createRetrofitAdapter(baseUrl: String): Retrofit {
        RetrofitBuilders.mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        val httpClient = RetrofitBuilders.okHttpClient().build()
        return RetrofitBuilders.decorateWithRhizomeFactories(
            RetrofitBuilders.createBaseChronicleRetrofitBuilder(
                baseUrl,
                httpClient
            )
        ).build()
    }

    // required by android 8.0 and higher.
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.resources.getString(R.string.channel_name)
            val channelDescription =
                context.resources.getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = channelDescription
            }
            //register channel
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Integer.MAX_VALUE)
            .any { it.service.className == serviceClass.name }
    }

    fun getPendingIntentMutabilityFlag(flags: Int): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or flags
        } else {
            flags
        }
    }
}
