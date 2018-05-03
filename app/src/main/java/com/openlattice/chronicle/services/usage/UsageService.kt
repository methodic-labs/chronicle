package com.openlattice.chronicle.services.usage

import android.app.IntentService
import android.arch.persistence.room.Room
import android.content.Intent
import android.os.Handler
import android.util.Log
import com.openlattice.chronicle.ChronicleApi
import com.openlattice.chronicle.preferences.EnrollmentSettings
import com.openlattice.chronicle.sensors.ActivityManagerChronicleSensor
import com.openlattice.chronicle.sensors.ChronicleSensor
import com.openlattice.chronicle.sensors.FQNS
import com.openlattice.chronicle.services.upload.PRODUCTION
import com.openlattice.chronicle.services.upload.createRetrofitAdapter
import com.openlattice.chronicle.storage.ChronicleDb
import com.openlattice.chronicle.storage.QueueEntry
import com.openlattice.chronicle.util.RetrofitBuilders

val USAGE_SERVICE = "UsageService"

class UsageService() : IntentService(USAGE_SERVICE) {
    private val chronicleDb = Room.databaseBuilder(applicationContext, ChronicleDb::class.java!!, "chronicle").build()
    private val storageQueue = chronicleDb.queueEntryData()
    private val settings = EnrollmentSettings(applicationContext)
    private val mapper = RetrofitBuilders.mapper
    val studyId = settings.getStudyId()
    val participantId = settings.getParticipantId()
    val chronicleApi = createRetrofitAdapter(PRODUCTION).create(ChronicleApi::class.java)
    val propertyTypeIds = chronicleApi.getPropertyTypeIds(FQNS)
    val sensors = mutableSetOf<ChronicleSensor>(ActivityManagerChronicleSensor(applicationContext))

    val handler = Handler();
    var id = 0L

    override fun onHandleIntent(intent: Intent?) {
        Log.i(USAGE_SERVICE, "Usage service is running.")
        handler.post(this::doWork)
    }

    fun doWork() {
        Log.i(USAGE_SERVICE, "Collecting Usage Information.")
        sensors
                .map { it.poll(propertyTypeIds) }
                .forEach { storageQueue.insertEntry(QueueEntry(System.currentTimeMillis(), id++, RetrofitBuilders.mapper.writeValueAsBytes(it))) }
        handler.postDelayed(this::doWork, 5 * 1000)
    }
}

