package com.openlattice.chronicle

import android.content.Context
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.common.base.Optional
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.openlattice.chronicle.api.ChronicleApi
import com.openlattice.chronicle.preferences.EnrollmentSettings
import com.openlattice.chronicle.preferences.getDevice
import com.openlattice.chronicle.preferences.getDeviceId
import com.openlattice.chronicle.services.upload.PRODUCTION
import com.openlattice.chronicle.services.upload.createRetrofitAdapter
import com.openlattice.chronicle.utils.Utils
import kotlinx.android.synthetic.main.activity_enrollment.*
import java.util.*
import java.util.concurrent.Executors

class Enrollment : AppCompatActivity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val mHandler = object : Handler(Looper.getMainLooper()) {}
    private val crashlytics = FirebaseCrashlytics.getInstance()

    private lateinit var orgIdText: TextInputEditText
    private lateinit var studyIdText: TextInputEditText
    private lateinit var participantIdText: TextInputEditText
    private lateinit var statusMessageText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var submitBtn: MaterialButton
    private lateinit var doneBtn: MaterialButton
    private lateinit var studyIdTextLayout: TextInputLayout
    private lateinit var orgIdTextLayout: TextInputLayout
    private lateinit var participantIdTextLayout: TextInputLayout


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enrollment)

        orgIdText = findViewById(R.id.orgIdText)
        studyIdText = findViewById(R.id.studyIdText)
        participantIdText = findViewById(R.id.participantIdText)
        statusMessageText = findViewById(R.id.statusMessage)
        progressBar = findViewById(R.id.enrollmentProgress)
        submitBtn = findViewById(R.id.button)
        doneBtn = findViewById(R.id.doneButton)

        studyIdTextLayout = findViewById(R.id.studyIdTextLayout)
        orgIdTextLayout = findViewById(R.id.orgIdTextLayout)
        participantIdTextLayout = findViewById(R.id.participantIdTextLayout)

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
            val studyId = appLinkData.getQueryParameter("studyId")
            val participantId = appLinkData.getQueryParameter("participantId")
            studyIdText.setText(studyId)
            participantIdText.setText(participantId)
        }
    }

    fun enrollDevice(view: View) {
        doEnrollment()
    }

    fun handleOnClickDone(view: View) {
        doMainActivity(this)
        finish()
    }

    private fun validateInput(orgId: String, studyId: String, participantId: String): Boolean {
        var error = false

        if (orgId.isBlank()) {
            orgIdText.error = getString(R.string.invalid_org_id_blank)
            error = true
        } else if (!Utils.isValidUUID(orgId)) {
            orgIdText.error = getString(R.string.invalid_org_id_format)
            error = true
        }

        if (studyId.isBlank()) {
            studyIdText.error = getString(R.string.invalid_study_id_blank)
            error = true
        } else if (!Utils.isValidUUID(studyId)) {
            studyIdText.error = getString(R.string.invalid_study_id_format)
            error = true
        }

        if (participantId.isBlank()) {
            participantIdText.error = getString(R.string.invalid_participant)
            error = true
        }

        return error
    }

    private fun closeKeyBoard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun doEnrollment() {

        val orgIdStr: String = orgIdText.text.toString().trim()
        val studyIdStr: String = studyIdText.text.toString().trim()
        val participantId: String = participantIdText.text.toString().trim()

        val error = validateInput(orgIdStr, studyIdStr, participantId)
        if (error) {
            return
        }

        try {
            val orgId = UUID.fromString(orgIdStr)
            val studyId = UUID.fromString(studyIdStr)
            val deviceId = getDeviceId(applicationContext)

            statusMessageText.visibility = View.INVISIBLE
            submitBtn.visibility = View.INVISIBLE
            progressBar.visibility = View.VISIBLE
            closeKeyBoard()

            executor.execute {
                val chronicleApi = createRetrofitAdapter(PRODUCTION).create(ChronicleApi::class.java)

                var chronicleId: UUID? = null
                try {
                    chronicleId = chronicleApi.enroll(orgId, studyId, participantId, deviceId, Optional.of(getDevice(deviceId)))
                } catch (e: Exception) {
                    crashlytics.log("caught exception - orgId: \"$orgId\" ; studyId: \"$studyId\" ; participantId: \"$participantId\"")
                    FirebaseCrashlytics.getInstance().recordException(e)
                }

                // TODO: actually retrieve device id
                if (chronicleId != null) {
                    Log.i(javaClass.canonicalName, "Chronicle id: " + chronicleId.toString())
                    mHandler.post {
                        val enrollmentSettings = EnrollmentSettings(applicationContext)

                        enrollmentSettings.setStudyId(studyId)
                        enrollmentSettings.setParticipantId(participantId)
                        enrollmentSettings.setOrganizationId(orgId)

                        // hide text fields, progress bar, and enroll button
                        studyIdTextLayout.visibility = View.GONE
                        participantIdTextLayout.visibility = View.GONE
                        orgIdTextLayout.visibility = View.GONE
                        progressBar.visibility = View.GONE
                        submitBtn.visibility = View.GONE
                        // show success message and done button
                        statusMessageText.text = getString(R.string.device_enroll_success)
                        statusMessageText.visibility = View.VISIBLE
                        doneBtn.visibility = View.VISIBLE
                    }
                } else {
                    crashlytics.log("unable to enroll device - studyId: \"$studyId\" ; participantId: \"$participantId\"")
                    Log.e(javaClass.canonicalName, "unable to enroll device.")
                    mHandler.post {
                        progressBar.visibility = View.INVISIBLE
                        submitBtn.visibility = View.VISIBLE
                        doneBtn.visibility = View.INVISIBLE
                        statusMessageText.visibility = View.VISIBLE
                        statusMessageText.text = getString(R.string.device_enroll_failure)
                    }
                }
            }
        } catch (e: Exception) {
            statusMessageText.text = getString(R.string.device_enroll_failure)
            statusMessageText.visibility = View.VISIBLE
            submitBtn.visibility = View.VISIBLE
            doneBtn.visibility = View.INVISIBLE
            FirebaseCrashlytics.getInstance().recordException(e)
            Log.e(javaClass.canonicalName, "unable to enroll", e)
        }
    }
}
