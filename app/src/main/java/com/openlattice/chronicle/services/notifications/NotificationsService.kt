package com.openlattice.chronicle.services.notifications

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import com.openlattice.chronicle.R
import com.openlattice.chronicle.preferences.EnrollmentSettings
import com.openlattice.chronicle.receivers.lifecycle.NotificationsReceiver
import java.util.*

const val CHANNEL_ID = "Chronicle"

class NotificationsService :IntentService ("SurveyNotificationsService") {

    override fun onHandleIntent(intent: Intent) {
        val enrollmentSettings = EnrollmentSettings(this)

        if (enrollmentSettings.getNotificationsEnabled()) {
            scheduleNotifications()
        } else {
            cancelPendingNotifications()
        }
    }


    // schedule daily notifications at 7.00pm central time
    // ref: https://developer.android.com/training/scheduling/alarms
    private fun scheduleNotifications () {

        val alarmManager : AlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = createPendingAlarmIntent()

        // set alarm to fire at 7.00pm central time
        val calendar : Calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 19)
        calendar.set(Calendar.MINUTE, 0)
        calendar.timeZone = TimeZone.getTimeZone("Central Standard Time")

        alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, AlarmManager.INTERVAL_DAY, alarmIntent)
    }

    private fun createPendingAlarmIntent() :PendingIntent {
        val intent  = Intent(this, NotificationsReceiver::class.java)
        return PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)
    }

    // invoke this when the device is no longer registered / or the study's notifications are turned off
    // cancel both the pending intent and cancel the alarm using alarm manager
    // ref: https://stackoverflow.com/questions/11681095/cancel-an-alarmmanager-pendingintent-in-another-pendingintent

    private fun cancelPendingNotifications() {
        val alarmManager : AlarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = createPendingAlarmIntent()

        alarmIntent.cancel()
        alarmManager.cancel(alarmIntent)

    }
}


// this is required by Android 8.0 and higher. Should be called at when the app starts and
// can be called called repeatedly after
// ref: https://developer.android.com/training/notify-user/build-notification

fun createNotificationChannel (context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = context.resources.getString(R.string.channel_name)
        val channelDescription = context.resources.getString(R.string.channel_description)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = channelDescription
        }

        //register channel
        val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}