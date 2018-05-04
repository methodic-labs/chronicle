package com.openlattice.chronicle

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import com.google.common.base.Optional
import com.openlattice.chronicle.preferences.EnrollmentSettings
import com.openlattice.chronicle.preferences.getDevice
import com.openlattice.chronicle.preferences.getDeviceId
import com.openlattice.chronicle.receivers.lifecycle.REQUEST_CODE
import com.openlattice.chronicle.receivers.lifecycle.UsageCollectionAlarmReceiver
import com.openlattice.chronicle.receivers.lifecycle.scheduleUploadJob
import com.openlattice.chronicle.services.upload.PRODUCTION
import com.openlattice.chronicle.services.upload.createRetrofitAdapter
import java.lang.IllegalArgumentException
import java.util.*
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    val executor = Executors.newSingleThreadExecutor()
    val mHandler = object : Handler(Looper.getMainLooper()) {}


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main)
        scheduleAlarm()
        scheduleUploadJob(this)
        handleIntent(intent)
        val enrollment = EnrollmentSettings(this)
        if (enrollment.enrolled) {
            val errorMessageText = findViewById<TextView>(R.id.errorMessage)
            val studyIdText = findViewById<EditText>(R.id.studyIdText)
            val participantIdText = findViewById<EditText>(R.id.participantIdText)
            val submitBtn = findViewById<Button>(R.id.button)


            studyIdText.visibility = INVISIBLE
            participantIdText.visibility = INVISIBLE
            submitBtn.visibility = INVISIBLE

            errorMessageText.setText(getString(R.string.already_enrolled) + "\nStudy = ${enrollment.getStudyId().toString()}\nParticipant = ${enrollment.getParticipantId()}\nDevice = ${getDeviceId(this)}")
            errorMessageText.visibility = VISIBLE
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val appLinkIntent = intent
        val appLinkAction = appLinkIntent.action
        val appLinkData = appLinkIntent.data

        if (Intent.ACTION_VIEW.equals(appLinkAction) && appLinkData != null) {
            val studyIdText = findViewById<EditText>(R.id.studyIdText)
            val participantIdText = findViewById<EditText>(R.id.participantIdText)
            val studyId = appLinkData.getQueryParameter("studyId")
            val participantId = appLinkData.getQueryParameter("participantId")
            doEnrollment()
        }
    }

    fun scheduleAlarm() {
        val intent = Intent(applicationContext, UsageCollectionAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val firstMillis = System.currentTimeMillis()
        val alarm = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.setRepeating(AlarmManager.ELAPSED_REALTIME, firstMillis, 60 * 1000, pendingIntent)
    }

    fun enrollDevice(view: View) {
        doEnrollment()
    }

    fun doEnrollment() {
        val studyIdText = findViewById<EditText>(R.id.studyIdText)
        val participantIdText = findViewById<EditText>(R.id.participantIdText)
        val errorMessageText = findViewById<TextView>(R.id.errorMessage)
        val progressBar = findViewById<ProgressBar>(R.id.enrollmentProgress)
        val submitBtn = findViewById<Button>(R.id.button)

        if (participantIdText.text.isBlank()) {
            errorMessageText.setText("Invalid study or participant id.")
        }

        if (studyIdText.text.isNotBlank() && participantIdText.text.isNotBlank()) {
            try {
                val id = UUID.fromString(studyIdText.text.toString())
                val participantId = participantIdText.text.toString()
                val deviceId = getDeviceId(applicationContext)

                submitBtn.visibility = INVISIBLE
                progressBar.visibility = VISIBLE

                executor.execute {
                    val chronicleStudyApi = createRetrofitAdapter(PRODUCTION).create(ChronicleStudyApi::class.java)
                    val chronicleId = chronicleStudyApi.enrollSource(
                            id,
                            participantId,
                            deviceId,
                            Optional.of(getDevice(deviceId)))
                    if (chronicleId != null) {
                        Log.i(javaClass.canonicalName, "Chronicle id: " + chronicleId.toString())
                        mHandler.post {
                            val enrollmentSettings = EnrollmentSettings(applicationContext)
                            enrollmentSettings.setStudyId(id)
                            enrollmentSettings.setParticipantId(participantId)

                            progressBar.visibility = INVISIBLE
                            studyIdText.visibility = INVISIBLE
                            participantIdText.visibility = INVISIBLE
                            errorMessageText.visibility = VISIBLE
                            errorMessageText.setText("SUCCESSFULLY ENROLLED")
                        }
                    } else {
                        Log.e(javaClass.canonicalName, "Unable to enroll device.")
                        mHandler.post {
                            progressBar.visibility = INVISIBLE
                            submitBtn.visibility = VISIBLE
                            errorMessageText.visibility = VISIBLE
                            errorMessageText.setText("Unable to enroll device!")
                        }
                    }

                }


            } catch (e: IllegalArgumentException) {
                errorMessageText.setText("Invalid study or participant id.")
                Log.e(javaClass.canonicalName, "Unable to parse UUID.");
            }
        }
    }
}