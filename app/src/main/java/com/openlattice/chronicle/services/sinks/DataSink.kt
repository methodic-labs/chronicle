package com.openlattice.chronicle.services.sinks


import com.openlattice.chronicle.android.ChronicleDataUpload


interface DataSink {
    fun submit(data: List<ChronicleDataUpload>): Map<String, Boolean>
}