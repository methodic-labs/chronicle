package com.openlattice.chronicle.services.status

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.AsyncTask
import android.util.Log
import com.openlattice.chronicle.ChronicleStudyApi
import com.openlattice.chronicle.data.ParticipationStatus
import com.openlattice.chronicle.preferences.EnrollmentSettings
import com.openlattice.chronicle.services.upload.PRODUCTION
import com.openlattice.chronicle.services.upload.cancelUploadJobScheduler
import com.openlattice.chronicle.services.upload.createRetrofitAdapter
import com.openlattice.chronicle.services.upload.scheduleUploadJob
import com.openlattice.chronicle.services.usage.cancelUsageMonitoringJobScheduler
import com.openlattice.chronicle.services.usage.scheduleUsageMonitoringJob
import java.lang.Exception
import java.util.*

const val STATUS_CHECK_PERIOD_MILLIS = 15 * 60 * 1000L

class ParticipationStatusMonitoringService : JobService() {
    override fun onStopJob(p0: JobParameters?): Boolean {
        return true
    }

    override fun onStartJob(p0: JobParameters?): Boolean {
        val enrollmentSettings = EnrollmentSettings(applicationContext)
        val studyId :UUID = enrollmentSettings.getStudyId()
        val participantId :String = enrollmentSettings.getParticipantId()

        EnrollmentStatusTask(studyId, participantId, enrollmentSettings, applicationContext).execute()
        return true
    }

    class EnrollmentStatusTask(private val studyId :UUID, private val participantId :String, private val enrollmentSettings: EnrollmentSettings, private val context :Context) : AsyncTask<Objects, Void, ParticipationStatus>() {
        private val chronicleStudyApi = createRetrofitAdapter(PRODUCTION).create(ChronicleStudyApi::class.java)

        override fun doInBackground(vararg params: Objects?): ParticipationStatus {
            val participationStatus: ParticipationStatus
            try {
                participationStatus = chronicleStudyApi.getParticipationStatus(studyId, participantId)
            } catch (e :Exception) {
                return ParticipationStatus.UNKNOWN
            }
            return participationStatus
        }

        override fun onPostExecute(result: ParticipationStatus) {
            super.onPostExecute(result)
            Log.i(javaClass.name, "Status of participant $participantId in study ${studyId}: $result")
            enrollmentSettings.setParticipationStatus(result)

            if (result == ParticipationStatus.ENROLLED) {
                scheduleUploadJob(context)
                scheduleUsageMonitoringJob(context)
            } else {
                cancelUsageMonitoringJobScheduler(context)
                cancelUploadJobScheduler(context)
            }
        }
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