package com.openlattice.chronicle.models

import com.openlattice.chronicle.android.ChronicleSample
import java.time.OffsetDateTime
import java.util.*

data class ExtractedUsageEvent(
    val appPackageName: String,
    val interactionType: String,
    val eventType: Int,
    val timestamp: OffsetDateTime,
    val timezone: String,
    val user: String,
    val applicationLabel: String,
) : ChronicleSample