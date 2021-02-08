package com.openlattice.chronicle

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity

class BatteryOptimizationExemption : AppCompatActivity() {

    private lateinit var settingsButton: Button

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.optimization_exemption)

        settingsButton = findViewById(R.id.open_settings_btn)
        settingsButton.setOnClickListener { openSettings() }

    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun openSettings() {
        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        startActivityForResult(intent, 1)
    }

    // ignore result and go to MainActivity
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        startActivity(Intent(this, MainActivity::class.java))
    }
}