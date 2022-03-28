package com.openlattice.chronicle.services.sinks

<<<<<<< HEAD
import com.openlattice.chronicle.android.ChronicleDataUpload
=======
import com.openlattice.chronicle.android.ChronicleUsageEvent
>>>>>>> e7c0ed09476a63cbd6b50ec78d74b7462710a223

interface DataSink {
    fun submit(data: List<ChronicleDataUpload>): Map<String, Boolean>
}