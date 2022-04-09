package com.openlattice.chronicle.services.sinks


import com.openlattice.chronicle.android.ChronicleSample


interface DataSink {
    fun submit(data: List<ChronicleSample>): Map<String, Boolean>
}