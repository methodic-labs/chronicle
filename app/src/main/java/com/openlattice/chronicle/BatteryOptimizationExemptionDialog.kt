package com.openlattice.chronicle

import android.app.Dialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

class BatteryOptimizationExemptionDialog : DialogFragment() {
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        return activity?.let {
            val builder = AlertDialog.Builder(it)
            builder.setTitle("Battery Optimization")
                    .setMessage(R.string.battery_optimization_exemption)
                    .setPositiveButton(R.string.settings
                    ) { dialog, _ ->
                        dialog.cancel()
                        val intent = Intent().apply {
                            action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                        }
                        startActivity(intent)
                    }
                    .setNegativeButton(android.R.string.cancel
                    ) { dialog, _ ->
                        // TODO: maybe we should record that the user opted to cancel dialog???
                        dialog.cancel()
                    }

            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

}