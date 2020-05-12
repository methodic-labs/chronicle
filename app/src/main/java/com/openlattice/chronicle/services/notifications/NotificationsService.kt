package com.openlattice.chronicle.services.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.preference.PreferenceManager
import android.util.Log
import com.crashlytics.android.Crashlytics
import com.openlattice.chronicle.ChronicleStudyApi
import com.openlattice.chronicle.R
import com.openlattice.chronicle.constants.Jobs.NOTIFICATION_JOB_ID
import com.openlattice.chronicle.data.ParticipationStatus
import com.openlattice.chronicle.preferences.EnrollmentSettings
import com.openlattice.chronicle.receivers.lifecycle.NotificationsReceiver
import com.openlattice.chronicle.services.upload.PRODUCTION
import com.openlattice.chronicle.services.upload.createRetrofitAdapter
import com.openlattice.chronicle.utils.Utils.isJobServiceScheduled
import io.fabric.sdk.android.Fabric
import org.joda.time.DateTime
import java.util.*
import java.util.concurrent.Executors

const val CHANNEL_ID = "Chronicle"
const val NOTIFICATION_PERIOD_MILLIS = 24 * 60 * 60 * 1000L
const val LAST_NOTIFICATION_SETTING = "last_notification"

class NotificationsService : JobService() {
    private val executor = Executors.newSingleThreadExecutor()
    private val chronicleStudyApi = createRetrofitAdapter(PRODUCTION).create(ChronicleStudyApi::class.java)

    override fun onCreate() {
        super.onCreate()
        Fabric.with(this, Crashlytics())
    }

    override fun onStopJob(parameters: JobParameters?): Boolean {
        Log.i(javaClass.name, "Notifications service stopped")
        executor.shutdown()
        return true;
    }

    override fun onStartJob(parameters: JobParameters?): Boolean {
        Log.i(javaClass.name, "Notifications service initialized")

        executor.execute {
            createNotificationChannel(this)

            val enrollmentSettings = EnrollmentSettings(this)
            val participantId: String = enrollmentSettings.getParticipantId()
            val studyId: UUID = enrollmentSettings.getStudyId()

            var notificationsEnabled = enrollmentSettings.getNotificationsEnabled()
            var participationStatus = enrollmentSettings.getParticipationStatus()
            try {
                notificationsEnabled = chronicleStudyApi.isNotificationsEnabled(studyId)
                participationStatus = chronicleStudyApi.getParticipationStatus(studyId, participantId)
            } catch (e: Exception) {
                Crashlytics.log("caught exception: studyId: \"$studyId\" participantId: \"$participantId\"")
                Crashlytics.logException(e)
            }
            Log.i(javaClass.name, "Notifications enabled :$notificationsEnabled, participation status: $participationStatus")

            enrollmentSettings.setNotificationsEnabled(notificationsEnabled)
            enrollmentSettings.setParticipationStatus(participationStatus)

            if (notificationsEnabled && participationStatus == ParticipationStatus.ENROLLED) {
                scheduleNotification()
            } else {
                cancelNotification()
            }
            jobFinished(parameters, false)
        }
        return true;
    }

    // schedule notification at 7.00pm
    // ref: https://developer.android.com/training/scheduling/alarms
    private fun scheduleNotification() {

        val alarmManager: AlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, NotificationsReceiver::class.java)
        val alarmIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        // set alarm to fire at 7.00pm
        val calendar: Calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 19)
        calendar.set(Calendar.MINUTE, 0)

        // only send notification if none has been sent today
        if (getLastNotificationDate(this) != DateTime.now().toLocalDate().toString()) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, alarmIntent)
            setLastNotificationDate(this)
        }
    }

    // invoke this when the participant is no longer enrolled or the study's notifications are turned off
    private fun cancelNotification() {
        val alarmManager: AlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, NotificationsReceiver::class.java)
        val pendingIntent = PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_NO_CREATE)
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }
}


// this is required by Android 8.0 and higher. Should be called at when the app starts and
// can be called called repeatedly after
// ref: https://developer.android.com/training/notify-user/build-notification

fun createNotificationChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = context.resources.getString(R.string.channel_name)
        val channelDescription = context.resources.getString(R.string.channel_description)
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

fun setLastNotificationDate(context: Context) {
    val settings = PreferenceManager.getDefaultSharedPreferences(context)
    settings
            .edit()
            .putString(LAST_NOTIFICATION_SETTING, DateTime.now().toLocalDate().toString())
            .apply()
}

fun getLastNotificationDate(context: Context) :String {
    val settings = PreferenceManager.getDefaultSharedPreferences(context)
    return settings.getString(LAST_NOTIFICATION_SETTING, "")
}

fun scheduleNotificationJobService(context: Context) {
    if (!isJobServiceScheduled(context, NOTIFICATION_JOB_ID.id)) {
        val componentName = ComponentName(context, NotificationsService::class.java)
        val jobBuilder = JobInfo.Builder(NOTIFICATION_JOB_ID.id, componentName)
        jobBuilder.setPersisted(true)
        jobBuilder.setPeriodic(NOTIFICATION_PERIOD_MILLIS)
        jobBuilder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        jobScheduler.schedule(jobBuilder.build())
    }
}
