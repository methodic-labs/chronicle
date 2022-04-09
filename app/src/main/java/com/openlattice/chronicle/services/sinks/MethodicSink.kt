package com.openlattice.chronicle.services.sinks

import android.util.Log
import com.openlattice.chronicle.android.ChronicleData
import com.openlattice.chronicle.android.ChronicleSample
import com.openlattice.chronicle.study.StudyApi
import java.util.*

class MethodicSink(
    private var studyId: UUID,
    private var participantId: String,
    private var deviceId: String,
    private var studyApi: StudyApi
) : DataSink {
    override fun submit(data: List<ChronicleSample>): Map<String, Boolean> {
        val written = try {
            studyApi.uploadAndroidUsageEventData( studyId, participantId, deviceId, ChronicleData( data) )
        } catch (e: Exception) {
            Log.i(javaClass.name, "Exception when uploading data", e)
            0
        }
        return mapOf(
            MethodicSink::class.java.name to (written > 0)
        )
    }
}