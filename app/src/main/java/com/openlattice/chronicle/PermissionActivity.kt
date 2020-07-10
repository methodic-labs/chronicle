package com.openlattice.chronicle

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.TextView
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric


class PermissionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Fabric.with(this,  Crashlytics())
        setContentView(R.layout.activity_permission)
        if (hasUsageSettingPermission(this)) {
            doMainActivity(this)
            finish()
        }
    }


    fun handleGetUsageAccessSettings(view: View) {
        getUsageAccessSettings()
        waitOnUsageSettingPermission()
    }

    fun getUsageAccessSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            val permissionsText = findViewById<TextView>(R.id.permissionsText)
            permissionsText.text = getString(R.string.manual_usage_settings_permission)
        }
    }

    fun waitOnUsageSettingPermission() {
        val packageManager = applicationContext.packageManager
        val applicationInfo = packageManager.getApplicationInfo(applicationContext.packageName, 0)
        val appOpsManager = applicationContext.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        appOpsManager.startWatchingMode(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                applicationInfo.packageName,
                { op, packageName ->
                    Log.i(packageName, "Usage stats settings changed detected launching main class")
                    doMainActivity(applicationContext)
                    finish()
                })
    }
}



fun doMainActivity(context: Context) {
    val intent = Intent(context, MainActivity::class.java)
    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    context.startActivity(intent)
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
