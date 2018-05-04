package com.openlattice.chronicle.sensors

import com.google.common.collect.SetMultimap
import java.util.*

const val IMPORTANCE = "ol.recordtype"
const val NAME = "general.fullname"
const val TIMESTAMP = "ol.datelogged"
const val ALTITUDE = "location.altitude"
const val LONGITUDE = "location.longitude"
const val LATITUDE = "location.latitude"
const val ID = "general.stringid"
val FQNS = setOf(IMPORTANCE, NAME, TIMESTAMP, ALTITUDE, LONGITUDE, LATITUDE, ID)

interface ChronicleSensor {
    fun poll(propertyTypeIds: Map<String, UUID>): List<SetMultimap<UUID, Object>>
}