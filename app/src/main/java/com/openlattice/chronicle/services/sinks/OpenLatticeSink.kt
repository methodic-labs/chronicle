package com.openlattice.chronicle.services.sinks

import com.google.common.collect.SetMultimap
import com.openlattice.chronicle.ChronicleApi
import java.util.*

class OpenLatticeSink(val studyId: UUID, val participantId: String, val deviceId: String, val chronicleApi: ChronicleApi) : DataSink {
    override fun submit(data: List<SetMultimap<UUID, Any>>) {
        chronicleApi.upload( studyId, participantId, deviceId, data)
    }
}