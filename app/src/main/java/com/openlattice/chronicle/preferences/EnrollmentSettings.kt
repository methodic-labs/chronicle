package com.openlattice.chronicle.preferences

import android.content.Context
import android.os.Build
import android.preference.PreferenceManager
import android.provider.Settings
import com.google.common.base.Optional
import com.google.common.collect.ImmutableMap
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.openlattice.chronicle.data.ParticipationStatus
import com.openlattice.chronicle.serialization.JsonSerializer.deserializePropertyTypeIds
import com.openlattice.chronicle.serialization.JsonSerializer.serializePropertyTypeIds
import com.openlattice.chronicle.sources.AndroidDevice
import com.openlattice.chronicle.utils.Utils
import java.util.*

const val PARTICIPANT_ID = "participantId"
const val AWARENESS_NOTIFICATIONS_ENABLED = "notificationsEnabled"
const val STUDY_ID = "studyId"
const val ORGANIZATION_ID = "organizationId"
const val DEVICE_ID = "deviceId"
const val PARTICIPATION_STATUS = "participationStatus"
const val PROPERTY_TYPE_IDS = "com.openlattice.PropertyTypeIds"

val INVALID_STUDY_ID = UUID(0, 0)
val INVALID_ORG_ID = INVALID_STUDY_ID;

class EnrollmentSettings(private val context: Context) {
    private val settings = PreferenceManager.getDefaultSharedPreferences(context)

    private var participantId: String
    private var studyId: UUID
    private var organizationId: UUID
    var enrolled: Boolean = true

    init {
        val orgIdStr = settings.getString(ORGANIZATION_ID, "") ?: ""
        val studyIdString = settings.getString(STUDY_ID, "") ?: ""
        participantId = settings.getString(PARTICIPANT_ID, "") ?: ""

        if ( Utils.isValidUUID(orgIdStr) && Utils.isValidUUID(studyIdString) && participantId.isNotBlank()) {
            studyId = UUID.fromString(studyIdString)
            organizationId = UUID.fromString(orgIdStr)
            setCrashlyticsUser(participantId)
            setCrashlyticsState(studyId, getDeviceId(context))
        } else {
            studyId = INVALID_STUDY_ID
            organizationId = INVALID_ORG_ID
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

    fun getOrganizationId(): UUID {
        return organizationId
    }

    fun setOrganizationId(orgId: UUID) {
        organizationId = orgId;
        settings.edit()
                .putString(ORGANIZATION_ID, orgId.toString())
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

    fun setAwarenessNotificationsEnabled(notificationsEnabled: Boolean) {
        settings.edit()
                .putBoolean(AWARENESS_NOTIFICATIONS_ENABLED, notificationsEnabled)
                .apply()
    }

    fun getAwarenessNotificationsEnabled(): Boolean {
        return settings.getBoolean(AWARENESS_NOTIFICATIONS_ENABLED, false)
    }

    fun updateEnrolled() {
        enrolled = !( organizationId.equals(INVALID_STUDY_ID) || studyId.equals(INVALID_STUDY_ID) || participantId.isBlank())
    }

    fun setPropertyTypeIds(propertyTypeIds: Map<String, UUID>) {
        settings
                .edit()
                .putString(PROPERTY_TYPE_IDS, serializePropertyTypeIds(propertyTypeIds))
                .apply()
    }

    fun getPropertyTypeIds(): Map<String, UUID> {
        return deserializePropertyTypeIds(settings.getString(PROPERTY_TYPE_IDS, ""))
                ?: ImmutableMap.of()
    }

    fun setParticipationStatus(participationStatus: ParticipationStatus) {
        settings
                .edit()
                .putString(PARTICIPATION_STATUS, participationStatus.toString())
                .apply()
    }

    fun getParticipationStatus(): ParticipationStatus {
        return ParticipationStatus.valueOf(settings.getString(PARTICIPATION_STATUS, ParticipationStatus.UNKNOWN.toString()))
    }

}

fun getDeviceId(context: Context): String {
    return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
}

fun getDevice(deviceId: String): AndroidDevice {
    return AndroidDevice(deviceId, Build.MODEL, Build.VERSION.CODENAME, Build.BRAND, Build.DISPLAY, Build.VERSION.SDK_INT.toString(), Build.PRODUCT, deviceId, Optional.absent())
}

fun setCrashlyticsUser(participantId: String) {
    val crashlytics = FirebaseCrashlytics.getInstance()

    crashlytics.setUserId(participantId)
}
fun setCrashlyticsState(studyId: UUID, deviceId: String) {
    val crashlytics = FirebaseCrashlytics.getInstance()

    crashlytics.setCustomKey(DEVICE_ID, deviceId)
    crashlytics.setCustomKey(STUDY_ID, studyId.toString())
}
