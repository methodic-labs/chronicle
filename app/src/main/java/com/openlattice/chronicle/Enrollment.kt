package com.openlattice.chronicle

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import com.crashlytics.android.Crashlytics
import com.google.common.base.Optional
import com.openlattice.chronicle.preferences.EnrollmentSettings
import com.openlattice.chronicle.preferences.getDevice
import com.openlattice.chronicle.preferences.getDeviceId
import com.openlattice.chronicle.services.upload.PRODUCTION
import com.openlattice.chronicle.services.upload.createRetrofitAdapter
import com.openlattice.chronicle.utils.Utils
import io.fabric.sdk.android.Fabric
import java.lang.IllegalArgumentException
import java.util.*
import java.util.concurrent.Executors

class Enrollment : AppCompatActivity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val mHandler = object : Handler(Looper.getMainLooper()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Fabric.with(this, Crashlytics())
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
        }
    }

    fun enrollDevice(view: View) {
        doEnrollment()
    }

    fun handleOnClickDone(view :View) {
        doMainActivity(this)
        finish()
    }

    private fun doEnrollment() {

        val studyIdText = findViewById<EditText>(R.id.studyIdText)
        val participantIdText = findViewById<EditText>(R.id.participantIdText)
        val statusMessageText = findViewById<TextView>(R.id.statusMessage)
        val progressBar = findViewById<ProgressBar>(R.id.enrollmentProgress)
        val submitBtn = findViewById<Button>(R.id.button)
        val doneBtn = findViewById<Button>(R.id.doneButton)

        if (studyIdText.text.isBlank()) {
            statusMessageText.text = getString(R.string.invalid_study_id_blank)
            statusMessageText.visibility = View.VISIBLE
        }

        if (!Utils.isValidUUID(studyIdText.text.toString())) {
            statusMessageText.text = getString(R.string.invalid_study_id_format)
            statusMessageText.visibility = View.VISIBLE
        }

        if (participantIdText.text.isBlank()) {
            statusMessageText.text = getString(R.string.invalid_participant)
            statusMessageText.visibility = View.VISIBLE
        }

        if (Utils.isValidUUID(studyIdText.text.toString()) && participantIdText.text.isNotBlank()) {
            try {
                val studyId = UUID.fromString(studyIdText.text.toString())
                val participantId = participantIdText.text.toString()
                val deviceId = getDeviceId(applicationContext)

                submitBtn.visibility = View.INVISIBLE
                progressBar.visibility = View.VISIBLE

                executor.execute {
                    val chronicleStudyApi = createRetrofitAdapter(PRODUCTION).create(ChronicleStudyApi::class.java)

                    //TODO: Actually retrieve id of device.
                    val chronicleId = if (chronicleStudyApi.isKnownDatasource(studyId, participantId, deviceId)) {
                        UUID.randomUUID()
                    } else {
                        chronicleStudyApi.enrollSource(
                                studyId,
                                participantId,
                                deviceId,
                                Optional.of(getDevice(deviceId)))
                    }

                    if (chronicleId != null) {
                        Log.i(javaClass.canonicalName, "Chronicle id: " + chronicleId.toString())
                        mHandler.post {
                            val enrollmentSettings = EnrollmentSettings(applicationContext)
                            enrollmentSettings.setStudyId(studyId)
                            enrollmentSettings.setParticipantId(participantId)
                            // hide text fields, progress bar, and enroll button
                            studyIdText.visibility = View.GONE
                            participantIdText.visibility = View.GONE
                            progressBar.visibility = View.GONE
                            submitBtn.visibility = View.GONE
                            // show success message and done button
                            statusMessageText.text = getString(R.string.device_enroll_success)
                            statusMessageText.visibility = View.VISIBLE
                            doneBtn.visibility = View.VISIBLE
                        }
                    } else {
                        Crashlytics.log("unable to enroll device, chronicleId is null")
                        Log.e(javaClass.canonicalName, "Unable to enroll device.")
                        mHandler.post {
                            progressBar.visibility = View.INVISIBLE
                            submitBtn.visibility = View.VISIBLE
                            doneBtn.visibility = View.INVISIBLE
                            statusMessageText.visibility = View.VISIBLE
                            statusMessageText.text = getString(R.string.device_enroll_failure)
                        }
                    }
                }


            } catch (e: IllegalArgumentException) {
                statusMessageText.text = getString(R.string.invalid_study_id_format)
                statusMessageText.visibility = View.VISIBLE
                Crashlytics.logException(e)
                Log.e(javaClass.canonicalName, "Unable to parse UUID.")
            }
        }
    }
}

