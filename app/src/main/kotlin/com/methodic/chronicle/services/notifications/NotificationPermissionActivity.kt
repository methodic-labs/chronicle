package com.methodic.chronicle.services.notifications

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import com.openlattice.chronicle.Enrollment
import com.openlattice.chronicle.R
import com.openlattice.chronicle.doMainActivity

class NotificationPermissionActivity : AppCompatActivity() {
    companion object {
        var currentPermissionActivity : NotificationPermissionActivity? = null
    }
    private lateinit var openSettingsBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission_notification)
        if (hasNotificationPermission(this)) {
            doMainActivity(this, intent)
            finish()
        }

        openSettingsBtn = findViewById(R.id.open_notification_settings_btn)
        openSettingsBtn.setOnClickListener{
            handleGetNotificationSettings()
        }
    }

    private fun handleGetNotificationSettings() {
        getUsageAccessSettings()
//        currentPermissionActivity = this
    }
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.i(Enrollment::class.java.name, "Notifications enabled!")
            doMainActivity(this, intent)
        } else {
            // Inform researchers that user blocked notifications for their study
            Log.e(Enrollment::class.java.name,"Unable to send notifications!" )
        }
    }
    private fun getUsageAccessSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

        if (intent.resolveActivity(packageManager) != null) {
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
//            startActivityForResult(intent, 1)
        } else {
            val permissionsText = findViewById<TextView>(R.id.notificationPermissionsText)
            permissionsText.text = getString(R.string.manual_usage_settings_permission)
        }
    }
}

fun hasNotificationPermission(context: Context): Boolean {
    val notificationManagerCompat = NotificationManagerCompat.from(context)
    return notificationManagerCompat.areNotificationsEnabled()
}
