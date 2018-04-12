package com.openlattice.chronicle.services.sinks

import com.google.common.collect.SetMultimap


class BrokerDataSink(private val dataSinks: MutableSet<DataSink>) : DataSink {
    constructor() : this(HashSet())

    fun register(dataSink: DataSink) {
        dataSinks.add(dataSink);
    }

    override fun submit(data: SetMultimap<String, Object>) {
        dataSinks.forEach({ dataSink -> dataSink.submit(data) });
    }
}