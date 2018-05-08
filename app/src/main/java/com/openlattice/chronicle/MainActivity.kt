package com.openlattice.chronicle

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v7.app.AppCompatActivity
import android.widget.TextView
import com.openlattice.chronicle.preferences.EnrollmentSettings
import com.openlattice.chronicle.receivers.lifecycle.scheduleUploadJob
import com.openlattice.chronicle.services.upload.getLastUpload
import com.openlattice.chronicle.services.usage.scheduleUsageEventsJob


const val LAST_UPLOAD_REFRESH_INTERVAL = 5000L

class MainActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main)

        handler.post(this::updateLastUpload)

        if (hasUsageSettingPermission(this)) {
            val enrollment = EnrollmentSettings(this)
            if (enrollment.enrolled) {
                val studyIdText = findViewById<TextView>(R.id.mainStudyId)
                val participantIdText = findViewById<TextView>(R.id.mainParticipantId)
                studyIdText.text = "Study Id: ${enrollment.getStudyId()}"
                participantIdText.text = "Participant Id: ${enrollment.getParticipantId()}"
                scheduleUploadJob(this)
                scheduleUsageEventsJob(this)
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

    private fun updateLastUpload() {
        val lastUploadText = findViewById<TextView>(R.id.lastUploadText)
        lastUploadText.text = "Last Upload: ${getLastUpload(this)}"
        handler.postDelayed(this::updateLastUpload, LAST_UPLOAD_REFRESH_INTERVAL)
    }
}