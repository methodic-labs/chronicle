package com.openlattice.chronicle.services.sinks

import com.google.common.collect.SetMultimap
import com.openlattice.chronicle.api.ChronicleApi
import com.openlattice.chronicle.preferences.INVALID_ORG_ID
import java.util.*

class OpenLatticeSink : DataSink {
    private var studyId: UUID
    private var participantId: String
    private var deviceId: String
    private lateinit var chronicleApi: ChronicleApi
    private lateinit var legacyChronicleApi: com.openlattice.chronicle.ChronicleApi
    private lateinit var orgId: UUID

    override fun submit(data: List<SetMultimap<UUID, Any>>): Map<String, Boolean> {

        if (this::orgId.isInitialized) {
            return mapOf(OpenLatticeSink::class.java.name to
                    ((chronicleApi.upload(orgId, studyId, participantId, deviceId, data) ?: 0) > 0))
        }

        // support apps installed for legacy studies
        return mapOf(OpenLatticeSink::class.java.name to
                ((legacyChronicleApi.upload(studyId, participantId, deviceId, data) ?: 0) > 0))
    }

    constructor(orgId: UUID, studyId: UUID, participantId: String, deviceId: String, chronicleApi: ChronicleApi) : super() {
        this.orgId = orgId
        this.studyId = studyId
        this.participantId = participantId
        this.deviceId = deviceId
        this.chronicleApi = chronicleApi
    }

    constructor(studyId: UUID, participantId: String, deviceId: String, legacyChronicleApi: com.openlattice.chronicle.ChronicleApi) {
        this.studyId = studyId
        this.participantId = participantId
        this.deviceId = deviceId
        this.legacyChronicleApi = legacyChronicleApi
    }
}