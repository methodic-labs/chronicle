package com.openlattice.chronicle

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.openlattice.chronicle.receivers.lifecycle.REQUEST_CODE
import com.openlattice.chronicle.receivers.lifecycle.UsageCollectionAlarmReceiver

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main)
    }

    fun scheduleAlarm() {
        val intent = Intent(applicationContext, UsageCollectionAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val firstMillis = System.currentTimeMillis()
        val alarm = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarm.setRepeating(AlarmManager.ELAPSED_REALTIME, firstMillis, 60*1000 )
    }


    fun scheduleUploads() {

    }
}