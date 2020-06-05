package com.openlattice.chronicle.services.status

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.arch.persistence.room.Room
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Log
import com.crashlytics.android.Crashlytics
import com.google.common.collect.ImmutableMap
import com.google.gson.Gson
import com.openlattice.chronicle.ChronicleStudyApi
import com.openlattice.chronicle.constants.Jobs.MONITOR_PARTICIPATION_STATUS_JOB_ID
import com.openlattice.chronicle.constants.NotificationType
import com.openlattice.chronicle.data.ParticipationStatus
import com.openlattice.chronicle.preferences.EnrollmentSettings
import com.openlattice.chronicle.sensors.ACTIVE
import com.openlattice.chronicle.sensors.CRON
import com.openlattice.chronicle.sensors.NAME
import com.openlattice.chronicle.sensors.PROPERTY_TYPES
import com.openlattice.chronicle.services.notifications.NOTIFICATIONS_ENABLED
import com.openlattice.chronicle.services.notifications.NOTIFICATION_ENTRY
import com.openlattice.chronicle.services.notifications.NotificationsService
import com.openlattice.chronicle.services.upload.PRODUCTION
import com.openlattice.chronicle.services.upload.cancelUploadJobScheduler
import com.openlattice.chronicle.services.upload.createRetrofitAdapter
import com.openlattice.chronicle.services.upload.scheduleUploadJob
import com.openlattice.chronicle.services.usage.cancelUsageMonitoringJobScheduler
import com.openlattice.chronicle.services.usage.scheduleUsageMonitoringJob
import com.openlattice.chronicle.storage.ChronicleDb
import com.openlattice.chronicle.storage.Notification
import com.openlattice.chronicle.storage.NotificationDao
import io.fabric.sdk.android.Fabric
import org.apache.olingo.commons.api.edm.FullQualifiedName
import org.springframework.scheduling.support.CronSequenceGenerator
import java.util.*
import java.util.concurrent.Executors
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

const val STATUS_CHECK_PERIOD_MILLIS = 15 * 60 * 1000L
const val SAVED_NOTIFICATIONS = "savedNotifications"

class EnrollmentStatusMonitoringService : JobService() {
    private val executor = Executors.newSingleThreadExecutor()
    private val chronicleStudyApi = createRetrofitAdapter(PRODUCTION).create(ChronicleStudyApi::class.java)
    private var activeQuestionnaires: Map<UUID, Map<FullQualifiedName, Set<Any>>> = HashMap()
    private var notificationEntries: MutableList<Notification> = mutableListOf()


    override fun onCreate() {
        super.onCreate()
        Fabric.with(this, Crashlytics())
    }

    override fun onStopJob(p0: JobParameters?): Boolean {
        Log.i(javaClass.name, "Enrollment status service stopped")
        executor.shutdown()
        return true // reschedule the job
    }

    override fun onStartJob(parameters: JobParameters?): Boolean {
        Log.i(javaClass.name, "Enrollment status service initialized")

        executor.execute {

            val enrollmentSettings = EnrollmentSettings(applicationContext)
            val studyId: UUID = enrollmentSettings.getStudyId()
            val participantId: String = enrollmentSettings.getParticipantId()

            var participationStatus = enrollmentSettings.getParticipationStatus()
            var notificationsEnabled = enrollmentSettings.getAwarenessNotificationsEnabled()

            try {
                participationStatus = chronicleStudyApi.getParticipationStatus(studyId, participantId)
                notificationsEnabled = chronicleStudyApi.isNotificationsEnabled(studyId)
                activeQuestionnaires = chronicleStudyApi.getActiveQuestionnaires(studyId)

            } catch (e: Exception) {
                Crashlytics.log("caught exception: studyId: \"$studyId\" participantId: \"$participantId\"")
                Crashlytics.logException(e)
            }
            Log.i(javaClass.name, "Participation status: $participationStatus")
            Log.i(javaClass.name, "active questionnaires: $activeQuestionnaires")

            if (participationStatus == ParticipationStatus.ENROLLED) {
                scheduleUploadJob(this)
                scheduleUsageMonitoringJob(this)
            } else {
                cancelUsageMonitoringJobScheduler(this)
                cancelUploadJobScheduler(this)
            }

            var notification = Notification(
                    studyId.toString(),
                    NotificationType.AWARENESS,
                    "0 0 19 * * *",
                    "Chronicle Survey",
                    "Tap to complete survey"
            )
            var intent = Intent(this, NotificationsService::class.java).apply {
                putExtra(NOTIFICATION_ENTRY, Gson().toJson(notification))
                putExtra(NOTIFICATIONS_ENABLED, participationStatus == ParticipationStatus.ENROLLED && notificationsEnabled)
            }

            NotificationsService.enqueueWork(this, intent)

            // schedule notifications for active questionnaires
            for ((key, value) in activeQuestionnaires) {
                val cron = value[FullQualifiedName(CRON)]?.iterator()?.next()?.toString()
                val name = value[FullQualifiedName(NAME)]?.iterator()?.next()?.toString()
                val active = value[FullQualifiedName(ACTIVE)]?.iterator()?.next()?.equals(true)

                Log.i(javaClass.name, "questionnaire $key -> active ? $active")

                if (!cron.isNullOrEmpty() && !name.isNullOrEmpty()) {
                    notification = Notification(
                            key.toString(),
                            NotificationType.QUESTIONNAIRE,
                            cron,
                            name,
                            "Tap to complete questionnaire"
                    )
                    notificationEntries.add(notification)

                    intent = Intent(this, NotificationsService::class.java).apply {
                        putExtra(NOTIFICATION_ENTRY,  Gson().toJson(notification))
                        putExtra(NOTIFICATIONS_ENABLED, active != null && active)
                    }
                    NotificationsService.enqueueWork(this, intent)
                }
            }

            enrollmentSettings.setParticipationStatus(participationStatus)
            enrollmentSettings.setAwarenessNotificationsEnabled(notificationsEnabled)

            jobFinished(parameters, false)
        }
        return true
    }
}



fun scheduleEnrollmentStatusJob(context: Context) {
    val serviceComponent = ComponentName(context, EnrollmentStatusMonitoringService::class.java)
    val jobBuilder = JobInfo.Builder(MONITOR_PARTICIPATION_STATUS_JOB_ID.id, serviceComponent)
    jobBuilder.setPersisted(true)
    jobBuilder.setPeriodic(STATUS_CHECK_PERIOD_MILLIS)
    jobBuilder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
    val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
    jobScheduler.schedule(jobBuilder.build())
}
