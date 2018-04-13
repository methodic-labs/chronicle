package com.openlattice.chronicle

import android.os.Bundle
import android.os.PersistableBundle
import android.support.v7.app.AppCompatActivity
import com.openlattice.chronicle.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main)

        start_collection_button.setOnClickListener { attemptStartLog() }

    }

    private fun attemptStartLog() {
//        if (mAuthTask != null) {
//            return
//        }
//
//        // Reset errors.
//        participantId.error = null
//        studyid.error = null
//
//        // Store values at the time of the login attempt.
//        val emailStr = participantId.text.toString()
//        val passwordStr = studyid.text.toString()
//
//        var cancel = false
//        var focusView: View? = null
//
//        // Check for a valid password, if the user entered one.
//        if (!TextUtils.isEmpty(passwordStr) && !isPasswordValid(passwordStr)) {
//            studyid.error = getString(R.string.error_invalid_password)
//            focusView = studyid
//            cancel = true
//        }
//
//        // Check for a valid participantId.
//        if (TextUtils.isEmpty(emailStr)) {
//            participantId.error = getString(R.string.error_field_required)
//            focusView = participantId
//            cancel = true
//        } else if (!isEmailValid(emailStr)) {
//            participantId.error = getString(R.string.error_invalid_email)
//            focusView = participantId
//            cancel = true
//        }
//
//        if (cancel) {
//            // There was an error; don't attempt login and focus the first
//            // form field with an error.
//            focusView?.requestFocus()
//        } else {
//            // Show a progress spinner, and kick off a background task to
//            // perform the user login attempt.
//            showProgress(true)
//            mAuthTask = UserLoginTask(emailStr, passwordStr)
//            mAuthTask!!.execute(null as Void?)
//        }
    }
}