package com.openlattice.chronicle.utils

import android.content.Context
import com.openlattice.chronicle.ChronicleStudyApi
import com.openlattice.chronicle.api.ChronicleApi
import com.openlattice.chronicle.data.ParticipationStatus
import com.openlattice.chronicle.preferences.EnrollmentSettings
import com.openlattice.chronicle.preferences.INVALID_ORG_ID
import com.openlattice.chronicle.services.upload.PRODUCTION
import org.apache.olingo.commons.api.edm.FullQualifiedName
import java.util.*

class ApiClient(context: Context) {

    private val chronicleApi =
        Utils.createRetrofitAdapter(PRODUCTION).create(ChronicleApi::class.java)
    private val legacyChronicleStudyApi = Utils.createRetrofitAdapter(PRODUCTION)
        .create(ChronicleStudyApi::class.java) // legacy studies

    private val settings = EnrollmentSettings(context)

    private val studyId = settings.getStudyId()
    private val organizationId = settings.getOrganizationId()
    private val participantId = settings.getParticipantId()

    fun getParticipationStatus(): ParticipationStatus {
        return when (organizationId) {
            INVALID_ORG_ID -> legacyChronicleStudyApi.getParticipationStatus(studyId, participantId)
                ?: ParticipationStatus.UNKNOWN
            else -> {
                chronicleApi.getParticipationStatus(organizationId, studyId, participantId)
                    ?: ParticipationStatus.UNKNOWN
            }
        }
    }

    fun isNotificationsEnabled(): Boolean {
        return when (organizationId) {
            INVALID_ORG_ID -> legacyChronicleStudyApi.isNotificationsEnabled(studyId) ?: false
            else -> chronicleApi.isNotificationsEnabled(organizationId, studyId) ?: false
        }
    }

    fun getStudyQuestionnaires(): Map<UUID, Map<FullQualifiedName, Set<Any>>> {
        return when (organizationId) {
            INVALID_ORG_ID -> legacyChronicleStudyApi.getStudyQuestionnaires(studyId) ?: mapOf()
            else -> chronicleApi.getStudyQuestionnaires(organizationId, studyId) ?: mapOf()
        }
    }
}