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
import org.apache.olingo.commons.api.edm.FullQualifiedName
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

    init {
        val orgIdStr = settings.getString(ORGANIZATION_ID, "") ?: ""
        val studyIdString = settings.getString(STUDY_ID, "") ?: ""
        participantId = settings.getString(PARTICIPANT_ID, "") ?: ""

        studyId = if (Utils.isValidUUID(studyIdString)) UUID.fromString(studyIdString) else INVALID_STUDY_ID
        organizationId = if (Utils.isValidUUID(orgIdStr)) UUID.fromString(orgIdStr) else INVALID_ORG_ID

        if (isEnrolled()) {
            setCrashlyticsUser(participantId)
            setCrashlyticsState(organizationId, studyId, getDeviceId(context))
        }
    }

    fun isEnrolled(): Boolean {
        return !( studyId.equals(INVALID_STUDY_ID) || participantId.isBlank())
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

    fun getOrganizationId(): UUID {
        return organizationId
    }

    fun setOrganizationId(orgId: UUID) {
        organizationId = orgId;
        settings.edit()
                .putString(ORGANIZATION_ID, orgId.toString())
                .apply()
    }

    fun setStudyId(_studyId: UUID) {
        studyId = _studyId
        settings.edit()
                .putString(STUDY_ID, _studyId.toString())
                .apply()
    }

    fun setAwarenessNotificationsEnabled(notificationsEnabled: Boolean) {
        settings.edit()
                .putBoolean(AWARENESS_NOTIFICATIONS_ENABLED, notificationsEnabled)
                .apply()
    }

    fun getAwarenessNotificationsEnabled(): Boolean {
        return settings.getBoolean(AWARENESS_NOTIFICATIONS_ENABLED, false)
    }

    fun setPropertyTypeIds(propertyTypeIds: Map<FullQualifiedName, UUID>) {
        settings
                .edit()
                .putString(PROPERTY_TYPE_IDS, serializePropertyTypeIds(propertyTypeIds))
                .apply()
    }

    fun getPropertyTypeIds(): Map<FullQualifiedName, UUID> {
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
        val status = settings.getString(PARTICIPATION_STATUS, ParticipationStatus.UNKNOWN.name) ?:ParticipationStatus.UNKNOWN.name

        return ParticipationStatus.valueOf(status)
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
fun setCrashlyticsState(orgId: UUID, studyId: UUID, deviceId: String) {
    val crashlytics = FirebaseCrashlytics.getInstance()

    crashlytics.setCustomKey(DEVICE_ID, deviceId)
    crashlytics.setCustomKey(STUDY_ID, studyId.toString())
    if (orgId != INVALID_ORG_ID) {
        crashlytics.setCustomKey(ORGANIZATION_ID, orgId.toString())
    }
}
