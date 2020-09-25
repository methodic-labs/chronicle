package com.openlattice.chronicle.services.upload

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.arch.persistence.room.Room
import android.content.ComponentName
import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import com.crashlytics.android.Crashlytics
import com.fasterxml.jackson.databind.SerializationFeature
import com.google.common.base.Optional
import com.google.common.base.Stopwatch
import com.openlattice.chronicle.ChronicleApi
import com.openlattice.chronicle.ChronicleStudyApi
import com.openlattice.chronicle.constants.Jobs.UPLOAD_JOB_ID
import com.openlattice.chronicle.preferences.EnrollmentSettings
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
import io.fabric.sdk.android.Fabric
import org.joda.time.DateTime
import retrofit2.Retrofit
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

const val PRODUCTION = "https://api.openlattice.com/"
const val BATCH_SIZE = 100 // 24 * 60 * 60 / 5 //17280
const val LAST_UPDATED_SETTING = "com.openlattice.chronicle.upload.LastUpdated"
const val UPLOAD_PERIOD_MILLIS = 15 * 60 * 1000L

class UploadJobService : JobService() {
    private val executor = Executors.newSingleThreadExecutor()
    private val chronicleApi = createRetrofitAdapter(PRODUCTION).create(ChronicleApi::class.java)
    private val chronicleStudyApi = createRetrofitAdapter(PRODUCTION).create(ChronicleStudyApi::class.java)
    private val serviceId = Random().nextLong()
    private val limiter = com.google.common.util.concurrent.RateLimiter.create(10.0)

    private lateinit var chronicleDb: ChronicleDb
    private lateinit var settings: EnrollmentSettings
    private lateinit var studyId: UUID
    private lateinit var participantId: String
    private lateinit var deviceId: String
    private lateinit var dataSink: BrokerDataSink

    override fun onCreate() {
        super.onCreate()
        Fabric.with(this, Crashlytics())
    }

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
            studyId = settings.getStudyId()
            participantId = settings.getParticipantId()
            dataSink = BrokerDataSink(
                    mutableSetOf(
                            OpenLatticeSink(studyId, participantId, deviceId, chronicleApi),
                            ConsoleSink()))
            Log.i("${javaClass.name}-$serviceId", "Job service is initialized")
            latch.countDown()
        }

        latch.await()
        Log.i("${javaClass.name}-$serviceId", "Upload job service is running with batch size " + BATCH_SIZE.toString())

        executor.execute {
            if (chronicleApi.isRunning == true) {
                val deviceId = getDeviceId(applicationContext)
                val studyId = settings.getStudyId()
                val participantId = settings.getParticipantId()

                if (deviceId.isNullOrBlank() || studyId == null || participantId.isNullOrBlank()) {
                    Crashlytics.log("studyId: \"$studyId\" ; participantId: \"$participantId\" ; deviceId: \"$deviceId\"")
                }

                var isKnown = false
                var chronicleId: UUID? = null
                try {
                    isKnown = chronicleStudyApi.isKnownDatasource(studyId, participantId, deviceId)
                    chronicleId = chronicleStudyApi.enrollSource(studyId, participantId, deviceId, Optional.of(getDevice(deviceId)))
                } catch (e: Exception) {
                    Crashlytics.log("caught exception - studyId: \"$studyId\" ; participantId: \"$participantId\"")
                    Crashlytics.logException(e)
                }

                //Only run the upload job if the device is already enrolled or we are able to properly enroll.
                if (isKnown || chronicleId != null) {

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
    return settings.getString(LAST_UPDATED_SETTING, "Never")
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

