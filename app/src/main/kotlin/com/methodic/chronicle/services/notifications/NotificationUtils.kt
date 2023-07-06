package com.methodic.chronicle.services.notifications

import android.util.Log
import android.widget.Toast
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await


fun getFirebaseRegistrationToken() : String {
    return runBlocking { FirebaseMessaging.getInstance().token.await() }

//
//    addOnCompleteListener(OnCompleteListener { task ->
//        if (!task.isSuccessful) {
//            Log.w(TAG, "Fetching FCM registration token failed", task.exception)
//            return@OnCompleteListener
//        }
//
//
//        val token = task.result
//
//        // Log and toast
//
//        Log.d(TAG, "")
//        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
//    }
}