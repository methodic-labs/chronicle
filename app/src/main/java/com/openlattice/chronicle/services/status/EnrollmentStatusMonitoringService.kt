package com.openlattice.chronicle.services.status

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import com.crashlytics.android.Crashlytics
import com.openlattice.chronicle.ChronicleStudyApi
import com.openlattice.chronicle.data.ParticipationStatus
import com.openlattice.chronicle.preferences.EnrollmentSettings
import com.openlattice.chronicle.services.upload.PRODUCTION
import com.openlattice.chronicle.constants.Jobs.MONITOR_PARTICIPATION_STATUS_JOB_ID
import com.openlattice.chronicle.services.notifications.NOTIFICATIONS_ENABLED
import com.openlattice.chronicle.services.notifications.NotificationsService
import com.openlattice.chronicle.services.upload.cancelUploadJobScheduler
import com.openlattice.chronicle.services.upload.createRetrofitAdapter
import com.openlattice.chronicle.services.upload.scheduleUploadJob
import com.openlattice.chronicle.services.usage.cancelUsageMonitoringJobScheduler
import com.openlattice.chronicle.services.usage.scheduleUsageMonitoringJob
import io.fabric.sdk.android.Fabric
import java.lang.Exception
import java.util.*
import java.util.concurrent.Executors

const val STATUS_CHECK_PERIOD_MILLIS = 15 * 60 * 1000L

class EnrollmentStatusMonitoringService : JobService() {
    private val executor = Executors.newSingleThreadExecutor()
    private val chronicleStudyApi = createRetrofitAdapter(PRODUCTION).create(ChronicleStudyApi::class.java)

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
            val studyId :UUID = enrollmentSettings.getStudyId()
            val participantId :String = enrollmentSettings.getParticipantId()

            var participationStatus = enrollmentSettings.getParticipationStatus()
            var notificationsEnabled = enrollmentSettings.getNotificationsEnabled()

            try {
                participationStatus = chronicleStudyApi.getParticipationStatus(studyId, participantId)
                notificationsEnabled = chronicleStudyApi.isNotificationsEnabled(studyId)

            } catch (e :Exception) {
                Crashlytics.log("caught exception: studyId: \"$studyId\" participantId: \"$participantId\"")
                Crashlytics.logException(e)
            }
            Log.i(javaClass.name, "Participation status: $participationStatus")

            if (participationStatus == ParticipationStatus.ENROLLED) {
                scheduleUploadJob(this)
                scheduleUsageMonitoringJob(this)
            } else {
                cancelUsageMonitoringJobScheduler(this)
                cancelUploadJobScheduler(this)
            }

            // schedule notification
            val intent = Intent(this, NotificationsService::class.java)
            intent.putExtra(NOTIFICATIONS_ENABLED, participationStatus == ParticipationStatus.ENROLLED && notificationsEnabled)
            NotificationsService.enqueueWork(this, intent)

            enrollmentSettings.setParticipationStatus(participationStatus)
            enrollmentSettings.setNotificationsEnabled(notificationsEnabled)

            jobFinished(parameters, false)
        }
        return true
    }
}

fun scheduleParticipationStatusJob(context :Context) {
    val serviceComponent = ComponentName(context, EnrollmentStatusMonitoringService::class.java)
    val jobBuilder = JobInfo.Builder(MONITOR_PARTICIPATION_STATUS_JOB_ID.id, serviceComponent)
    jobBuilder.setPersisted(true)
    jobBuilder.setPeriodic(STATUS_CHECK_PERIOD_MILLIS)
    jobBuilder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
    val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
    jobScheduler.schedule(jobBuilder.build())
}