package com.openlattice.chronicle

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import com.google.common.base.Optional
import com.openlattice.chronicle.preferences.EnrollmentSettings
import com.openlattice.chronicle.preferences.getDevice
import com.openlattice.chronicle.preferences.getDeviceId
import com.openlattice.chronicle.receivers.lifecycle.scheduleUploadJob
import com.openlattice.chronicle.services.upload.PRODUCTION
import com.openlattice.chronicle.services.upload.createRetrofitAdapter
import com.openlattice.chronicle.services.usage.scheduleUsageJob
import java.lang.IllegalArgumentException
import java.util.*
import java.util.concurrent.Executors

class Enrollment : AppCompatActivity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val mHandler = object : Handler(Looper.getMainLooper()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enrollment)
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val appLinkIntent = intent
        val appLinkAction = appLinkIntent.action
        val appLinkData = appLinkIntent.data

        if (Intent.ACTION_VIEW == appLinkAction && appLinkData != null) {
            val studyIdText = findViewById<EditText>(R.id.studyIdText)
            val participantIdText = findViewById<EditText>(R.id.participantIdText)
            val studyId = appLinkData.getQueryParameter("studyId")
            val participantId = appLinkData.getQueryParameter("participantId")
            studyIdText.setText(studyId)
            participantIdText.setText(participantId)
//            doEnrollment()
        }
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

        if (studyIdText.text.isBlank()) {
            errorMessageText.text = getString(R.string.invalid_study_id_blank)
            errorMessageText.visibility = View.VISIBLE
        }

        if (participantIdText.text.isBlank()) {
            errorMessageText.text = getString(R.string.invalid_participant)
            errorMessageText.visibility = View.VISIBLE
        }


        if (studyIdText.text.isNotBlank() && participantIdText.text.isNotBlank()) {
            try {
                val id = UUID.fromString(studyIdText.text.toString())
                val participantId = participantIdText.text.toString()
                val deviceId = getDeviceId(applicationContext)

                submitBtn.visibility = View.INVISIBLE
                progressBar.visibility = View.VISIBLE

                executor.execute {
                    val chronicleStudyApi = createRetrofitAdapter(PRODUCTION).create(ChronicleStudyApi::class.java)


                    val chronicleId = if (chronicleStudyApi.isKnownDatasource(id, participantId, deviceId)) {
                        UUID.randomUUID()
                    } else {
                        chronicleStudyApi.enrollSource(
                                id,
                                participantId,
                                deviceId,
                                Optional.of(getDevice(deviceId)))
                    }

                    if (chronicleId != null) {
                        Log.i(javaClass.canonicalName, "Chronicle id: " + chronicleId.toString())
                        mHandler.post {
                            val enrollmentSettings = EnrollmentSettings(applicationContext)
                            enrollmentSettings.setStudyId(id)
                            enrollmentSettings.setParticipantId(participantId)

                            progressBar.visibility = View.INVISIBLE
                            submitBtn.visibility = View.VISIBLE
                            //studyIdText.visibility = View.INVISIBLE
                            //participantIdText.visibility = View.INVISIBLE
                            errorMessageText.visibility = View.VISIBLE
                            errorMessageText.setText(getString(R.string.device_enroll_success))
                            doMainActivity(this)
                            scheduleUploadJob(this)
                            scheduleUsageJob(this)
                        }
                    } else {
                        Log.e(javaClass.canonicalName, "Unable to enroll device.")
                        mHandler.post {
                            progressBar.visibility = View.INVISIBLE
                            submitBtn.visibility = View.VISIBLE
                            errorMessageText.visibility = View.VISIBLE
                            errorMessageText.setText(getString(R.string.device_enroll_failure))
                        }
                    }
                }


            } catch (e: IllegalArgumentException) {
                errorMessageText.text = getString(R.string.invalid_study_id_format)
                errorMessageText.visibility = View.VISIBLE
                Log.e(javaClass.canonicalName, "Unable to parse UUID.");
            }
        }
    }
}
