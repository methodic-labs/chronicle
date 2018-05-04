package com.openlattice.chronicle.services.upload

import android.app.job.JobParameters
import android.app.job.JobService
import android.arch.persistence.room.Room
import android.provider.Settings
import android.util.Log
import com.openlattice.chronicle.ChronicleApi
import com.openlattice.chronicle.preferences.EnrollmentSettings
import com.openlattice.chronicle.services.sinks.BrokerDataSink
import com.openlattice.chronicle.services.sinks.OpenLatticeSink
import com.openlattice.chronicle.storage.ChronicleDb
import com.openlattice.chronicle.serialization.JsonSerializer
import com.openlattice.chronicle.util.RetrofitBuilders.*
import retrofit2.Retrofit
import java.util.*
import java.util.concurrent.Executors

const val PRODUCTION = "https://api.openlattice.com/"
const val BATCH_SIZE = 24 * 60 * 60 / 5 //17280

class UploadJobService : JobService() {
    val executor = Executors.newFixedThreadPool(1);
    val chronicleApi = createRetrofitAdapter(PRODUCTION).create(ChronicleApi::class.java)

    private lateinit var chronicleDb: ChronicleDb
    private lateinit var settings: EnrollmentSettings
    lateinit var studyId: UUID
    lateinit var participantId: String
    lateinit var deviceId: String
    lateinit var dataSink: BrokerDataSink

    override fun onCreate() {
        super.onCreate()
        chronicleDb = Room.databaseBuilder(applicationContext, ChronicleDb::class.java!!, "chronicle").build()
        deviceId = Settings.Secure.getString(applicationContext.getContentResolver(), Settings.Secure.ANDROID_ID)
        settings = EnrollmentSettings(applicationContext)
        studyId = settings.getStudyId()
        participantId = settings.getParticipantId()
        dataSink = BrokerDataSink(kotlin.collections.mutableSetOf(com.openlattice.chronicle.services.sinks.OpenLatticeSink(studyId, participantId, deviceId, chronicleApi)))

    }

    override fun onStopJob(params: JobParameters?): Boolean {
        return true
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        Log.i(javaClass.name, "Upload job service kicking upload job with batch size " + BATCH_SIZE.toString())
        executor.execute({
            val queue = chronicleDb.queueEntryData()
            var nextEntries = queue.getNextEntries(BATCH_SIZE)
            while (nextEntries.size >= BATCH_SIZE) {
                val data = nextEntries
                        .map { qe -> qe.data }
                        .map { qe -> JsonSerializer.deserializeQueueEntry(qe) }

                dataSink.submit(data)
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