package com.openlattice.chronicle.services.sinks

import com.openlattice.chronicle.android.ChronicleUsageEvent

interface DataSink {
    fun submit(data: List<ChronicleUsageEvent>): Map<String, Boolean>
}