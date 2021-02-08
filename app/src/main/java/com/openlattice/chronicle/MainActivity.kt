package com.openlattice.chronicle

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.ktx.Firebase
import com.openlattice.chronicle.data.ParticipationStatus
import com.openlattice.chronicle.preferences.EnrollmentSettings
import com.openlattice.chronicle.preferences.INVALID_ORG_ID
import com.openlattice.chronicle.services.notifications.createNotificationChannel
import com.openlattice.chronicle.services.notifications.scheduleNotificationsWorker
import com.openlattice.chronicle.services.upload.scheduleUploadWork
import com.openlattice.chronicle.services.usage.scheduleUsageMonitoringWork
import com.openlattice.chronicle.utils.Utils.getLastUpload
import kotlinx.coroutines.*
import java.util.*

const val LAST_UPLOAD_REFRESH_INTERVAL = 5000L

class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    private lateinit var enrollmentSettings: EnrollmentSettings
    private lateinit var orgId: UUID
    private lateinit var studyId: UUID
    private lateinit var participantId: String
    private lateinit var participationStatus: ParticipationStatus
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private lateinit var lastUploadText: TextView
    private lateinit var studyIdText: TextView
    private lateinit var participantIdText: TextView
    private lateinit var orgIdTextView: TextView
    private lateinit var orgIdLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lastUploadText = findViewById(R.id.lastUploadValue)
        firebaseAnalytics = Firebase.analytics
        enrollmentSettings = EnrollmentSettings(this)
        orgId = enrollmentSettings.getOrganizationId()
        studyId = enrollmentSettings.getStudyId()
        participantId = enrollmentSettings.getParticipantId()
        participationStatus = enrollmentSettings.getParticipationStatus()

        studyIdText = findViewById(R.id.studyId)
        participantIdText = findViewById(R.id.participantId)
        orgIdTextView = findViewById(R.id.orgId)
        orgIdLabel = findViewById(R.id.orgIdLabel)

        // disable crashlytics in debug
        if (BuildConfig.DEBUG) {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false)
        }
        // create notification channel
        createNotificationChannel(this)

        if (!hasUsageSettingPermission(this)) {
            startActivity(Intent(this, PermissionActivity::class.java))
            finish()
        }

        if (enrollmentSettings.isEnrolled()) {

            firebaseAnalytics.setUserId("${orgId}_${studyId}_${participantId}")

            studyIdText.text = studyId.toString()
            participantIdText.text = participantId

            if (orgId != INVALID_ORG_ID) {
                orgIdTextView.text = orgId.toString()
            } else {
                orgIdTextView.visibility = View.GONE
                orgIdLabel.visibility = View.GONE
            }

            launch(Dispatchers.Default) {
                updateLastUpload()
            }

            // start workers
            scheduleUploadWork(this)
            scheduleUsageMonitoringWork(this)
            scheduleNotificationsWorker(this)

        } else {

            val enrollmentIntent = Intent(this, Enrollment::class.java).apply {
                data = intent.data
                action = intent.action
            }

            startActivity(enrollmentIntent)
            return
        }

        requestBatteryOptimizationExemption()
    }

    private suspend fun updateLastUpload() {
        withContext(Dispatchers.IO) {
            while (true) {

                val lastUpload = getLastUpload(applicationContext)

                launch(Dispatchers.Main) {
                    lastUploadText.text = lastUpload
                }

                delay(LAST_UPLOAD_REFRESH_INTERVAL)
            }

        }
    }

    // check that devices with android 6.0 (api 23) are exempt from Doze and App Standby optimizations
    // https://developer.android.com/training/monitoring-device-state/doze-standby
    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !hasIgnoreBatteryOptimization(this)) {
            startActivity(Intent(this, BatteryOptimizationExemption::class.java))
        }
    }

    override fun onBackPressed() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }


    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }
}
