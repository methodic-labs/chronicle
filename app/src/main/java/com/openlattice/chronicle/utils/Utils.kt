package com.openlattice.chronicle.utils

import com.crashlytics.android.Crashlytics

import java.util.UUID

object Utils {

    fun isValidUUID(possibleUUID: String): Boolean = try {
        val uuid = UUID.fromString(possibleUUID)
        val uuidAsString = uuid.toString()
        uuidAsString == possibleUUID
    }
    catch (e: IllegalArgumentException) {
        Crashlytics.logException(e)
        false
    }
}
