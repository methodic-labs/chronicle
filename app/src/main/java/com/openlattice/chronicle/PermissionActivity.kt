package com.openlattice.chronicle

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.methodic.chronicle.R

class PermissionActivity : AppCompatActivity() {

    private lateinit var openSettingsBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permission)
        if (hasUsageSettingPermission(this)) {
            doMainActivity(this, intent)
            finish()
        }

        openSettingsBtn = findViewById(R.id.open_settings_btn)
        openSettingsBtn.setOnClickListener{
            handleGetUsageAccessSettings()
        }
    }

    private fun handleGetUsageAccessSettings() {
        getUsageAccessSettings()
        waitOnUsageSettingPermission()
    }

    private fun getUsageAccessSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            val permissionsText = findViewById<TextView>(R.id.permissionsText)
            permissionsText.text = getString(R.string.manual_usage_settings_permission)
        }
    }

    private fun waitOnUsageSettingPermission() {
        val packageManager = applicationContext.packageManager
        val applicationInfo = packageManager.getApplicationInfo(applicationContext.packageName, 0)
        val appOpsManager = applicationContext.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        appOpsManager.startWatchingMode(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                applicationInfo.packageName,
                { op, packageName ->
                    Log.i(packageName, "Usage stats settings changed detected launching main class")
                    doMainActivity(applicationContext, intent)
                    finish()
                })
    }
}

fun doMainActivity(context: Context, intent: Intent) {
    val newActivityIntent = Intent(context, MainActivity::class.java).apply {
        action = intent.action
        data = intent.data
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    context.startActivity(newActivityIntent)
}

fun hasUsageSettingPermission(context: Context): Boolean {
    return try {
        val packageManager = context.packageManager
        val applicationInfo = packageManager.getApplicationInfo(context.packageName, 0)
        val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, applicationInfo.uid, applicationInfo.packageName)
        mode == AppOpsManager.MODE_ALLOWED
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}


@RequiresApi(Build.VERSION_CODES.M)
fun hasIgnoreBatteryOptimization(context: Context): Boolean {
    return try {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager;
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)

    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}
