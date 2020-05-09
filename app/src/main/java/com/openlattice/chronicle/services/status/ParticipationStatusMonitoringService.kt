package com.openlattice.chronicle.services.status

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.AsyncTask
import android.util.Log
import com.crashlytics.android.Crashlytics
import com.openlattice.chronicle.ChronicleStudyApi
import com.openlattice.chronicle.data.ParticipationStatus
import com.openlattice.chronicle.preferences.EnrollmentSettings
import com.openlattice.chronicle.services.upload.PRODUCTION
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

class ParticipationStatusMonitoringService : JobService() {
    private val executor = Executors.newSingleThreadExecutor()
    private val chronicleStudyApi = createRetrofitAdapter(PRODUCTION).create(ChronicleStudyApi::class.java)

    override fun onCreate() {
        super.onCreate()
        Fabric.with(this, Crashlytics())
    }
    override fun onStopJob(p0: JobParameters?): Boolean {
        Log.i(javaClass.name, "Participation status service stopped")
        return true
    }

    override fun onStartJob(p0: JobParameters?): Boolean {
        Log.i(javaClass.name, "Participation status service initialized")
        val enrollmentSettings = EnrollmentSettings(applicationContext)
        val studyId :UUID = enrollmentSettings.getStudyId()
        val participantId :String = enrollmentSettings.getParticipantId()

        executor.execute {
            var participationStatus: ParticipationStatus
            try {
                participationStatus = chronicleStudyApi.getParticipationStatus(studyId, participantId)
            } catch (e :Exception) {
                Log.e(javaClass.name, "Error retrieving participation status")
                participationStatus = ParticipationStatus.UNKNOWN
            }
            enrollmentSettings.setParticipationStatus(participationStatus)

            if (participationStatus == ParticipationStatus.ENROLLED) {
                scheduleUploadJob(this)
                scheduleUsageMonitoringJob(this)
            } else {
                cancelUsageMonitoringJobScheduler(this)
                cancelUploadJobScheduler(this)
            }
        }
        return true
    }
}

fun scheduleParticipationStatusJob(context :Context) {
    val serviceComponent = ComponentName(context, ParticipationStatusMonitoringService::class.java)
    val jobBuilder = JobInfo.Builder(0, serviceComponent)
    jobBuilder.setPersisted(true)
    jobBuilder.setPeriodic(STATUS_CHECK_PERIOD_MILLIS)
    jobBuilder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
    val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
    jobScheduler.schedule(jobBuilder.build())
}