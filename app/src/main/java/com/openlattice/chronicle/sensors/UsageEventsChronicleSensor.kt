package com.openlattice.chronicle.sensors

import android.app.ActivityManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSetMultimap
import com.google.common.collect.SetMultimap
import org.joda.time.LocalDateTime
import java.util.*
import kotlin.collections.ArrayList

const val USAGE_EVENTS_POLL_INTERVAL = 10 * 1000L

/**
 * A sensor that collect information about UsageEvents for uploading to Chronicle.
 */
class UsageEventsChronicleSensor(val context: Context) : ChronicleSensor {
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private var previousPollTimestamp = System.currentTimeMillis() - USAGE_EVENTS_POLL_INTERVAL
    override fun poll(propertyTypeIds: Map<String, UUID>): List<SetMultimap<UUID, Object>> {
        if( propertyTypeIds.isEmpty() ) {
            return ImmutableList.of()
        }
        val currentPollTimestamp = System.currentTimeMillis()
        val usageEventsList: MutableList<UsageEvents.Event> = ArrayList()

        if( ( currentPollTimestamp - previousPollTimestamp ) < USAGE_EVENTS_POLL_INTERVAL ) {
            return ImmutableList.of()
        }

        val usageEvents = usageStatsManager.queryEvents(previousPollTimestamp, currentPollTimestamp)

        previousPollTimestamp = currentPollTimestamp

        while (usageEvents.hasNextEvent()) {
            val event: UsageEvents.Event = UsageEvents.Event()
            usageEvents.getNextEvent(event)
            usageEventsList.add(event)
        }

        Log.i(javaClass.name , "Collected ${usageEventsList.size} usage events.")

        return usageEventsList
                .map {
                    ImmutableSetMultimap.of(
                            propertyTypeIds[ID]!!, UUID.randomUUID() as Object,
                            propertyTypeIds[NAME]!!, it.packageName as Object,
                            propertyTypeIds[IMPORTANCE]!!, mapImportance(it.eventType) as Object,
                            propertyTypeIds[TIMESTAMP]!!, LocalDateTime( it.timeStamp ) as Object)
                }
    }

    fun mapImportance(importance: Int): String {
        return when (importance) {
            UsageEvents.Event.MOVE_TO_BACKGROUND -> "Move to Background"
            UsageEvents.Event.MOVE_TO_FOREGROUND -> "Move to Foreground"
            UsageEvents.Event.CONFIGURATION_CHANGE -> "Configuration Change"
            UsageEvents.Event.SHORTCUT_INVOCATION -> "Shortcut Invocation"
            UsageEvents.Event.USER_INTERACTION -> "User Interaction"
            UsageEvents.Event.NONE -> "None"
            else -> {
                return "Unknown importance: " + importance.toString()
            }
        }
    }

}