package com.openlattice.chronicle

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.TextView
import com.openlattice.chronicle.preferences.EnrollmentSettings
import com.openlattice.chronicle.receivers.lifecycle.scheduleUploadJob
import com.openlattice.chronicle.services.usage.scheduleUsageJob


class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main)
        if (hasUsageSettingPermission(this)) {
            val enrollment = EnrollmentSettings(this)

            if (enrollment.enrolled) {
                val studyIdText = findViewById<TextView>(R.id.mainStudyId)
                val participantIdText = findViewById<TextView>(R.id.mainParticipantId)
                studyIdText.text = "Study Id: ${enrollment.getStudyId()}"
                participantIdText.text = "Participant Id: ${enrollment.getParticipantId()}"
            } else {
                startActivity(Intent(this, Enrollment::class.java))
            }
        } else {
            startActivity(Intent(this, PermissionActivity::class.java))
            return
        }


//        if (enrollment.enrolled) {
//            val errorMessageText = findViewById<TextView>(R.id.errorMessage)
//            val studyIdText = findViewById<EditText>(R.id.studyIdText)
//            val participantIdText = findViewById<EditText>(R.id.participantIdText)
//            val submitBtn = findViewById<Button>(R.id.button)
//
//
//            studyIdText.visibility = INVISIBLE
//            participantIdText.visibility = INVISIBLE
//            submitBtn.visibility = INVISIBLE
//
//            errorMessageText.text =
//                    getString(R.string.already_enrolled) +
//                    "\nStudy = ${enrollment.getStudyId().toString()}" +
//                    "\nParticipant = ${enrollment.getParticipantId()}" +
//                    "\nDevice = ${getDeviceId(this)}"
//            errorMessageText.visibility = VISIBLE
//        } else {
//
//        }
    }


//    fun scheduleAlarm() {
//        val intent = Intent(applicationContext, UsageCollectionAlarmReceiver::class.java)
//        val pendingIntent = PendingIntent.getBroadcast(this, REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT)
//        val firstMillis = System.currentTimeMillis() + 10000
//        val alarm = getSystemService(Context.ALARM_SERVICE) as AlarmManager
//        alarm.setRepeating(AlarmManager.ELAPSED_REALTIME, firstMillis, 60 * 1000, pendingIntent)
//    }


}