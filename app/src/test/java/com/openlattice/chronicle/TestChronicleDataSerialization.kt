package com.openlattice.chronicle

import android.util.Log
import android.util.Log.INFO
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.guava.GuavaModule
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.openlattice.chronicle.models.ExtractedUsageEvent
import org.apache.commons.lang3.RandomStringUtils
import org.junit.Test
import java.time.OffsetDateTime
import java.util.*
import com.openlattice.chronicle.android.ChronicleData
import org.junit.Assert
import org.slf4j.LoggerFactory


class TestChronicleDataSerialization {
    companion object {
        val mapper = ObjectMapper()
        val logger = LoggerFactory.getLogger(TestChronicleDataSerialization::class.java)
        init {
            mapper.registerModule(GuavaModule())
            mapper.registerModule(JodaModule())
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
    }
}