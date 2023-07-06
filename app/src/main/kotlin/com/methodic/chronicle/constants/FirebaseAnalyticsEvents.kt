package com.methodic.chronicle.constants

class FirebaseAnalyticsEvents {
    companion object {
        const val ENROLLMENT_SUCCESS = "enrollment_success"
        const val ENROLLMENT_FAILURE = "enrollment_fail"
        const val UPLOAD_START = "upload_start"
        const val UPLOAD_BATCH_SUCCESS = "upload_batch_success"
        const val UPLOAD_SUCCESS = "upload_success"
        const val UPLOAD_FAILURE = "upload_failure"
        const val SUBMIT_FAILURE = "submit_failure"
        const val USAGE_START = "usage_sensor_start"
        const val USAGE_SUCCESS = "usage_sensor_success"
        const val USAGE_FAILURE = "usage_sensor_failure"
        const val NOTIFICATIONS_START = "notifications_start"
        const val NOTIFICATIONS_FAILURE = "notification_failure"
    }
}