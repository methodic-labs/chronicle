package com.openlattice.chronicle.services.sinks

import com.google.common.collect.SetMultimap
import com.openlattice.chronicle.study.StudyApi
import java.util.*

class OpenLatticeSink(
    private var studyId: UUID,
    private var participantId: String,
    private var deviceId: String,
    private var studyApi: StudyApi
) : DataSink {
    private lateinit var orgId: UUID

    override fun submit(data: List<SetMultimap<UUID, Any>>): Map<String, Boolean> {
        return mapOf(
            OpenLatticeSink::class.java.name to
                    ((studyApi.uploadAndroidUsageEventData(studyId, participantId, deviceId, data)
                        ?: 0) > 0)
        )
    }
}