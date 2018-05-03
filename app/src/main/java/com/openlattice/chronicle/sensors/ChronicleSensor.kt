package com.openlattice.chronicle.sensors

import com.google.common.collect.SetMultimap
import java.util.*

const val IMPORTANCE = "ol.recordtype"
const val NAME = "general.fullname"
const val TIMESTAMP = "ol.datalogged"
const val ALTITUDE = "ol.altitude"
const val LONGITUDE = "ol.longitude"
const val LATITUDE = "ol.latitude"
const val ID = "general.stringid"
val FQNS = mutableSetOf(IMPORTANCE, NAME, TIMESTAMP, ALTITUDE, LONGITUDE, LATITUDE, ID)

interface ChronicleSensor {
    fun poll(propertyTypeIds: Map<String, UUID>): SetMultimap<UUID, Object>
}