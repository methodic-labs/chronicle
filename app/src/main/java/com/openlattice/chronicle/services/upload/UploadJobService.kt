package com.openlattice.chronicle.services.upload

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.arch.persistence.room.Room
import android.content.ComponentName
import android.content.Context
import android.preference.PreferenceManager
import android.provider.Settings
import android.util.Log
import com.google.common.base.Stopwatch
import com.openlattice.chronicle.ChronicleApi
import com.openlattice.chronicle.preferences.EnrollmentSettings
import com.openlattice.chronicle.serialization.JsonSerializer
import com.openlattice.chronicle.services.sinks.BrokerDataSink
import com.openlattice.chronicle.storage.ChronicleDb
import com.openlattice.chronicle.util.RetrofitBuilders.*
import org.joda.time.LocalDateTime
import retrofit2.Retrofit
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

const val PRODUCTION = "https://api.openlattice.com/"
const val BATCH_SIZE = 100 // 24 * 60 * 60 / 5 //17280
const val LAST_UPDATED_SETTING = "com.openlattice.chronicle.upload.LastUpdated"

class UploadJobService : JobService() {
    private val executor = Executors.newSingleThreadExecutor()
    private val chronicleApi = createRetrofitAdapter(PRODUCTION).create(ChronicleApi::class.java)

    private lateinit var chronicleDb: ChronicleDb
    private lateinit var settings: EnrollmentSettings
    private lateinit var studyId: UUID
    private lateinit var participantId: String
    private lateinit var deviceId: String
    private lateinit var dataSink: BrokerDataSink

    override fun onStopJob(params: JobParameters?): Boolean {
        executor.shutdown()
        executor.awaitTermination(1, TimeUnit.MINUTES)
        chronicleDb.close()
        return true
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        executor.execute {
            chronicleDb = Room.databaseBuilder(applicationContext, ChronicleDb::class.java!!, "chronicle").build()
            deviceId = Settings.Secure.getString(applicationContext.getContentResolver(), Settings.Secure.ANDROID_ID)
            settings = EnrollmentSettings(applicationContext)
            studyId = settings.getStudyId()
            participantId = settings.getParticipantId()
            dataSink = BrokerDataSink(kotlin.collections.mutableSetOf(com.openlattice.chronicle.services.sinks.OpenLatticeSink(studyId, participantId, deviceId, chronicleApi)))
            Log.i(javaClass.name, "Job service is initialized")
        }

        Log.i(javaClass.name, "Upload job service is running with batch size " + BATCH_SIZE.toString())

        executor.execute({
            val queue = chronicleDb.queueEntryData()
            var nextEntries = queue.getNextEntries(BATCH_SIZE)
            var notEmptied = nextEntries.isNotEmpty()
            while (notEmptied) {
                val w = Stopwatch.createStarted()
                val data = nextEntries
                        .map { qe -> qe.data }
                        .map { qe -> JsonSerializer.deserializeQueueEntry(qe) }
                        .flatMap { it }
                Log.d(javaClass.name, "Loading $BATCH_SIZE items from queue took ${w.elapsed(TimeUnit.MILLISECONDS)} millis")
                w.reset()
                w.start()
                dataSink.submit(data)
                setLastUpload(this)
                Log.d(javaClass.name, "Uploading ${data.size} to OpenLattice items from queue took ${w.elapsed(TimeUnit.MILLISECONDS)} millis")
                queue.deleteEntries(nextEntries)
                nextEntries = queue.getNextEntries(BATCH_SIZE)
                notEmptied = nextEntries.size == BATCH_SIZE
            }
            jobFinished(params, false)
        })
        return true
    }

}

fun setLastUpload(context: Context) {
    val settings = PreferenceManager.getDefaultSharedPreferences(context)
    with(settings.edit()) {
        putString(LAST_UPDATED_SETTING, LocalDateTime.now().toString())
        apply()
    }

}

fun getLastUpload(context: Context): String {
    val settings = PreferenceManager.getDefaultSharedPreferences(context)
    return settings.getString(LAST_UPDATED_SETTING, "Never")
}

fun createRetrofitAdapter(baseUrl: String): Retrofit {
    val httpClient = okHttpClient().build()
    return decorateWithRhizomeFactories(createBaseChronicleRetrofitBuilder(baseUrl, httpClient)).build()
}

fun scheduleUploadJob(context: Context) {
    val serviceComponent = ComponentName(context, UploadJobService::class.java)
    val jobBuilder = JobInfo.Builder(0, serviceComponent)
    jobBuilder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
    jobBuilder.setPeriodic(UPLOAD_PERIOD_MILLIS)
    jobBuilder.setPersisted(true)
    val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
    jobScheduler.schedule(jobBuilder.build())
}

const val UPLOAD_PERIOD_MILLIS =  15 * 60 * 1000L