package com.openlattice.chronicle.services.notifications

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.*
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.Gson
import com.openlattice.chronicle.ChronicleStudyApi
import com.openlattice.chronicle.api.ChronicleApi
import com.openlattice.chronicle.constants.NotificationType
import com.openlattice.chronicle.data.ParticipationStatus
import com.openlattice.chronicle.preferences.EnrollmentSettings
import com.openlattice.chronicle.preferences.INVALID_ORG_ID
import com.openlattice.chronicle.sensors.ACTIVE
import com.openlattice.chronicle.sensors.NAME
import com.openlattice.chronicle.sensors.RECURRENCE_RULE
import com.openlattice.chronicle.services.notifications.NotificationsService
import com.openlattice.chronicle.services.upload.PRODUCTION
import com.openlattice.chronicle.utils.Utils
import org.apache.olingo.commons.api.edm.FullQualifiedName
import java.util.*
import java.util.concurrent.TimeUnit

const val NOTIFICATIONS_INTERVAL_MIN = 15L
val TAG = NotificationsWorker::class.java.simpleName

class NotificationsWorker(context: Context, workerParameters: WorkerParameters) : Worker(context, workerParameters) {
    private val chronicleApi = Utils.createRetrofitAdapter(PRODUCTION).create(ChronicleApi::class.java)
    private val legacyChronicleStudyApi = Utils.createRetrofitAdapter(PRODUCTION).create(ChronicleStudyApi::class.java) // legacy studies

    private lateinit var crashlytics: FirebaseCrashlytics
    private lateinit var enrollmentSettings: EnrollmentSettings
    private lateinit var studyId: UUID
    private lateinit var participantId: String
    private lateinit var orgId: UUID
    private lateinit var participationStatus: ParticipationStatus
    private lateinit var studyQuestionnaires: Map<UUID, Map<FullQualifiedName, Set<Any>>>

    private var notificationsEnabled = false

    override fun doWork(): Result {

        Log.i( TAG,"Notifications worker started")

        try {
            enrollmentSettings = EnrollmentSettings(applicationContext)
            studyId = enrollmentSettings.getStudyId()
            participantId = enrollmentSettings.getParticipantId()
            orgId = enrollmentSettings.getOrganizationId()
            participationStatus = enrollmentSettings.getParticipationStatus()
            notificationsEnabled = enrollmentSettings.getAwarenessNotificationsEnabled()
            crashlytics = FirebaseCrashlytics.getInstance()

            workHelper()
        } catch (e: Exception) {
            crashlytics.recordException(e)
            return Result.failure()
        }
        return Result.success()
    }

    private fun workHelper() {

        if (orgId == INVALID_ORG_ID) {
            participationStatus = legacyChronicleStudyApi.getParticipationStatus(studyId, participantId)
            notificationsEnabled = legacyChronicleStudyApi.isNotificationsEnabled(studyId)
            studyQuestionnaires = legacyChronicleStudyApi.getStudyQuestionnaires(studyId)

        } else {
            participationStatus = chronicleApi.getParticipationStatus(orgId, studyId, participantId)
            notificationsEnabled = chronicleApi.isNotificationsEnabled(orgId, studyId)
            studyQuestionnaires = chronicleApi.getStudyQuestionnaires(orgId, studyId)
        }

        Log.i(javaClass.name, "Participation status: $participationStatus")
        Log.i(javaClass.name, "Study questionnaires: $studyQuestionnaires")

        var notification = NotificationEntry(
                studyId.toString(),
                NotificationType.AWARENESS,
                "FREQ=DAILY;BYHOUR=19;BYMINUTE=0;BYSECOND=0",
                "Chronicle Survey",
                "Tap to complete survey"
        )
        var intent = Intent(applicationContext, NotificationsService::class.java).apply {
            putExtra(NOTIFICATION_ENTRY, Gson().toJson(notification))
            putExtra(NOTIFICATIONS_ENABLED, participationStatus == ParticipationStatus.ENROLLED && notificationsEnabled)
        }

        NotificationsService.enqueueWork(applicationContext, intent)

        // schedule notifications for active questionnaires
        for ((key, value) in studyQuestionnaires) {
            val recurrenceRuleSet = value[RECURRENCE_RULE]?.iterator()?.next()?.toString()
            val name = value[NAME]?.iterator()?.next()?.toString()
            val active = value[ACTIVE]?.iterator()?.next()?.equals(true)

            if (!recurrenceRuleSet.isNullOrEmpty() && !name.isNullOrEmpty()) {
                recurrenceRuleSet.split("RRULE:").filter { it.isNotEmpty() }.forEach {
                    notification = NotificationEntry(
                            key.toString(),
                            NotificationType.QUESTIONNAIRE,
                            it,
                            name,
                            "Tap to complete questionnaire"
                    )
                    intent = Intent(applicationContext, NotificationsService::class.java).apply {
                        putExtra(NOTIFICATION_ENTRY, Gson().toJson(notification))
                        putExtra(NOTIFICATIONS_ENABLED, active != null && active && participationStatus == ParticipationStatus.ENROLLED)
                    }
                    NotificationsService.enqueueWork(applicationContext, intent)
                }
            }
        }

        enrollmentSettings.setParticipationStatus(participationStatus)
        enrollmentSettings.setAwarenessNotificationsEnabled(notificationsEnabled)
    }
}

fun scheduleNotificationsWorker(context: Context) {

    val workRequest: PeriodicWorkRequest =
            PeriodicWorkRequestBuilder<NotificationsWorker>(NOTIFICATIONS_INTERVAL_MIN, TimeUnit.MINUTES)
                    .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork("notifications",
            ExistingPeriodicWorkPolicy.REPLACE, // cancel cancelling
            workRequest
    )
}