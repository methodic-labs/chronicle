package com.openlattice.chronicle

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.openlattice.chronicle.data.ParticipationStatus
import com.openlattice.chronicle.preferences.EnrollmentSettings
import com.openlattice.chronicle.services.notifications.createNotificationChannel
import com.openlattice.chronicle.services.status.scheduleEnrollmentStatusJob
import com.openlattice.chronicle.services.upload.getLastUpload
import com.openlattice.chronicle.services.upload.scheduleUploadJob
import com.openlattice.chronicle.services.usage.scheduleUsageMonitoringJob

const val LAST_UPLOAD_REFRESH_INTERVAL = 5000L

class MainActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {

        if (BuildConfig.DEBUG) {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(false)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // create notification channel
        createNotificationChannel(this)

        if (hasUsageSettingPermission(this)) {
            val enrollment = EnrollmentSettings(this)
            if (enrollment.enrolled) {
                val studyIdText = findViewById<TextView>(R.id.studyId)
                val participantIdText = findViewById<TextView>(R.id.participantId)
                val orgIdTextView = findViewById<TextView>(R.id.orgId)

                studyIdText.text = enrollment.getStudyId().toString()
                participantIdText.text = enrollment.getParticipantId()
                orgIdTextView.text = enrollment.getOrganizationId().toString()

                if (enrollment.getParticipationStatus() == ParticipationStatus.ENROLLED) {
                    scheduleUploadJob(this)
                    scheduleUsageMonitoringJob(this)
                }
                scheduleEnrollmentStatusJob(this)
                handler.post(this::updateLastUpload)
            } else {
                startActivity(Intent(this, Enrollment::class.java))
            }
        } else {
            startActivity(Intent(this, PermissionActivity::class.java))
            return
        }

    }

    private fun updateLastUpload() {
        val lastUploadText = findViewById<TextView>(R.id.lastUploadValue)
        lastUploadText.text = getLastUpload(this)
        handler.postDelayed(this::updateLastUpload, LAST_UPLOAD_REFRESH_INTERVAL)
    }
}
