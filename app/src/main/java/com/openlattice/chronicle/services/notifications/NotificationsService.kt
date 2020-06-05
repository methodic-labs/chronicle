package com.openlattice.chronicle.services.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.app.JobIntentService
import android.util.Log
import com.crashlytics.android.Crashlytics
import com.google.gson.Gson
import com.openlattice.chronicle.R
import com.openlattice.chronicle.constants.Jobs
import com.openlattice.chronicle.preferences.EnrollmentSettings
import com.openlattice.chronicle.preferences.PARTICIPANT_ID
import com.openlattice.chronicle.preferences.STUDY_ID
import com.openlattice.chronicle.receivers.lifecycle.NotificationsReceiver
import io.fabric.sdk.android.Fabric
import org.springframework.scheduling.support.CronSequenceGenerator
import java.util.*

const val CHANNEL_ID = "Chronicle"
const val NOTIFICATIONS_ENABLED = "notificationsEnabled"
const val NOTIFICATION_ENTRY = "notificationEntry"

class NotificationsService : JobIntentService() {
    private lateinit var settings: EnrollmentSettings

    override fun onCreate() {
        super.onCreate()
        Fabric.with(this, Crashlytics())
        settings = EnrollmentSettings(this)
    }

    companion object {
        fun enqueueWork(context: Context, intent: Intent) {
            val serviceComponent = ComponentName(context, NotificationsService::class.java)
            enqueueWork(context, serviceComponent, Jobs.NOTIFICATION_JOB_ID.id, intent)
        }
    }

    override fun onHandleWork(intent: Intent) {
        val notificationEntry = intent.getStringExtra(NOTIFICATION_ENTRY)
        if (intent.getBooleanExtra(NOTIFICATIONS_ENABLED, true)) {
            scheduleNotification(notificationEntry)
        } else {
            cancelNotification(notificationEntry)
        }
    }

    // schedule next notification
    // ref: https://developer.android.com/training/scheduling/alarms
    private fun scheduleNotification(notificationEntry: String) {
        val notification = Gson().fromJson(notificationEntry, NotificationEntry::class.java)
        Log.i(javaClass.name, "notification to schedule: $notification")

        val intent = createNotificationIntent(notificationEntry)
        val pendingIntent = PendingIntent.getBroadcast(this, notification.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT)

        try {
            // Cron expression must of length 6 or will throw IllegalArgumentException exception
            // example input: "0 0 */2 * * *" (repeat every 2 hours)
            val date = CronSequenceGenerator(notification.cronExpression).next(Date())
            val calendar = Calendar.getInstance()
            calendar.time = date
            Log.i(javaClass.name, "notification time: $date")

            val alarmManager: AlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            }

        } catch (e: Exception) {
            Log.i(javaClass.name, "caught exception", e)
        }

    }

    // invoke this when the participant is no longer enrolled or the study's notifications are turned off
    private fun cancelNotification(notificationEntry: String) {
        val notification = Gson().fromJson(notificationEntry, NotificationEntry::class.java)
        Log.i(javaClass.name, "Notification to cancel: $notification")

        val intent = createNotificationIntent(notificationEntry)
        val pendingIntent = PendingIntent.getBroadcast(this, notification.hashCode(), intent, PendingIntent.FLAG_NO_CREATE)

        val alarmManager: AlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }

    private fun createNotificationIntent(notificationEntry: String): Intent {
        return Intent(this, NotificationsReceiver::class.java).apply {
            putExtra(NOTIFICATION_ENTRY, notificationEntry)
            putExtra(STUDY_ID, settings.getStudyId().toString())
            putExtra(PARTICIPANT_ID, settings.getParticipantId())
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
