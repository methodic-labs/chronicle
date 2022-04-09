package com.openlattice.chronicle.models

import com.openlattice.chronicle.android.ChronicleSample
import java.time.OffsetDateTime
import java.util.*

data class ExtractUsageStat(
    val appPackageName: String,
    val startTime: OffsetDateTime,
    val endTime: OffsetDateTime,
    val lastUsed: OffsetDateTime,
    val timezone: String = TimeZone.getDefault().id,
    val totalTimeInForeground: Long,
    val totalTimeVisible: Long,
    val totalTimeForegroundServiceUsed: Long,
    val applicationLabel: String
) : ChronicleSample
