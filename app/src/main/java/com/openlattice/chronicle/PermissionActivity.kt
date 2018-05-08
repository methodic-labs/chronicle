package com.openlattice.chronicle

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import android.content.pm.PackageManager
import android.app.AppOpsManager
import android.content.Context
import android.content.Context.APP_OPS_SERVICE
import android.content.pm.ApplicationInfo
import android.support.v4.content.ContextCompat.startActivity
import android.util.Log
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
        }
    }


    fun handleGetUsageAccessSettings(view: View) {
        getUsageAccessSettings()
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
}

fun waitOnUsageSettingPermission(context: Context) {
    val packageManager = context.packageManager
    val applicationInfo = packageManager.getApplicationInfo(context.packageName, 0)
    val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    appOpsManager.startWatchingMode(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            applicationInfo.packageName,
            { op, packageName ->
                Log.i(packageName, "Usage stats settings changed detected launching main class")
                doMainActivity(context)
            })
}

fun doMainActivity(context: Context) {
    context.startActivity(Intent(context, MainActivity::class.java))
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
