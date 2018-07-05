package com.openlattice.chronicle.sensors

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSetMultimap
import com.google.common.collect.SetMultimap
import org.joda.time.DateTime
import org.joda.time.LocalDateTime
import java.util.*
import kotlin.collections.ArrayList

const val USAGE_EVENTS_POLL_INTERVAL = 15 * 60 * 1000L
const val LAST_USAGE_QUERY_TIMESTAMP = "com.openlattice.sensors.LastUsageQueryTimestamp"

/**
 * A sensor that collect information about UsageEvents for uploading to Chronicle.
 */
class UsageEventsChronicleSensor(context: Context) : ChronicleSensor {
    private val settings = PreferenceManager.getDefaultSharedPreferences(context)
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private var previousPollTimestamp = settings.getLong(LAST_USAGE_QUERY_TIMESTAMP, System.currentTimeMillis() - USAGE_EVENTS_POLL_INTERVAL)

    @Synchronized
    override fun poll(propertyTypeIds: Map<String, UUID>): List<SetMultimap<UUID, Object>> {
        if (propertyTypeIds.isEmpty()) {
            return ImmutableList.of()
        }
        val currentPollTimestamp = System.currentTimeMillis()
        val usageEventsList: MutableList<UsageEvents.Event> = ArrayList()

        if ((currentPollTimestamp - previousPollTimestamp) < USAGE_EVENTS_POLL_INTERVAL) {
            return ImmutableList.of()
        }

        val usageEvents = usageStatsManager.queryEvents(previousPollTimestamp, currentPollTimestamp)
        settings.edit().putLong(LAST_USAGE_QUERY_TIMESTAMP, currentPollTimestamp).apply()
        previousPollTimestamp = currentPollTimestamp

        while (usageEvents.hasNextEvent()) {
            val event: UsageEvents.Event = UsageEvents.Event()
            usageEvents.getNextEvent(event)
            usageEventsList.add(event)
        }

        Log.i(javaClass.name, "Collected ${usageEventsList.size} usage events.")

        return usageEventsList
                .map {
                    ImmutableSetMultimap.of(
                            propertyTypeIds[ID]!!, UUID.randomUUID() as Object,
                            propertyTypeIds[NAME]!!, it.packageName as Object,
                            propertyTypeIds[IMPORTANCE]!!, mapImportance(it.eventType) as Object,
                            propertyTypeIds[TIMESTAMP]!!, DateTime(it.timeStamp) as Object)
                }
    }

    private fun mapImportance(importance: Int): String {
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