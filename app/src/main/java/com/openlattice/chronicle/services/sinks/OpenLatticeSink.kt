package com.openlattice.chronicle.services.sinks

import com.google.common.collect.SetMultimap
import com.openlattice.chronicle.storage.StorageQueue

class OpenLatticeSink( queue: StorageQueue) : DataSink {
    override fun submit(data: SetMultimap<String, Object>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        //Here is where we push data into OpenLattice.
    }
}