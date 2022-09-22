package com.openlattice.chronicle

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.work.WorkInfo
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.ktx.Firebase
import com.methodic.chronicle.R
import com.openlattice.chronicle.data.ParticipationStatus
import com.openlattice.chronicle.models.UploadStatusModel
import com.openlattice.chronicle.preferences.EnrollmentSettings
import com.openlattice.chronicle.services.notifications.DeviceUnlockMonitoringService
import com.openlattice.chronicle.services.notifications.scheduleNotificationsWorker
import com.openlattice.chronicle.services.upload.scheduleUploadWork
import com.openlattice.chronicle.services.usage.scheduleUsageMonitoringWork
import com.openlattice.chronicle.utils.Utils.getLastUpload
import kotlinx.coroutines.*
import java.util.*

const val LAST_UPLOAD_REFRESH_INTERVAL = 5000L

class MainActivity : AppCompatActivity() {

    private lateinit var enrollmentSettings: EnrollmentSettings
    private lateinit var studyId: UUID
    private lateinit var participantId: String
    private lateinit var participationStatus: ParticipationStatus
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    private lateinit var lastUploadText: TextView
    private lateinit var studyIdText: TextView
    private lateinit var participantIdText: TextView
    private lateinit var uploadProgressView: LinearLayout

    private val uploadStatusModel: UploadStatusModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lastUploadText = findViewById(R.id.lastUploadValue)
        firebaseAnalytics = Firebase.analytics
        enrollmentSettings = EnrollmentSettings(this)
        studyId = enrollmentSettings.getStudyId()
        participantId = enrollmentSettings.getParticipantId()
        participationStatus = enrollmentSettings.getParticipationStatus()

        studyIdText = findViewById(R.id.studyId)
        participantIdText = findViewById(R.id.participantId)
        uploadProgressView = findViewById(R.id.uploadProgressView)

        uploadProgressView.visibility = View.GONE

        // Always enable crashlytics. Debug crashes will go to separate app on firebase
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)


        if (!hasUsageSettingPermission(this)) {
            startActivity(Intent(this, PermissionActivity::class.java))
            finish()
        }

        // observer
        uploadStatusModel.outputWorkInfo.observe(this, workInfoObserver())

        if (enrollmentSettings.isEnrolled()) {

            firebaseAnalytics.setUserId("${studyId}_${participantId}")

            studyIdText.text = studyId.toString()
            participantIdText.text = participantId


            GlobalScope.launch(Dispatchers.Main) {
                updateLastUpload()
            }

            if (enrollmentSettings.isUserIdentificationEnabled()) {
                DeviceUnlockMonitoringService.startService(this)
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
    }

    private fun workInfoObserver(): Observer<List<WorkInfo>> {
        return Observer { listOfWorkInfo ->

            // if there is no matching work info, do nothing
            if (listOfWorkInfo.isNullOrEmpty()) {
                return@Observer
            }

            val workInfo = listOfWorkInfo[0]

            if (workInfo.state.name == WorkInfo.State.RUNNING.name) {
                uploadProgressView.visibility = View.VISIBLE
            } else {
                uploadProgressView.visibility = View.GONE
            }

        }
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

    override fun onBackPressed() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }

    // check that devices with android 6.0 (api 23) are exempt from Doze and App Standby optimizations
    // https://developer.android.com/training/monitoring-device-state/doze-standby
    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !hasIgnoreBatteryOptimization(this) && enrollmentSettings.isBatteryOptimizationDialogEnabled()) {
            BatteryOptimizationExemptionDialog().show(supportFragmentManager, "batteryExemption")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // handle menu selection
        return when (item.itemId) {
            R.id.settings -> {
                // launch settings
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
