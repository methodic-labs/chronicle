package com.openlattice.chronicle

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.openlattice.chronicle.data.ParticipationStatus
import com.openlattice.chronicle.preferences.EnrollmentSettings
import com.openlattice.chronicle.preferences.INVALID_ORG_ID
import com.openlattice.chronicle.services.notifications.createNotificationChannel
import com.openlattice.chronicle.services.status.scheduleEnrollmentStatusJob
import com.openlattice.chronicle.services.upload.getLastUpload
import com.openlattice.chronicle.services.upload.scheduleUploadJob
import com.openlattice.chronicle.services.usage.scheduleUsageMonitoringJob
import java.util.*

const val LAST_UPLOAD_REFRESH_INTERVAL = 5000L

class MainActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())

    private lateinit var enrollmentSettings: EnrollmentSettings
    private lateinit var orgId: UUID
    private lateinit var studyId :UUID
    private lateinit var participantId :String
    private lateinit var participationStatus: ParticipationStatus

    override fun onCreate(savedInstanceState: Bundle?) {

        if (BuildConfig.DEBUG) {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // create notification channel
        createNotificationChannel(this)

        if (hasUsageSettingPermission(this)) {
            enrollmentSettings = EnrollmentSettings(this)
            orgId = enrollmentSettings.getOrganizationId()
            studyId = enrollmentSettings.getStudyId()
            participantId = enrollmentSettings.getParticipantId()
            participationStatus = enrollmentSettings.getParticipationStatus()

            if (enrollmentSettings.isEnrolled()) {
                val studyIdText = findViewById<TextView>(R.id.studyId)
                val participantIdText = findViewById<TextView>(R.id.participantId)
                val orgIdTextView = findViewById<TextView>(R.id.orgId)
                val orgIdLabel :TextView = findViewById(R.id.orgIdLabel)

                studyIdText.text = studyId.toString()
                participantIdText.text = participantId

                if (orgId != INVALID_ORG_ID) {
                    orgIdTextView.text = orgId.toString()
                } else {
                    orgIdTextView.visibility = View.GONE
                    orgIdLabel.visibility = View.GONE
                }

                if (participationStatus == ParticipationStatus.ENROLLED) {
                    scheduleUploadJob(this)
                    scheduleUsageMonitoringJob(this)
                }
                scheduleEnrollmentStatusJob(this)
                handler.post(this::updateLastUpload)
            } else {

                val enrollmentIntent = Intent(this, Enrollment::class.java).apply {
                    data = intent.data
                    action = intent.action
                }

                startActivity(enrollmentIntent)
            }
        } else {
            startActivity(Intent(this, PermissionActivity::class.java))
        }

    }

    private fun updateLastUpload() {
        val lastUploadText = findViewById<TextView>(R.id.lastUploadValue)
        lastUploadText.text = getLastUpload(this)
        handler.postDelayed(this::updateLastUpload, LAST_UPLOAD_REFRESH_INTERVAL)
    }

    override fun onBackPressed() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }
}
