package com.openlattice.chronicle.preferences

import android.content.Context
import android.os.Build
import android.preference.PreferenceManager
import android.provider.Settings
import com.google.common.base.Optional
import com.google.common.collect.ImmutableMap
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.openlattice.chronicle.R
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

class EnrollmentSettings(private val context: Context) {
    private val settings = PreferenceManager.getDefaultSharedPreferences(context)

    private var participantId: String
    private var studyId: UUID

    init {
        val studyIdString = settings.getString(STUDY_ID, "") ?: ""
        participantId = settings.getString(PARTICIPANT_ID, "") ?: ""
        studyId = if (Utils.isValidUUID(studyIdString)) UUID.fromString(studyIdString) else INVALID_STUDY_ID

        if (isEnrolled()) {
            setCrashlyticsUser(studyId, participantId, getDeviceId(context))
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


    fun setTargetUser(user: String) {
        val previousUser = getCurrentUser()
        settings
            .edit()
            .putString(context.getString(R.string.previous_user), previousUser)
            .apply()
        settings
            .edit()
            .putLong(context.getString(R.string.current_user_timestamp), System.currentTimeMillis())
            .apply()
        settings
            .edit()
            .putString(context.getString(R.string.current_user), user)
            .apply()
    }

    fun getCurrentUser(): String? {
        return settings.getString(context.getString(R.string.current_user), context.getString(R.string.user_unassigned))
    }

    fun getCurrentUserTimestamp(): Long {
        return settings.getLong(context.getString(R.string.current_user_timestamp), 0)
    }

    fun getPreviousUser(): String? {
        return settings.getString(context.getString(R.string.previous_user), context.getString(R.string.user_unassigned))
    }

    fun isUserIdentificationEnabled(): Boolean {
        return settings.getBoolean(context.getString(R.string.identify_user), false)
    }

    fun toggleBatteryOptimizationDialog(enable: Boolean) {
        settings
            .edit()
            .putBoolean(context.getString(R.string.disable_battery_optimization_dialog), enable)
            .apply()
    }

    fun isBatteryOptimizationDialogEnabled(): Boolean {
        return settings.getBoolean(context.getString(R.string.disable_battery_optimization_dialog), false)
    }

}

fun getDeviceId(context: Context): String {
    return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
}

fun getDevice(deviceId: String): AndroidDevice {
    return AndroidDevice(deviceId, Build.MODEL, Build.VERSION.CODENAME, Build.BRAND, Build.DISPLAY, Build.VERSION.SDK_INT.toString(), Build.PRODUCT, deviceId, Optional.absent())
}

fun setCrashlyticsUser(studyId: UUID, participantId: String, deviceId: String) {
    val crashlytics = FirebaseCrashlytics.getInstance()

    crashlytics.setUserId(participantId)
    crashlytics.setCustomKey(DEVICE_ID, deviceId)
    crashlytics.setCustomKey(STUDY_ID, studyId.toString())
}
