package com.openlattice.chronicle.serialization

import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import com.openlattice.chronicle.android.ChronicleSample
import com.openlattice.chronicle.sources.SourceDevice

object JacksonPolymorphism {

    @JsonTypeInfo(
        use = JsonTypeInfo.Id.CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@class"
    )
    abstract class ChronicleSampleMixin

    @JsonTypeInfo(
        use = JsonTypeInfo.Id.CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@class"
    )
    abstract class SourceDeviceMixin

    @JvmStatic
    fun configure(mapper: ObjectMapper) {
        mapper.addMixIn(ChronicleSample::class.java, ChronicleSampleMixin::class.java)
        mapper.addMixIn(SourceDevice::class.java, SourceDeviceMixin::class.java)
    }
}
