package com.openlattice.chronicle.sensors

import com.google.common.collect.SetMultimap
import com.openlattice.chronicle.android.ChronicleData
import com.openlattice.chronicle.android.ChronicleSample
import com.openlattice.chronicle.storage.UserStorageQueue
import org.apache.olingo.commons.api.edm.FullQualifiedName
import java.util.*

val IMPORTANCE = FullQualifiedName("ol.recordtype")
val GENERAL_NAME = FullQualifiedName("general.fullname")
val APP_NAME = FullQualifiedName("ol.title")
val TIMESTAMP = FullQualifiedName("ol.datelogged")
val DURATION = FullQualifiedName("general.Duration")
val START_TIME = FullQualifiedName("ol.datetimestart")
val END_TIME = FullQualifiedName("general.EndTime")
val ALTITUDE = FullQualifiedName("location.altitude")
val LONGITUDE = FullQualifiedName("location.longitude")
val LATITUDE = FullQualifiedName("location.latitude")
val ID = FullQualifiedName("general.stringid")
val TIMEZONE = FullQualifiedName("ol.timezone")
val RECURRENCE_RULE = FullQualifiedName("ol.rrule")
val NAME = FullQualifiedName("ol.name")
val ACTIVE = FullQualifiedName("ol.active")
val USER = FullQualifiedName("ol.user")

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
    NAME,
    USER
)

interface ChronicleSensor {
    fun poll(currentPollTimestamp: Long, users: NavigableMap<Long, String>): ChronicleData
}
