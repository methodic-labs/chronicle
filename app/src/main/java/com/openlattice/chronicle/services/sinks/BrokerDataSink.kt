package com.openlattice.chronicle.services.sinks

import com.google.common.collect.SetMultimap
<<<<<<< HEAD
import com.openlattice.chronicle.android.ChronicleDataUpload
=======
import com.openlattice.chronicle.android.ChronicleUsageEvent
>>>>>>> e7c0ed09476a63cbd6b50ec78d74b7462710a223
import java.util.*


class BrokerDataSink(private val dataSinks: MutableSet<DataSink>) : DataSink {
    constructor() : this(HashSet())

    fun register(dataSink: DataSink) {
        dataSinks.add(dataSink)
    }

<<<<<<< HEAD
    override fun submit(data: List<ChronicleDataUpload>): Map<String, Boolean> {
=======
    override fun submit(data: List<ChronicleUsageEvent>): Map<String, Boolean> {
>>>>>>> e7c0ed09476a63cbd6b50ec78d74b7462710a223
        return dataSinks.asSequence().flatMap { it.submit(data).asSequence().map { it.key to it.value } }.toMap()
    }
}