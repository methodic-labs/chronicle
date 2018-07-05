package com.openlattice.chronicle.services.sinks

import com.google.common.collect.SetMultimap
import java.util.*

interface DataSink {
    fun submit(data: List<SetMultimap<UUID, Any>>) : Map<String, Boolean>
}