package com.openlattice.chronicle.services.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.work.*
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.openlattice.chronicle.ChronicleStudyApi
import com.openlattice.chronicle.R
import com.openlattice.chronicle.api.ChronicleApi
import com.openlattice.chronicle.constants.FirebaseAnalyticsEvents
import com.openlattice.chronicle.constants.NotificationType
import com.openlattice.chronicle.data.ParticipationStatus
import com.openlattice.chronicle.preferences.*
import com.openlattice.chronicle.receivers.lifecycle.NotificationDismissedReceiver
import com.openlattice.chronicle.receivers.lifecycle.SurveyNotificationsReceiver
import com.openlattice.chronicle.receivers.lifecycle.UnlockPhoneReceiver
import com.openlattice.chronicle.sensors.ACTIVE
import com.openlattice.chronicle.sensors.NAME
import com.openlattice.chronicle.sensors.RECURRENCE_RULE
import com.openlattice.chronicle.services.upload.PRODUCTION
import com.openlattice.chronicle.utils.Utils
import org.apache.olingo.commons.api.edm.FullQualifiedName
import org.dmfs.rfc5545.DateTime
import org.dmfs.rfc5545.recur.RecurrenceRule
import java.util.*
import java.util.concurrent.TimeUnit

const val NOTIFICATIONS_INTERVAL_MIN = 15L
const val NOTIFICATION_DELETED_ACTION = "NOTIFICATION_DELETED"
const val CHANNEL_ID = "Chronicle"
const val NOTIFICATION_DETAILS = "NOTIFICATION_DETAILS"
const val SURVEY_NOTIFICATION_ACTION = "SURVEY_NOTIFICATION_ACTION"

val TAG = NotificationsWorker::class.java.simpleName

class NotificationsWorker(context: Context, workerParameters: WorkerParameters) :
    Worker(context, workerParameters) {
    private val chronicleApi =
        Utils.createRetrofitAdapter(PRODUCTION).create(ChronicleApi::class.java)
    private val legacyChronicleStudyApi = Utils.createRetrofitAdapter(PRODUCTION)
        .create(ChronicleStudyApi::class.java) // legacy studies

    private lateinit var unlockPhoneReceiver: UnlockPhoneReceiver
    private lateinit var notificationDismissedReceiver: NotificationDismissedReceiver
    private lateinit var surveyNotificationsReceiver: SurveyNotificationsReceiver

    private lateinit var crashlytics: FirebaseCrashlytics
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var enrollmentSettings: EnrollmentSettings
    private lateinit var studyId: UUID
    private lateinit var participantId: String
    private lateinit var orgId: UUID

    private var participationStatus: ParticipationStatus? = ParticipationStatus.UNKNOWN
    private var studyQuestionnaires: Map<UUID, Map<FullQualifiedName, Set<Any>>>? = mapOf()
    private var notificationsEnabled: Boolean? = false

    override fun doWork(): Result {
        // register receivers
        registerNotificationReceivers()

        // required by android 8.0 and higher.
        createNotificationChannel()

        try {
            enrollmentSettings = EnrollmentSettings(applicationContext)
            studyId = enrollmentSettings.getStudyId()
            participantId = enrollmentSettings.getParticipantId()
            orgId = enrollmentSettings.getOrganizationId()
            crashlytics = FirebaseCrashlytics.getInstance()
            firebaseAnalytics = Firebase.analytics

            workHelper()
        } catch (e: Exception) {
            crashlytics.recordException(e)
            firebaseAnalytics.logEvent(FirebaseAnalyticsEvents.NOTIFICATIONS_FAILURE, null)
            return Result.failure()
        }
        return Result.success()
    }

    private fun createReceiverIntentFilter(
        action: String,
        priority: Int = IntentFilter.SYSTEM_HIGH_PRIORITY
    ): IntentFilter {
        return IntentFilter(action).apply {
            setPriority(priority)
        }
    }

    private fun registerNotificationReceivers() {
        unlockPhoneReceiver = UnlockPhoneReceiver()
        notificationDismissedReceiver = NotificationDismissedReceiver()
        surveyNotificationsReceiver = SurveyNotificationsReceiver()

        var intentFilter = createReceiverIntentFilter(Intent.ACTION_USER_PRESENT)
        applicationContext.registerReceiver(unlockPhoneReceiver, intentFilter)
        Log.i(javaClass.name, "PhoneUnlockedReceiver is registered")

        intentFilter = createReceiverIntentFilter(NOTIFICATION_DELETED_ACTION)
        applicationContext.registerReceiver(notificationDismissedReceiver, intentFilter)
        Log.i(javaClass.name, "NotificationDismissedReceiver is registered")

        intentFilter = createReceiverIntentFilter(SURVEY_NOTIFICATION_ACTION)
        applicationContext.registerReceiver(surveyNotificationsReceiver, intentFilter)
        Log.i(javaClass.name, "NotificationsReceiver is registered")
    }

    private fun workHelper() {

        Log.i(TAG, "Notifications worker started")
        firebaseAnalytics.logEvent(FirebaseAnalyticsEvents.NOTIFICATIONS_START, null)

        if (orgId == INVALID_ORG_ID) {
            participationStatus =
                legacyChronicleStudyApi.getParticipationStatus(studyId, participantId)
            notificationsEnabled = legacyChronicleStudyApi.isNotificationsEnabled(studyId)
            studyQuestionnaires = legacyChronicleStudyApi.getStudyQuestionnaires(studyId)

        } else {
            participationStatus = chronicleApi.getParticipationStatus(orgId, studyId, participantId)
            notificationsEnabled = chronicleApi.isNotificationsEnabled(orgId, studyId)
            studyQuestionnaires = chronicleApi.getStudyQuestionnaires(orgId, studyId)
        }
        enrollmentSettings.setParticipationStatus(
            participationStatus ?: ParticipationStatus.UNKNOWN
        )
        enrollmentSettings.setAwarenessNotificationsEnabled(notificationsEnabled ?: false)

        Log.i(javaClass.name, "Participation status: $participationStatus")
        Log.i(javaClass.name, "Study questionnaires: $studyQuestionnaires")

        // schedule/cancel daily notifications at 7.00 to redirect to app usage survey. Daily notifications should only be displayed if enabled on study and participant is enrolled
        var notification = NotificationDetails(
            studyId.toString(),
            NotificationType.AWARENESS,
            "FREQ=DAILY;BYHOUR=19;BYMINUTE=0;BYSECOND=0",
            "Chronicle Survey",
            "Tap to complete survey"
        )
        handleNotification(
            notification,
            participationStatus == ParticipationStatus.NOT_ENROLLED || notificationsEnabled == false
        )

        // handle push notifications for active questionnaires. Notifications should be cancelled if participant is not enrolled, or the questionnaire is marked as inactive
        for ((key, value) in (studyQuestionnaires ?: mapOf())) {
            val recurrenceRuleSet = value[RECURRENCE_RULE]?.iterator()?.next()?.toString()
            val name = value[NAME]?.iterator()?.next()?.toString()
            val active = value[ACTIVE]?.iterator()?.next()?.equals(true)

            if (!recurrenceRuleSet.isNullOrEmpty() && !name.isNullOrEmpty()) {
                recurrenceRuleSet.split("RRULE:").filter { it.isNotEmpty() }.forEach {
                    notification = NotificationDetails(
                        key.toString(),
                        NotificationType.QUESTIONNAIRE,
                        it,
                        name,
                        "Tap to complete questionnaire"
                    )
                    handleNotification(
                        notification,
                        active == null || !active || participationStatus == ParticipationStatus.NOT_ENROLLED
                    )
                }
            }
        }
    }

    private fun handleNotification(notification: NotificationDetails, cancel: Boolean) {
        if (cancel) {
            cancelScheduledNotification(notification)
        } else {
            scheduleNotification(notification)
        }
    }

    private fun scheduleNotification(notification: NotificationDetails) {
        Log.i(javaClass.name, "notification to schedule: $notification")

        val intent = createNotificationIntent(notification)
        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            notification.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        try {
            val date = getNextRecurringDate(notification.recurrenceRule)!!
            Log.i(javaClass.name, "notification time: $date")

            val calendar = Calendar.getInstance()
            calendar.time = date

            val alarmManager: AlarmManager =
                applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            }

        } catch (e: Exception) {
            Log.i(javaClass.name, "caught exception", e)
        }
    }

    private fun cancelScheduledNotification(notification: NotificationDetails) {
        Log.i(javaClass.name, "Notification to cancel: $notification")

        val intent = createNotificationIntent(notification)
        val pendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            notification.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE
        )

        val alarmManager: AlarmManager =
            applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }

    // generate next date from a rfc 5545 recurrence string
    // https://tools.ietf.org/html/rfc5545#section-3.3.10
    private fun getNextRecurringDate(recurrenceRule: String): Date? {
        try {
            val rule = RecurrenceRule(recurrenceRule)
            val iterator = rule.iterator(DateTime.now().timestamp, TimeZone.getDefault())
            val nextTimestamp: Long = iterator.nextMillis()
            return Date(nextTimestamp)
        } catch (e: Exception) {
            Log.i(javaClass.name, "caught exception", e)
        }
        return null
    }

    private fun createNotificationIntent(notification: NotificationDetails): Intent {
        return Intent(applicationContext, SurveyNotificationsReceiver::class.java).apply {
            putExtra(NOTIFICATION_DETAILS, Gson().toJson(notification))
            putExtra(STUDY_ID, enrollmentSettings.getStudyId().toString())
            putExtra(PARTICIPANT_ID, enrollmentSettings.getParticipantId())
            putExtra(ORGANIZATION_ID, enrollmentSettings.getOrganizationId().toString())
            action = SURVEY_NOTIFICATION_ACTION
        }
    }

    // this is required by Android 8.0 and higher. Should be called at when the app starts and
    // can be called called repeatedly after
    // ref: https://developer.android.com/training/notify-user/build-notification

    private fun createNotificationChannel() {
        Log.i(javaClass.name, "Create notification channel")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = applicationContext.resources.getString(R.string.channel_name)
            val channelDescription =
                applicationContext.resources.getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = channelDescription
            }
            //register channel
            val notificationManager: NotificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onStopped() {
        Log.i(javaClass.name, "Notifications worker has stopped")
        super.onStopped()

        // unregister receivers
        applicationContext.unregisterReceiver(unlockPhoneReceiver)
        applicationContext.unregisterReceiver(notificationDismissedReceiver)
        applicationContext.unregisterReceiver(surveyNotificationsReceiver)
    }

}

fun scheduleNotificationsWorker(context: Context) {

    val workRequest: PeriodicWorkRequest =
        PeriodicWorkRequestBuilder<NotificationsWorker>(
            NOTIFICATIONS_INTERVAL_MIN,
            TimeUnit.MINUTES
        )
            .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "notifications",
        ExistingPeriodicWorkPolicy.REPLACE,
        workRequest
    )
}
