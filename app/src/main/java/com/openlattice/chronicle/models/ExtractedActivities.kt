package com.openlattice.chronicle.models

import com.openlattice.chronicle.android.ChronicleSample
import java.time.OffsetDateTime
import java.util.*

data class ExtractedActivities(
    val process:String,
    val importance: String,
    val timestamp: OffsetDateTime = OffsetDateTime.now(),
    val timezone: String = TimeZone.getDefault().id
) : ChronicleSample