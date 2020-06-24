package com.openlattice.chronicle.sensors

import com.google.common.collect.SetMultimap
import java.util.*

const val IMPORTANCE = "ol.recordtype"
const val GENERAL_NAME = "general.fullname"
const val APP_NAME = "ol.title"
const val TIMESTAMP = "ol.datelogged"
const val DURATION = "general.Duration"
const val START_TIME = "ol.datetimestart"
const val END_TIME = "general.EndTime"
const val ALTITUDE = "location.altitude"
const val LONGITUDE = "location.longitude"
const val LATITUDE = "location.latitude"
const val ID = "general.stringid"
const val TIMEZONE = "ol.timezone"
const val RECURRENCE_RULE = "ol.rrule"
const val NAME = "ol.name"
const val ACTIVE = "ol.active"

val PROPERTY_TYPES = setOf(
        IMPORTANCE,
        GENERAL_NAME,
        APP_NAME,
        TIMESTAMP,
        ALTITUDE,
        LONGITUDE,
        LATITUDE,
        ID,
        DURATION,
        START_TIME,
        END_TIME,
        TIMEZONE,
        ACTIVE,
        RECURRENCE_RULE,
        NAME)

interface ChronicleSensor {
    fun poll(propertyTypeIds: Map<String, UUID>): List<SetMultimap<UUID, Any>>
}
