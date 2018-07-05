package com.openlattice.chronicle.services.sinks

import com.google.common.collect.SetMultimap
import com.openlattice.chronicle.ChronicleApi
import java.util.*

class OpenLatticeSink(private val studyId: UUID, private val participantId: String, private val deviceId: String, private val chronicleApi: ChronicleApi) : DataSink {
    override fun submit(data: List<SetMultimap<UUID, Any>>): Map<String, Boolean> {
        return mapOf(OpenLatticeSink::class.java.name to
                ((chronicleApi.upload(studyId, participantId, deviceId, data) ?: 0) > 0))
    }
}