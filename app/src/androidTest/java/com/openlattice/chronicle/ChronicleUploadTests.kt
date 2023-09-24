package com.openlattice.chronicle

import androidx.room.Room
import android.os.Build
import android.provider.Settings
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.util.Log
import androidx.test.InstrumentationRegistry.getContext
import androidx.test.InstrumentationRegistry.getTargetContext
import com.fasterxml.jackson.datatype.guava.GuavaModule
import com.google.common.base.Optional
import com.google.common.base.Stopwatch
import com.openlattice.chronicle.sources.AndroidDevice
import com.openlattice.chronicle.storage.ChronicleDb
import com.openlattice.chronicle.storage.StorageQueue
import com.openlattice.chronicle.util.RetrofitBuilders.createBaseChronicleRetrofitBuilder
import com.openlattice.chronicle.util.RetrofitBuilders.decorateWithRhizomeFactories
import com.openlattice.chronicle.util.RetrofitBuilders.mapper
import com.openlattice.chronicle.util.RetrofitBuilders.okHttpClient
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*
import java.util.concurrent.TimeUnit


@RunWith(AndroidJUnit4::class)
class ChronicleUploadTests {
    companion object ChronicleDbHolder {
        lateinit var chronicleDb: ChronicleDb
        lateinit var storageQueue: StorageQueue
        lateinit var chronicleApi: ChronicleApi
        lateinit var chronicleStudyApi: ChronicleStudyApi

        @BeforeClass
        @JvmStatic
        fun setupChronicleDb() {
            val appContext = InstrumentationRegistry.getInstrumentation().targetContext
            chronicleDb = Room.databaseBuilder(appContext, ChronicleDb::class.java!!, "chronicle").build()
            storageQueue = chronicleDb.queueEntryData()
            chronicleDb.queueEntryData().deleteEntries(chronicleDb.queueEntryData().getNextEntries(1000))
            val httpClient = okHttpClient().build()
            val retrofit = decorateWithRhizomeFactories(createBaseChronicleRetrofitBuilder("https://api.openlattice.com/", httpClient)).build()
            chronicleApi = retrofit.create(ChronicleApi::class.java)
            chronicleStudyApi = retrofit.create(ChronicleStudyApi::class.java)
        }

    }

    @Test
    fun testchronicleReadWriteSingleQueueEntry() {
        val deviceId = Settings.Secure.getString(getContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        val device = AndroidDevice(deviceId, Build.MODEL, Build.VERSION.CODENAME, Build.BRAND, Build.DISPLAY, Build.VERSION.SDK_INT.toString(), Build.PRODUCT, deviceId, mapOf())
        val studyId = UUID.fromString("28d661b8-a45a-41b6-aec4-ed9988fa28dc")
        val participantId = "participant1"

        val w = Stopwatch.createStarted();

        mapper.registerModule(GuavaModule())

        val deviceJson = mapper.writeValueAsString(Optional.of(device))
        Log.i(javaClass.canonicalName, mapper.writeValueAsString(Optional.of(device)))
        w.stop()
        var millis = w.elapsed(TimeUnit.MILLISECONDS)
        Log.i(javaClass.canonicalName, "Elapsed serialization time: $millis")
        w.reset()
        w.start()
        Log.i(javaClass.canonicalName, TypeRefs.map(mapper, deviceJson, TypeRefs.optDS()).get().toString())
        millis = w.elapsed(TimeUnit.MILLISECONDS)
        Log.i(javaClass.canonicalName, "Elapsed serialization time: $millis")

        chronicleStudyApi.enrollSource(studyId, participantId, deviceId, Optional.of(device))
        chronicleApi.upload(studyId, participantId, deviceId, mutableListOf())
    }


}