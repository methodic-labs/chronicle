package com.methodic.chronicle.services.notifications

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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
    private lateinit var openSettingsBtn: Button
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.i(Enrollment::class.java.name, "Notifications enabled!")
            doMainActivity(this, intent)
            finish()
        } else {
            //TODO: Inform researchers that user blocked notifications for their study
            Log.e(Enrollment::class.java.name, "Unable to send notifications!")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission_notification)
        if (hasNotificationPermission(this)) {
            doMainActivity(this, intent)
            finish()
        }

        openSettingsBtn = findViewById(R.id.open_notification_settings_btn)
        openSettingsBtn.setOnClickListener {
            requestPostNotificationsPermissions()
        }
    }

    @Deprecated("Remains for legacy android versions")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            if (!hasNotificationPermission(applicationContext)) {
                updateViewForUserAbort()
            } else {
                doMainActivity(this, intent)
                finish()
            }
        }

    }

    private fun requestPostNotificationsPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            } else {
                val packageName = applicationContext.packageName
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                }
                startActivityForResult(intent, 1)
            }
        } else {
            updateViewForOldVersions()
        }

        //These permissions is actually for NotificationListenerService
        //Which we will need to hook to receive notifications to settings changes.
        //This might not be as important to hook at the moment, since we can directly
        //check notification settings changes every time uploadWorker runs.
        //Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

    }

    private fun updateViewForUserAbort() {
        val permissionsText = findViewById<TextView>(R.id.notificationPermissionsText)
        permissionsText.text = getString(R.string.notification_permissions_required)
    }

    /**
     * For version newer than [Build.VERSION_CODES.O] we can access notification settings via intent
     * or via the request permission launcher. For older versions, we simply inform the user that
     * they must manually grant permissions for notifications.
     */
    private fun updateViewForOldVersions() {
        val permissionsText = findViewById<TextView>(R.id.notificationPermissionsText)
        permissionsText.text = getString(R.string.manual_notification_settings_permission)
    }
}

fun hasNotificationPermission(context: Context): Boolean {
    val notificationManagerCompat = NotificationManagerCompat.from(context)
    return notificationManagerCompat.areNotificationsEnabled()
}
