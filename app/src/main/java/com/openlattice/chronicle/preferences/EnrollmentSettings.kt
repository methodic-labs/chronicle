package com.openlattice.chronicle.preferences

import android.content.Context
import android.os.Build
import android.preference.PreferenceManager
import android.provider.Settings
import com.crashlytics.android.Crashlytics
import com.google.common.base.Optional
import com.openlattice.chronicle.sources.AndroidDevice
import java.security.AccessController.getContext
import java.util.*

val PARTICIPANT_ID = "participantId";
val STUDTY_ID = "studyId";

class EnrollmentSettings(private val context: Context) {
    private val settings = PreferenceManager.getDefaultSharedPreferences(context)

    private var participantId: String
    private var studyId: UUID
    var enrolled: Boolean = true

    init {
        val studyIdString = settings.getString(STUDTY_ID, "")
        participantId = settings.getString(PARTICIPANT_ID, "")
        if (studyIdString.isNotBlank() && participantId.isNotBlank()) {
            studyId = UUID.fromString(studyIdString)
            setCrashlyticsUser(studyId, participantId, getDeviceId(context))
            enrolled = true
        } else {
            studyId = UUID(0, 0)
            enrolled = false
        }
    }

    fun getParticipantId(): String {
        return participantId
    }

    fun getStudyId(): UUID {
        return studyId
    }

    fun setParticipantId(_participantId: String) {
        participantId = _participantId
        settings.edit()
                .putString(PARTICIPANT_ID, _participantId)
                .apply()
    }

    fun setStudyId(_studyId: UUID) {
        studyId = _studyId
        settings.edit()
                .putString(STUDTY_ID, _studyId.toString())
                .apply()
    }
}

fun getDeviceId(context: Context): String {
    return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
}

fun getDevice(deviceId: String): AndroidDevice {
    return AndroidDevice(deviceId, Build.MODEL, Build.VERSION.CODENAME, Build.BRAND, Build.DISPLAY, Build.VERSION.SDK_INT.toString(), Build.PRODUCT, deviceId, Optional.absent())
}

fun setCrashlyticsUser(studyId: UUID, participantId: String, deviceId: String) {
    Crashlytics.setUserIdentifier(participantId)
    Crashlytics.setUserEmail("$participantId@$studyId")
    Crashlytics.setUserName(deviceId)
}