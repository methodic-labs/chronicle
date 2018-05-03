package com.openlattice.chronicle.services.upload

import android.app.job.JobParameters
import android.app.job.JobService
import android.arch.persistence.room.Room
import android.provider.Settings
import com.openlattice.chronicle.ChronicleApi
import com.openlattice.chronicle.preferences.EnrollmentSettings
import com.openlattice.chronicle.services.sinks.BrokerDataSink
import com.openlattice.chronicle.services.sinks.OpenLatticeSink
import com.openlattice.chronicle.storage.ChronicleDb
import com.openlattice.chronicle.serialization.JsonSerializer
import com.openlattice.chronicle.util.RetrofitBuilders.*
import retrofit2.Retrofit
import java.util.concurrent.Executors

const val PRODUCTION = "https://api.openlattice.com/"

class UploadJobService() : JobService() {
    val executor = Executors.newFixedThreadPool(1);
    private val chronicleDb = Room.databaseBuilder(applicationContext, ChronicleDb::class.java!!, "chronicle").build()
    private val settings = EnrollmentSettings(applicationContext)

    val studyId = settings.getStudyId()
    val participantId = settings.getParticipantId()
    val deviceId = Settings.Secure.getString(applicationContext.getContentResolver(), Settings.Secure.ANDROID_ID);
    val chronicleApi = createRetrofitAdapter(PRODUCTION).create(ChronicleApi::class.java)
    val dataSink = BrokerDataSink(mutableSetOf(OpenLatticeSink(studyId, participantId, deviceId, chronicleApi)))

    override fun onStopJob(params: JobParameters?): Boolean {
        return true
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        executor.execute({
            val queue = chronicleDb.queueEntryData()
            var nextEntries = queue.getNextEntries(17280)
            while (nextEntries.size >= 17280) {
                val data = nextEntries
                        .map { qe-> qe.data }
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