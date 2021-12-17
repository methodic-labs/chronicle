package com.openlattice.chronicle

import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.openlattice.chronicle.preferences.EnrollmentSettings
import com.openlattice.chronicle.services.notifications.InteractivityMonitoringService
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
        private lateinit var batteryOptimizationDialogPreference: SwitchPreferenceCompat
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
            batteryOptimizationDialogPreference =
                findPreference(getString(R.string.disable_battery_optimization_dialog))!!

            settings?.let {
                targetUserPreference.setDefaultValue(it.getCurrentUser())
            }
            targetUserPreference.isEnabled = userIdentificationPreference.isChecked

            userIdentificationPreference.setOnPreferenceChangeListener { _, newValue ->
                val isChecked = newValue as Boolean
                targetUserPreference.isEnabled = isChecked

                if (!isChecked) {
                    context?.let {
                        settings?.setTargetUser(it.getString(R.string.user_unassigned))
                        targetUserPreference.value = it.getString(R.string.user_unassigned)
                        activity?.let { it1 -> scheduleUsageMonitoringWork(it1.applicationContext) }
                    }
                }

                context?.let {
                    InteractivityMonitoringService.startOrStopUnlockPhoneService(!isChecked, it)
                }

                true
            }

            targetUserPreference.setOnPreferenceChangeListener { preference, newValue ->
                activity?.let { scheduleUsageMonitoringWork(it.applicationContext) }
                true
            }

            // optionally show battery optimization setting
            batteryOptimizationDialogPreference.parent?.isVisible =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M

            batteryOptimizationDialogPreference.setOnPreferenceChangeListener { _, newValue ->
                settings?.toggleBatteryOptimizationDialog(newValue as Boolean)
                true
            }
        }
    }
}