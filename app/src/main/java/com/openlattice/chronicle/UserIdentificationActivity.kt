package com.openlattice.chronicle

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.openlattice.chronicle.preferences.EnrollmentSettings
import com.openlattice.chronicle.services.usage.scheduleUsageMonitoringWork

class UserIdentificationActivity : AppCompatActivity() {
    private lateinit var childUserBtn: Button
    private lateinit var otherUserBtn: Button

    private lateinit var settings: EnrollmentSettings
    private lateinit var targetUser: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_identification)

        settings = EnrollmentSettings(this)
        childUserBtn = findViewById(R.id.child_user_btn)
        otherUserBtn = findViewById(R.id.other_user_btn)

        // listeners
        otherUserBtn.setOnClickListener {
            handleOnSave(it.id)
        }

        childUserBtn.setOnClickListener {
            handleOnSave(it.id)
        }
    }

    private fun handleOnSave(buttonId: Int ) {
        targetUser = if (buttonId == R.id.other_user_btn) getString(R.string.user_other) else getString(R.string.user_target_child)
        settings.setTargetUser(targetUser)
        scheduleUsageMonitoringWork(this)
        Toast.makeText(this, "Device user has been set to \"$targetUser\"", Toast.LENGTH_SHORT).show()
        finish()
    }
}
