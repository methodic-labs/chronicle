package com.openlattice.chronicle.preferences

import android.content.Context
import android.os.Build
import android.preference.PreferenceManager
import android.provider.Settings
import com.crashlytics.android.Crashlytics
import com.google.common.base.Optional
import com.openlattice.chronicle.sources.AndroidDevice
import java.util.*

const val PARTICIPANT_ID = "participantId";
const val STUDY_ID = "studyId"
val INVALID_STUDY_ID = UUID(0,0)

class EnrollmentSettings(private val context: Context) {
    private val settings = PreferenceManager.getDefaultSharedPreferences(context)

    private var participantId: String
    private var studyId: UUID
    var enrolled: Boolean = true

    init {
        val studyIdString = settings.getString(STUDY_ID, "")
        participantId = settings.getString(PARTICIPANT_ID, "")
        if (studyIdString.isNotBlank() && participantId.isNotBlank()) {
            studyId = UUID.fromString(studyIdString)
            setCrashlyticsUser(studyId, participantId, getDeviceId(context))
        } else {
            studyId = UUID(0, 0)
        }
        updateEnrolled()
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
        updateEnrolled()
    }

    fun setStudyId(_studyId: UUID) {
        studyId = _studyId
        settings.edit()
                .putString(STUDY_ID, _studyId.toString())
                .apply()
        updateEnrolled()
    }

    fun updateEnrolled() {
        enrolled = !(studyId.equals( INVALID_STUDY_ID) || participantId.isBlank())
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