package com.openlattice.chronicle.services.sinks

import com.google.common.collect.SetMultimap
import java.util.*


class BrokerDataSink(private val dataSinks: MutableSet<DataSink>) : DataSink {
    constructor() : this(HashSet())

    fun register(dataSink: DataSink) {
        dataSinks.add(dataSink)
    }

    override fun submit(data: List<SetMultimap<UUID, Any>>) {
        dataSinks.forEach { it.submit(data) }
    }
}