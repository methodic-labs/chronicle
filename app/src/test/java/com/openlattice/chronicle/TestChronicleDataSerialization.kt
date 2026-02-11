package com.openlattice.chronicle

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.guava.GuavaModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.openlattice.chronicle.android.ChronicleData
import com.openlattice.chronicle.models.ExtractedUsageEvent
import com.openlattice.chronicle.serialization.JsonSerializer
import com.openlattice.chronicle.sources.AndroidDevice
import com.openlattice.chronicle.sources.SourceDevice
import org.apache.commons.lang3.RandomStringUtils
import org.junit.Assert
import org.junit.Test
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.util.TimeZone


class TestChronicleDataSerialization {
    companion object {
        val mapper = ObjectMapper()
        val logger = LoggerFactory.getLogger(TestChronicleDataSerialization::class.java)
        init {
            mapper.registerModule(KotlinModule.Builder().build())
            mapper.registerModule(GuavaModule())
            mapper.registerModule(JavaTimeModule())
            mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        }
    }

    @Test
    fun testChronicleDataSerialization() {
        val count = 10
        val usageEvents = (0 until count).map {
            ExtractedUsageEvent(
                RandomStringUtils.randomAlphanumeric(5),
                RandomStringUtils.randomAlphanumeric(5),
                OffsetDateTime.now(),
                TimeZone.getDefault().id,
                RandomStringUtils.randomAlphanumeric(5),
                RandomStringUtils.randomAlphanumeric(5),
            )
        }

        val json = mapper.writeValueAsString( ChronicleData(usageEvents) )
        println("Json: $json")
        Assert.assertTrue( json.contains ("@class") )

        val deserializedUsageEvents = mapper.readValue<ChronicleData>(json)
        Assert.assertEquals("Deserialized usage events must match." , usageEvents, deserializedUsageEvents )
    }

    @Test
    fun testSourceDeviceSerialization() {
        val device = AndroidDevice(
            RandomStringUtils.randomAlphanumeric(5),
            RandomStringUtils.randomAlphanumeric(5),
            RandomStringUtils.randomAlphanumeric(5),
            RandomStringUtils.randomAlphanumeric(5),
            RandomStringUtils.randomAlphanumeric(5),
            RandomStringUtils.randomAlphanumeric(5),
            RandomStringUtils.randomAlphanumeric(5),
            RandomStringUtils.randomAlphanumeric(5),
            mapOf( RandomStringUtils.randomAlphanumeric(5) to RandomStringUtils.randomAlphanumeric(5))
        )

        val json = mapper.writeValueAsString( device )
        println("Json: $json")
        Assert.assertTrue( json.contains ("@class") )

        val deserializedDevice = mapper.readValue<SourceDevice>(json)
        Assert.assertEquals("Deserialized usage events must match." , device, deserializedDevice )
    }
}