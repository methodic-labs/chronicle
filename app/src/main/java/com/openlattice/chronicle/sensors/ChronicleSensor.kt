package com.openlattice.chronicle.sensors

import com.google.common.collect.SetMultimap
import java.util.*

const val IMPORTANCE = ""
const val NAME = ""
const val TIMESTAMP = ""
val FQNS = mutableSetOf( IMPORTANCE, NAME, TIMESTAMP )

interface ChronicleSensor {
    fun poll( propertyTypeIds : Map<String, UUID> ) : SetMultimap<UUID, Object>
}