package com.openlattice.chronicle

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v7.app.AppCompatActivity
import android.widget.TextView
import com.crashlytics.android.Crashlytics
import com.openlattice.chronicle.preferences.EnrollmentSettings
import com.openlattice.chronicle.services.upload.scheduleUploadJob
import com.openlattice.chronicle.services.upload.getLastUpload
import com.openlattice.chronicle.services.usage.scheduleUsageEventsJob
import io.fabric.sdk.android.Fabric


const val LAST_UPLOAD_REFRESH_INTERVAL = 5000L

class MainActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Fabric.with(this,  Crashlytics())
        setContentView(R.layout.activity_main)

        if (hasUsageSettingPermission(this)) {
            val enrollment = EnrollmentSettings(this)
            if (enrollment.enrolled) {
                val studyIdText = findViewById<TextView>(R.id.mainStudyId)
                val participantIdText = findViewById<TextView>(R.id.mainParticipantId)
                studyIdText.text = "Study Id: ${enrollment.getStudyId()}"
                participantIdText.text = "Participant Id: ${enrollment.getParticipantId()}"
                scheduleUploadJob(this)
                scheduleUsageEventsJob(this)
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
        val lastUploadText = findViewById<TextView>(R.id.lastUploadText)
        lastUploadText.text = "Last Upload: ${getLastUpload(this)}"
        handler.postDelayed(this::updateLastUpload, LAST_UPLOAD_REFRESH_INTERVAL)
    }
}