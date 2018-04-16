package com.openlattice.chronicle

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.TargetApi
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v7.app.AppCompatActivity
import com.openlattice.chronicle.R
import android.text.TextUtils
import android.view.View

import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

//    TODO: Replace UserLoginTask with logging task
    private var mAuthTask: UserLoginTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main)

        start_collection_button.setOnClickListener { attemptStartLog() }

    }

    private fun attemptStartLog() {
        if (mAuthTask != null) {
            return
        }

        // Reset errors.
        participantId.error = null
        studyId.error = null

        // Store values at the time of the login attempt.
        val participantStr = participantId.text.toString()
        val studyStr = studyId.text.toString()

        var cancel = false
        var focusView: View? = null

        // Check for a valid studyId.
        if (TextUtils.isEmpty(studyStr)) {
            studyId.error = getString(R.string.error_field_required)
            focusView = studyId
            cancel = true
        } else if (!isParticipantValid(studyStr)) {
            studyId.error = getString(R.string.error_invalid_study_id)
            focusView = studyId
            cancel = true
        }

        // Check for a valid participantId.
        if (TextUtils.isEmpty(participantStr)) {
            participantId.error = getString(R.string.error_field_required)
            focusView = participantId
            cancel = true
        } else if (!isParticipantValid(participantStr)) {
            participantId.error = getString(R.string.error_invalid_participant_id)
            focusView = participantId
            cancel = true
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView?.requestFocus()
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true)
            mAuthTask = UserLoginTask(participantStr, studyStr)
            mAuthTask!!.execute(null as Void?)
        }
    }

    private fun isParticipantValid(participantId: String): Boolean {
        //TODO: Replace this with proper logic
        return participantId.length > 4
    }

    private fun isStudyValid(studyId: String): Boolean {
        //TODO: Replace this with proper logic
        return studyId.length > 4
    }


    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private fun showProgress(show: Boolean) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            val shortAnimTime = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

            login_form.visibility = if (show) View.GONE else View.VISIBLE
            login_form.animate()
                    .setDuration(shortAnimTime)
                    .alpha((if (show) 0 else 1).toFloat())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            login_form.visibility = if (show) View.GONE else View.VISIBLE
                        }
                    })

            login_progress.visibility = if (show) View.VISIBLE else View.GONE
            login_progress.animate()
                    .setDuration(shortAnimTime)
                    .alpha((if (show) 1 else 0).toFloat())
                    .setListener(object : AnimatorListenerAdapter() {
                        override fun onAnimationEnd(animation: Animator) {
                            login_progress.visibility = if (show) View.VISIBLE else View.GONE
                        }
                    })
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            login_progress.visibility = if (show) View.VISIBLE else View.GONE
            login_form.visibility = if (show) View.GONE else View.VISIBLE
        }
    }

    inner class UserLoginTask internal constructor(private val mParticipant: String, private val mStudy: String) : AsyncTask<Void, Void, Boolean>() {
        override fun doInBackground(vararg params: Void): Boolean? {
            // TODO: attempt authentication against a network service.

            try {
                // Simulate network access.
                Thread.sleep(2000)
            } catch (e: InterruptedException) {
                return false
            }

            return DUMMY_CREDENTIALS
                    .map { it.split(":") }
                    .firstOrNull { it[0] == mParticipant }
                    ?.let {
                        // Account exists, return true if the password matches.
                        it[1] == mStudy
                    }
                    ?: true
        }

        override fun onPostExecute(success: Boolean?) {
            mAuthTask = null
            showProgress(false)

            if (success!!) {
                finish()
            } else {
//                TODO: Is studyId wanted here?
                studyId.error = getString(R.string.error_invalid_study_id)
                studyId.requestFocus()
            }
        }

        override fun onCancelled() {
            mAuthTask = null
            showProgress(false)
        }
    }

    companion object {

        /**
         * A dummy authentication store containing known user names and passwords.
         * TODO: remove after connecting to a real authentication system.
         */
        private val DUMMY_CREDENTIALS = arrayOf("55555:66666", "77777:88888")
    }

}