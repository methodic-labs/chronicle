package com.openlattice.chronicle.services.upload

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import androidx.room.Room
import android.content.ComponentName
import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import com.fasterxml.jackson.databind.SerializationFeature
import com.google.common.base.Optional
import com.google.common.base.Stopwatch
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.openlattice.chronicle.ChronicleStudyApi
import com.openlattice.chronicle.api.ChronicleApi
import com.openlattice.chronicle.constants.Jobs.UPLOAD_JOB_ID
import com.openlattice.chronicle.preferences.EnrollmentSettings
import com.openlattice.chronicle.preferences.INVALID_ORG_ID
import com.openlattice.chronicle.preferences.getDevice
import com.openlattice.chronicle.preferences.getDeviceId
import com.openlattice.chronicle.serialization.JsonSerializer
import com.openlattice.chronicle.services.sinks.BrokerDataSink
import com.openlattice.chronicle.services.sinks.ConsoleSink
import com.openlattice.chronicle.services.sinks.OpenLatticeSink
import com.openlattice.chronicle.storage.ChronicleDb
import com.openlattice.chronicle.util.RetrofitBuilders
import com.openlattice.chronicle.util.RetrofitBuilders.*
import com.openlattice.chronicle.utils.Utils.isJobServiceScheduled
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import retrofit2.Retrofit
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

const val LAST_UPLOADED_PLACEHOLDER = "Never"
const val PRODUCTION = "https://api.openlattice.com/"
const val BATCH_SIZE = 100 // 24 * 60 * 60 / 5 //17280
const val LAST_UPDATED_SETTING = "com.openlattice.chronicle.upload.LastUpdated"
const val UPLOAD_PERIOD_MILLIS = 15 * 60 * 1000L

class UploadJobService : JobService() {
    private val executor = Executors.newSingleThreadExecutor()
    private val chronicleApi = createRetrofitAdapter(PRODUCTION).create(ChronicleApi::class.java)
    private val legacyChronicleApi = createRetrofitAdapter(PRODUCTION).create(com.openlattice.chronicle.ChronicleApi::class.java)
    private val legacyChronicleStudyApi = createRetrofitAdapter(PRODUCTION).create(ChronicleStudyApi::class.java)
    private val serviceId = Random().nextLong()
    private val limiter = com.google.common.util.concurrent.RateLimiter.create(10.0)
    private val crashlytics = FirebaseCrashlytics.getInstance()

    private lateinit var chronicleDb: ChronicleDb
    private lateinit var settings: EnrollmentSettings
    private lateinit var orgId: UUID
    private lateinit var studyId: UUID
    private lateinit var participantId: String
    private lateinit var deviceId: String
    private lateinit var dataSink: BrokerDataSink

    override fun onStopJob(params: JobParameters?): Boolean {
        executor.shutdown()
        executor.awaitTermination(1, TimeUnit.MINUTES)
        if (chronicleDb.isOpen) {
            chronicleDb.close()
        }
        return true
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        val latch = CountDownLatch(1)
        executor.execute {
            chronicleDb = Room.databaseBuilder(applicationContext, ChronicleDb::class.java!!, "chronicle").build()
            deviceId = getDeviceId(this)
            settings = EnrollmentSettings(applicationContext)
            orgId = settings.getOrganizationId()
            studyId = settings.getStudyId()
            participantId = settings.getParticipantId()

            dataSink = when (orgId) {
                INVALID_ORG_ID -> BrokerDataSink(
                        mutableSetOf(
                                OpenLatticeSink(studyId, participantId, deviceId, legacyChronicleApi),
                                ConsoleSink()))
                else -> BrokerDataSink(
                        mutableSetOf(
                                OpenLatticeSink(orgId, studyId, participantId, deviceId, chronicleApi),
                                ConsoleSink()))
            }

            Log.i("${javaClass.name}-$serviceId", "Job service is initialized")
            latch.countDown()
        }

        latch.await()
        Log.i("${javaClass.name}-$serviceId", "Upload job service is running with batch size " + BATCH_SIZE.toString())

        executor.execute {
            if (chronicleApi.isRunning == true) {
                if (deviceId.isNullOrBlank() || studyId == null || participantId.isNullOrBlank()) {
                    crashlytics.log("studyId: \"$studyId\" ; participantId: \"$participantId\" ; deviceId: \"$deviceId\"")
                }

                var chronicleId: UUID? = null
                try {

                    chronicleId = when (orgId) {
                        INVALID_ORG_ID ->  legacyChronicleStudyApi.enrollSource(studyId, participantId, deviceId, Optional.of(getDevice(deviceId)))
                        else -> chronicleApi.enroll(orgId, studyId, participantId, deviceId, Optional.of(getDevice(deviceId)))
                    }

                } catch (e: Exception) {
                    addLogMessage()
                    FirebaseCrashlytics.getInstance().recordException(e)
                }

                //Only run the upload job if the device is already enrolled or we are able to properly enroll.
                if (chronicleId != null) {

                    val queue = chronicleDb.queueEntryData()
                    var nextEntries = queue.getNextEntries(BATCH_SIZE)
                    var notEmptied = nextEntries.isNotEmpty()
                    while (notEmptied && chronicleApi.isRunning) {
                        limiter.acquire()
                        val w = Stopwatch.createStarted()
                        val data = nextEntries
                                .map { qe -> qe.data }
                                .map { qe -> JsonSerializer.deserializeQueueEntry(qe) }
                                .flatMap { it }
                        Log.d("${javaClass.name}-$serviceId", "Loading $BATCH_SIZE items from queue took ${w.elapsed(TimeUnit.MILLISECONDS)} millis")
                        w.reset()
                        w.start()
                        if (dataSink.submit(data)[OpenLatticeSink::class.java.name] == true) {
                            setLastUpload(this)
                            Log.d("${javaClass.name}-$serviceId", "Uploading ${data.size} to OpenLattice items from queue took ${w.elapsed(TimeUnit.MILLISECONDS)} millis")
                            queue.deleteEntries(nextEntries)
                            nextEntries = queue.getNextEntries(BATCH_SIZE)
                            notEmptied = nextEntries.size == BATCH_SIZE
                        }
                    }
                    chronicleDb.close()
                    jobFinished(params, false)
                }
            }
        }
        return true
    }

    private fun addLogMessage() {
        if (orgId == INVALID_ORG_ID) {
            crashlytics.log("caught exception - studyId: \"$studyId\" ; participantId: \"$participantId\"")
        } else {
            crashlytics.log("caught exception - orgId: \"$orgId\" ; studyId: \"$studyId\" ; participantId: \"$participantId\"")
        }
    }
}

fun setLastUpload(context: Context) {
    val settings = PreferenceManager.getDefaultSharedPreferences(context)
    with(settings.edit()) {
        putString(LAST_UPDATED_SETTING, DateTime.now().toString())
        apply()
    }
}

fun getLastUpload(context: Context): String {

    val settings = PreferenceManager.getDefaultSharedPreferences(context)
    val lastUpdated = settings.getString(LAST_UPDATED_SETTING, LAST_UPLOADED_PLACEHOLDER)

    if (lastUpdated != LAST_UPLOADED_PLACEHOLDER) {
        return DateTime.parse(lastUpdated).toString(DateTimeFormat.mediumDateTime())
    }

    return lastUpdated
}

fun createRetrofitAdapter(baseUrl: String): Retrofit {
    RetrofitBuilders.mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    val httpClient = okHttpClient().build()
    return decorateWithRhizomeFactories(createBaseChronicleRetrofitBuilder(baseUrl, httpClient)).build()
}

fun scheduleUploadJob(context: Context) {
    if (!isJobServiceScheduled(context, UPLOAD_JOB_ID.id)) {
        val serviceComponent = ComponentName(context, UploadJobService::class.java)
        val jobBuilder = JobInfo.Builder(UPLOAD_JOB_ID.id, serviceComponent)
        jobBuilder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        jobBuilder.setPeriodic(UPLOAD_PERIOD_MILLIS)
        jobBuilder.setPersisted(true)
        val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
        jobScheduler.schedule(jobBuilder.build())
    }
}

fun cancelUploadJobScheduler (context: Context) {
    val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
    jobScheduler.cancel(UPLOAD_JOB_ID.id)
}

