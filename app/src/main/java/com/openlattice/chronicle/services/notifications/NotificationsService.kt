package com.openlattice.chronicle.services.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.support.v4.app.JobIntentService
import com.crashlytics.android.Crashlytics
import com.google.gson.Gson
import com.openlattice.chronicle.R
import com.openlattice.chronicle.constants.Jobs
import com.openlattice.chronicle.preferences.EnrollmentSettings
import com.openlattice.chronicle.preferences.PARTICIPANT_ID
import com.openlattice.chronicle.preferences.STUDY_ID
import com.openlattice.chronicle.receivers.lifecycle.NotificationsReceiver
import com.openlattice.chronicle.storage.Notification
import io.fabric.sdk.android.Fabric
import org.springframework.scheduling.config.CronTask
import org.springframework.scheduling.support.CronSequenceGenerator
import java.lang.Exception
import java.util.*

const val CHANNEL_ID = "Chronicle"
const val NOTIFICATIONS_ENABLED = "notificationsEnabled"
const val NOTIFICATION_ENTRY = "notificationEntry"

class NotificationsService: JobIntentService() {
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
        if (intent.getBooleanExtra(NOTIFICATIONS_ENABLED, true)) {
            scheduleNotification(serializedString)
        } else {
            cancelNotification(serializedString)
        }
    }

    // schedule notification at 7.00pm
    // ref: https://developer.android.com/training/scheduling/alarms
    private fun scheduleNotification(serializedString: String) {
        val notification  = Gson().fromJson(serializedString, Notification::class.java)
        Log.i(javaClass.name, "notification to schedule: $notification")

        val alarmManager: AlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, NotificationsReceiver::class.java)

        val alarmIntent = PendingIntent.getBroadcast(this, notification.getRequestCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT)

        try {

            val date = CronSequenceGenerator(notification.cronExpression).next(Date())
            val calendar = Calendar.getInstance()
            calendar.time = date

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, alarmIntent)
            } else{
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, alarmIntent)
            }

        } catch (e: Exception) {
            Log.i(javaClass.name, "caught exception", e)
        }


    }

    // invoke this when the participant is no longer enrolled or the study's notifications are turned off
    private fun cancelNotification(serializedString: String) {
        val notification  = Gson().fromJson(serializedString, Notification::class.java)
        Log.i(javaClass.name, "Notification to cancel: $notification")

        val alarmManager: AlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, NotificationsReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(this, notification.getRequestCode(), intent, PendingIntent.FLAG_NO_CREATE)
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
