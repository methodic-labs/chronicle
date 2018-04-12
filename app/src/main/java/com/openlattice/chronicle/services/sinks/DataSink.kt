package com.openlattice.chronicle.services.sinks

import com.google.common.collect.SetMultimap

interface DataSink {
    fun submit( data : SetMultimap<String, Object>)
}