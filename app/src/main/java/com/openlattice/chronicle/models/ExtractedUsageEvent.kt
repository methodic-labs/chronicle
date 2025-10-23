package com.openlattice.chronicle.models

import com.fasterxml.jackson.annotation.JsonCreator
import com.openlattice.chronicle.android.ChronicleSample
import java.time.OffsetDateTime
import java.util.*

data class ExtractedUsageEvent @JsonCreator constructor(
    val appPackageName: String,
    val interactionType: String,
    val timestamp: OffsetDateTime,
    val timezone: String,
    val user: String,
    val applicationLabel: String,
) : ChronicleSample