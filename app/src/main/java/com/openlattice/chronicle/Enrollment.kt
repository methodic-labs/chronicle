package com.openlattice.chronicle

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.ktx.Firebase
import com.methodic.chronicle.constants.FirebaseAnalyticsEvents
import com.openlattice.chronicle.preferences.*
import com.openlattice.chronicle.services.upload.PRODUCTION
import com.openlattice.chronicle.study.StudyApi
import com.openlattice.chronicle.utils.Utils
import com.openlattice.chronicle.utils.Utils.createRetrofitAdapter
import java.util.*
import java.util.concurrent.Executors
import android.Manifest
import com.methodic.chronicle.services.notifications.NotificationPermissionActivity

class Enrollment : AppCompatActivity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val mHandler = object : Handler(Looper.getMainLooper()) {}
    private val crashlytics = FirebaseCrashlytics.getInstance()

    private lateinit var analytics: FirebaseAnalytics
    private lateinit var studyIdText: TextInputEditText
    private lateinit var participantIdText: TextInputEditText
    private lateinit var studyIdTextView: TextView
    private lateinit var participantIdTextView: TextView
    private lateinit var statusMessageText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var submitBtn: MaterialButton
    private lateinit var doneBtn: MaterialButton
    private lateinit var studyIdTextLayout: TextInputLayout
    private lateinit var participantIdTextLayout: TextInputLayout

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.i(Enrollment::class.java.name, "Notifications enabled!")
        } else {
            // Inform researchers that user blocked notifications for their study
            Log.e(Enrollment::class.java.name,"Unable to send notifications!" )
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enrollment)

        analytics = Firebase.analytics
        studyIdText = findViewById(R.id.studyIdText)
        participantIdText = findViewById(R.id.participantIdText)
        statusMessageText = findViewById(R.id.statusMessage)
        progressBar = findViewById(R.id.enrollmentProgress)
        submitBtn = findViewById(R.id.button)
        doneBtn = findViewById(R.id.doneButton)
        participantIdTextView = findViewById(R.id.participantIdTextView)
        studyIdTextView = findViewById(R.id.studyIdTextView)

        studyIdTextLayout = findViewById(R.id.studyIdTextLayout)
        participantIdTextLayout = findViewById(R.id.participantIdTextLayout)

        doneBtn.setOnClickListener {
            handleOnClickDone()
        }

        submitBtn.setOnClickListener {
            doEnrollment()
        }

        // ensure usage usage permission is granted before enrolling
        if (hasUsageSettingPermission(this)) {
            handleIntent(intent)
        } else {
            val permissionIntent = Intent(this, PermissionActivity::class.java).apply {
                action = intent.action
                data = intent.data
            }

            startActivity(permissionIntent)
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

        if (Intent.ACTION_VIEW == appLinkAction && appLinkData != null) {
            val studyId = appLinkData.getQueryParameter("studyId")
            val participantId = appLinkData.getQueryParameter("participantId")

            studyIdText.setText(studyId)
            participantIdText.setText(participantId)
        }
    }

    private fun handleOnClickDone() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun validateInput(studyId: String, participantId: String): Boolean {

        if (studyId.isBlank()) {
            studyIdText.error = getString(R.string.invalid_study_id_blank)

        } else if (!Utils.isValidUUID(studyId)) {
            studyIdText.error = getString(R.string.invalid_study_id_format)
        }

        if (participantId.isBlank()) {
            participantIdText.error = getString(R.string.invalid_participant)
        }

        return studyIdText.error.isNullOrBlank() && participantIdText.error.isNullOrBlank()
    }

    private fun closeKeyBoard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
    private fun askNotificationPermission() {
        startActivity(Intent(this, NotificationPermissionActivity::class.java))
        finish()
        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // TODO: display an educational UI explaining to the user the features that will be enabled
                //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
                //       If the user selects "No thanks," allow the user to continue without notifications.
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun doEnrollment() {
        // Declare the launcher at the top of your Activity/Fragment:

        val studyIdStr: String = studyIdText.text.toString().trim()
        val participantId: String = participantIdText.text.toString().trim()

        val isValidInput = validateInput(studyIdStr, participantId)
        if (!isValidInput) {
            return
        }

        try {
            val studyId = UUID.fromString(studyIdStr)
            val deviceId = getDeviceId(applicationContext)

            statusMessageText.visibility = View.INVISIBLE
            submitBtn.visibility = View.INVISIBLE
            progressBar.visibility = View.VISIBLE
            closeKeyBoard()

            executor.execute {
                val studyApi = createRetrofitAdapter(PRODUCTION).create(StudyApi::class.java)

                var chronicleId: UUID? = null
                try {
                    chronicleId = studyApi.enroll(
                        studyId,
                        participantId,
                        deviceId,
                        getDevice(deviceId)
                    )

                } catch (e: Exception) {
                    crashlytics.log("caught exception - studyId: \"$studyId\" ; participantId: \"$participantId\"")
                    FirebaseCrashlytics.getInstance().recordException(e)
                }

                // TODO: actually retrieve device id
                if (chronicleId != null) {
                    Log.i(javaClass.canonicalName, "Chronicle id: " + chronicleId.toString())
                    analytics.logEvent(FirebaseAnalyticsEvents.ENROLLMENT_SUCCESS, Bundle().apply {
                        putString(PARTICIPANT_ID, participantId)
                        putString(STUDY_ID, studyId.toString())
                    })
                    mHandler.post {
                        val enrollmentSettings = EnrollmentSettings(applicationContext)

                        enrollmentSettings.setStudyId(studyId)
                        enrollmentSettings.setParticipantId(participantId)

                        // hide text fields, progress bar, and enroll button
                        studyIdTextLayout.visibility = View.GONE
                        participantIdTextLayout.visibility = View.GONE
                        progressBar.visibility = View.GONE
                        submitBtn.visibility = View.GONE
                        studyIdTextView.visibility = View.GONE
                        participantIdTextView.visibility = View.GONE

                        // show success message and done button
                        statusMessageText.text = getString(R.string.device_enroll_success)
                        statusMessageText.visibility = View.VISIBLE
                        doneBtn.visibility = View.VISIBLE
                    }
                    askNotificationPermission()
                } else {
                    crashlytics.log("unable to enroll device - studyId: \"$studyId\" ; participantId: \"$participantId\"")
                    analytics.logEvent(FirebaseAnalyticsEvents.ENROLLMENT_FAILURE, Bundle().apply {
                        putString(PARTICIPANT_ID, participantId)
                        putString(STUDY_ID, studyId.toString())
                    })
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
            crashlytics.recordException(e)
            Log.e(javaClass.canonicalName, "unable to enroll", e)
        }
    }
}
