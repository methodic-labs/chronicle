package com.openlattice.chronicle.preferences

import android.content.Context
import android.os.Build
import android.preference.PreferenceManager
import android.provider.Settings
import com.google.common.base.Optional
import com.openlattice.chronicle.sources.AndroidDevice
import java.security.AccessController.getContext
import java.util.*

val PARTICIPANT_ID = "participantId";
val STUDTY_ID = "studyId";

class EnrollmentSettings(val context: Context) {
    val settings = PreferenceManager.getDefaultSharedPreferences(context)

    private var participantId = settings.getString(PARTICIPANT_ID, "")
    private var studyId = UUID.fromString(settings.getString(STUDTY_ID, ""))

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
                .commit()
    }

    fun setStudyId(_studyId: UUID) {
        studyId = _studyId
        settings.edit()
                .putString(STUDTY_ID, _studyId.toString())
                .commit()
    }
}

fun getDeviceId(context: Context): String {
    return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
}

fun getDevice(deviceId: String): AndroidDevice {
    return AndroidDevice(deviceId, Build.MODEL, Build.VERSION.CODENAME, Build.BRAND, Build.DISPLAY, Build.VERSION.SDK_INT.toString(), Build.PRODUCT, deviceId, Optional.absent())
}