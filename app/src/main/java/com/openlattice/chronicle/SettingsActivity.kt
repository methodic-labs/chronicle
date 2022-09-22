package com.openlattice.chronicle

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.methodic.chronicle.R
import com.openlattice.chronicle.preferences.EnrollmentSettings
import com.openlattice.chronicle.services.notifications.DeviceUnlockMonitoringService
import com.openlattice.chronicle.services.usage.scheduleUsageMonitoringWork

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        private lateinit var userIdentificationPreference: SwitchPreferenceCompat
//        private lateinit var batteryOptimizationDialogPreference: SwitchPreferenceCompat
        private lateinit var notificationAccessPreference: SwitchPreferenceCompat
        private lateinit var targetUserPreference: ListPreference

        private var settings: EnrollmentSettings? = null

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            settings = context?.let { EnrollmentSettings(it) }
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            userIdentificationPreference = findPreference(getString(R.string.identify_user))!!
            targetUserPreference = findPreference(getString(R.string.current_user))!!
//            batteryOptimizationDialogPreference =
//                findPreference(getString(R.string.disable_battery_optimization_dialog))!!
            notificationAccessPreference = findPreference(getString(R.string.enable_notification_access))!!

            settings?.let {
                targetUserPreference.setDefaultValue(it.getCurrentUser())
            }
            targetUserPreference.isEnabled = userIdentificationPreference.isChecked

            userIdentificationPreference.setOnPreferenceChangeListener { _, newValue ->
                val isChecked = newValue as Boolean
                targetUserPreference.isEnabled = isChecked

                context?.let { context ->
                    if (isChecked) {
                        DeviceUnlockMonitoringService.startService(context)
                    } else {
                        settings?.setTargetUser(context.getString(R.string.user_unassigned))
                        targetUserPreference.value = context.getString(R.string.user_unassigned)
                        scheduleUsageMonitoringWork(context)
                        DeviceUnlockMonitoringService.stopService(context)
                    }
                }

                true
            }

            targetUserPreference.setOnPreferenceChangeListener { preference, newValue ->
                activity?.let { scheduleUsageMonitoringWork(it.applicationContext) }
                true
            }

            // optionally show battery optimization setting
//            batteryOptimizationDialogPreference.parent?.isVisible =
//                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
//
//            batteryOptimizationDialogPreference.setOnPreferenceChangeListener { _, newValue ->
//                settings?.toggleBatteryOptimizationDialog(newValue as Boolean)
//                true
//            }

            val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(requireContext())
            val accessCurrentlyEnabled = enabledPackages.contains(context?.packageName)
            notificationAccessPreference.isChecked = accessCurrentlyEnabled

            notificationAccessPreference.setOnPreferenceChangeListener { _, newValue ->
                val enabled = newValue as Boolean
                if (notificationAccessPreference.isChecked.xor(enabled)) {
                    val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                    startActivityForResult(intent, 1)
                }
                true
            }
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            if (requestCode == 1) {
                val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(requireContext())
                val enabled = enabledPackages.contains(context?.packageName)
                notificationAccessPreference.isChecked = enabled
            }
        }

        override fun onResume() {
            super.onResume()
            val userIdentificationEnabled = userIdentificationPreference.isEnabled
            if (userIdentificationEnabled) {
                targetUserPreference.value = settings?.getCurrentUser()
            }
        }

        override fun onStop() {
            settings?.closeDb()
            super.onStop()
        }


    }
}