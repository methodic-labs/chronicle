package com.openlattice.chronicle.services.upload

import android.app.job.JobParameters
import android.app.job.JobService
import android.arch.persistence.room.Room
import android.provider.Settings
import android.util.Log
import com.google.common.base.Stopwatch
import com.openlattice.chronicle.ChronicleApi
import com.openlattice.chronicle.preferences.EnrollmentSettings
import com.openlattice.chronicle.serialization.JsonSerializer
import com.openlattice.chronicle.services.sinks.BrokerDataSink
import com.openlattice.chronicle.storage.ChronicleDb
import com.openlattice.chronicle.util.RetrofitBuilders.*
import retrofit2.Retrofit
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

const val PRODUCTION = "https://api.openlattice.com/"
const val BATCH_SIZE = 10 // 24 * 60 * 60 / 5 //17280

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
        chronicleDb.close()
        executor.shutdown()
        executor.awaitTermination(1, TimeUnit.MINUTES)
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
            while (nextEntries.size >= BATCH_SIZE) {
                val w = Stopwatch.createStarted()
                val data = nextEntries
                        .map { qe -> qe.data }
                        .map { qe -> JsonSerializer.deserializeQueueEntry(qe) }
                        .flatMap { it }
                Log.d(javaClass.name, "Loading $BATCH_SIZE items from queue took ${w.elapsed(TimeUnit.MILLISECONDS)} millis")
                w.reset()
                w.start()
                 dataSink.submit(data)

                Log.d(javaClass.name, "Uploading ${data.size} to OpenLattice items from queue took ${w.elapsed(TimeUnit.MILLISECONDS)} millis")
                queue.deleteEntries(nextEntries)
                nextEntries = queue.getNextEntries(BATCH_SIZE)
            }
            jobFinished(params, false)
        })
        return true
    }

}

fun createRetrofitAdapter(baseUrl: String): Retrofit {
    val httpClient = okHttpClient().build()
    return decorateWithRhizomeFactories(createBaseChronicleRetrofitBuilder(baseUrl, httpClient)).build()
}