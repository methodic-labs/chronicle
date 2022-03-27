package com.openlattice.chronicle.services.sinks

import android.util.Log
import com.google.common.collect.SetMultimap
import com.openlattice.chronicle.study.StudyApi
import java.util.*

class OpenLatticeSink(
    private var studyId: UUID,
    private var participantId: String,
    private var deviceId: String,
    private var studyApi: StudyApi
) : DataSink {

    override fun submit(data: List<SetMultimap<UUID, Any>>): Map<String, Boolean> {
        print("submitting stuff")
        val written = try {
            studyApi.uploadAndroidUsageEventData( studyId, participantId, deviceId, data)
        } catch (e: Exception) {
            Log.i(javaClass.name, "Exception when uploading data", e)
            0
        }
        return mapOf(
            OpenLatticeSink::class.java.name to (written > 0)
        )
    }
}