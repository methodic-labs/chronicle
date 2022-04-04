package com.openlattice.chronicle.services.sinks

import com.openlattice.chronicle.android.ChronicleDataUpload


class BrokerDataSink(private val dataSinks: MutableSet<DataSink>) : DataSink {
    constructor() : this(HashSet())

    fun register(dataSink: DataSink) {
        dataSinks.add(dataSink)
    }

    override fun submit(data: List<ChronicleDataUpload>): Map<String, Boolean> {
        return dataSinks.asSequence()
            .flatMap { it.submit(data).asSequence() }
            .associate { it.key to it.value }
    }
}