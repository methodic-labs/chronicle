package com.openlattice.chronicle

import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.openlattice.chronicle.preferences.EnrollmentSettings
import com.openlattice.chronicle.services.usage.scheduleUsageMonitoringWork

class UserIdentificationActivity : AppCompatActivity() {
    private lateinit var selectUserOptions: RadioGroup
    private lateinit var saveBtn: Button

    private lateinit var settings: EnrollmentSettings
    private lateinit var targetUser: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_identification)

        settings = EnrollmentSettings(this)
        saveBtn = findViewById(R.id.save_btn)
        selectUserOptions = findViewById(R.id.select_user_options)

        // listeners
        saveBtn.setOnClickListener {
            handleOnSave()
        }

        val checkedRadioBtnId = selectUserOptions.checkedRadioButtonId
        setTargetUser(checkedRadioBtnId)

        selectUserOptions.setOnCheckedChangeListener { _, checkedId ->
            setTargetUser(checkedId)
        }
    }

    private fun setTargetUser(checkedId: Int) {
        when (checkedId) {
            R.id.user_target_child -> {
                targetUser = getString(R.string.user_target_child)
            }

            R.id.user_other -> {
                targetUser = getString(R.string.user_other)
            }
        }
    }

    private fun handleOnSave() {
        settings.setTargetUser(targetUser)
        scheduleUsageMonitoringWork(this)
        Toast.makeText(this, "Device user has been set to \"$targetUser\"", Toast.LENGTH_SHORT).show()
        finish()
    }
}
