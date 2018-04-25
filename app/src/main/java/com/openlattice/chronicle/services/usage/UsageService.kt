package com.openlattice.chronicle.services.usage

import android.app.IntentService
import android.app.Service
import android.arch.persistence.room.Room
import android.content.Intent
import android.os.Handler
import android.util.Log
import com.openlattice.chronicle.storage.ChronicleDb

val USAGE_SERVICE = "UsageService"

class UsageService() : IntentService(USAGE_SERVICE) {
    val chronicleDb = Room.databaseBuilder(applicationContext, ChronicleDb::class.java!!, "chronicle").build()
    override fun onHandleIntent(intent: Intent?) {
        Log.i(USAGE_SERVICE , "Usage service is running.")

        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}