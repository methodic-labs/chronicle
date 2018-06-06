package com.openlattice.chronicle.sensors

import com.google.common.collect.SetMultimap
import org.joda.time.LocalDateTime
import java.util.*

const val IMPORTANCE = "ol.recordtype"
const val NAME = "general.fullname"
const val TIMESTAMP = "ol.datelogged"
const val DURATION = "general.Duration"
const val START_TIME = "ol.datetimestart"
const val END_TIME = "general.EndTime"
const val ALTITUDE = "location.altitude"
const val LONGITUDE = "location.longitude"
const val LATITUDE = "location.latitude"
const val ID = "general.stringid"
val PROPERTY_TYPES = setOf(
        IMPORTANCE,
        NAME,
        TIMESTAMP,
        ALTITUDE,
        LONGITUDE,
        LATITUDE,
        ID,
        DURATION,
        START_TIME,
        END_TIME)

interface ChronicleSensor {
    fun poll(propertyTypeIds: Map<String, UUID>): List<SetMultimap<UUID, Object>>
}
