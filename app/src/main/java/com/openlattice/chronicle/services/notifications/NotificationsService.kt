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
import com.crashlytics.android.Crashlytics
import com.openlattice.chronicle.R
import com.openlattice.chronicle.constants.Jobs
import com.openlattice.chronicle.preferences.EnrollmentSettings
import com.openlattice.chronicle.preferences.PARTICIPANT_ID
import com.openlattice.chronicle.preferences.STUDY_ID
import com.openlattice.chronicle.receivers.lifecycle.NotificationsReceiver
import io.fabric.sdk.android.Fabric
import java.util.*

const val CHANNEL_ID = "Chronicle"
const val NOTIFICATIONS_ENABLED = "notificationsEnabled"

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
            scheduleNotification()
        } else {
            cancelNotification()
        }
    }

    // schedule notification at 7.00pm
    // ref: https://developer.android.com/training/scheduling/alarms
    private fun scheduleNotification() {

        val alarmManager: AlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, NotificationsReceiver::class.java)
        intent.putExtra(PARTICIPANT_ID, settings.getParticipantId())
        intent.putExtra(STUDY_ID, settings.getStudyId().toString())

        val alarmIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        // set alarm to fire at 7.00pm
        val calendar: Calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 19)
        calendar.set(Calendar.MINUTE, 0)

        if (calendar.timeInMillis < System.currentTimeMillis()) {
            calendar.add(Calendar.DATE, 1)
        }

        alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, alarmIntent)
    }

    // invoke this when the participant is no longer enrolled or the study's notifications are turned off
    private fun cancelNotification() {
        val alarmManager: AlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, NotificationsReceiver::class.java)
        intent.putExtra(PARTICIPANT_ID, settings.getParticipantId())
        intent.putExtra(STUDY_ID, settings.getStudyId().toString())

        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_NO_CREATE)
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

