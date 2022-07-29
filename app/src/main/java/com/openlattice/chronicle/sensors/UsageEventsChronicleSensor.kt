package com.openlattice.chronicle.sensors

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import com.openlattice.chronicle.android.ChronicleData
import com.openlattice.chronicle.models.ExtractedUsageEvent
import com.openlattice.chronicle.utils.Utils.getAppFullName
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

const val USAGE_EVENTS_POLL_INTERVAL = 15 * 60 * 1000L
const val LAST_USAGE_QUERY_TIMESTAMP = "com.openlattice.sensors.LastUsageQueryTimestamp"

/**
 * A sensor that collect information about UsageEvents for uploading to Chronicle.
 */
class UsageEventsChronicleSensor(context: Context) : ChronicleSensor {
    private val settings = PreferenceManager.getDefaultSharedPreferences(context)
    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val appContext = context

    @Synchronized
    override fun poll(
        currentPollTimestamp: Long,
        users: NavigableMap<Long, String>
    ): ChronicleData {
        val usageEventsList: MutableList<UsageEvents.Event> = ArrayList()

        val previousPollTimestamp = settings.getLong(
            LAST_USAGE_QUERY_TIMESTAMP,
            System.currentTimeMillis() - USAGE_EVENTS_POLL_INTERVAL
        )

        val usageEvents = usageStatsManager.queryEvents(previousPollTimestamp, currentPollTimestamp)
        settings.edit().putLong(LAST_USAGE_QUERY_TIMESTAMP, currentPollTimestamp).apply()

        while (usageEvents.hasNextEvent()) {
            val event: UsageEvents.Event = UsageEvents.Event()
            usageEvents.getNextEvent(event)
            usageEventsList.add(event)
        }
        Log.i(javaClass.name, "Collected ${usageEventsList.size} usage events.")
        val timezone = TimeZone.getDefault().id

        return ChronicleData(usageEventsList.map {
            ExtractedUsageEvent(
                appPackageName = it.packageName,
                interactionType = mapImportance(it.eventType),
                timestamp = OffsetDateTime.ofInstant(
                    Instant.ofEpochMilli(it.timeStamp),
                    ZoneOffset.UTC
                ),
                eventType = it.eventType,
                timezone = timezone,
                applicationLabel = getAppFullName(appContext, it.packageName),
                user = getTargetUser(it.timeStamp, users),
            )
        })
    }

    private fun getTargetUser(
        eventTimestamp: Long,
        users: NavigableMap<Long, String>
    ): String {
        //If users is empty, you'll get a null and return ""
        val user = users.lowerEntry(eventTimestamp)?.value
        return if (user == null || user == "Not set") {
            ""
        } else user
    }

    private fun mapImportance(importance: Int): String {
        return when (importance) {
            UsageEvents.Event.MOVE_TO_BACKGROUND -> "Move to Background"
            UsageEvents.Event.MOVE_TO_FOREGROUND -> "Move to Foreground"
            UsageEvents.Event.ACTIVITY_PAUSED -> "Activity Paused"
            UsageEvents.Event.ACTIVITY_RESUMED -> "Activity Resumed"
            UsageEvents.Event.CONFIGURATION_CHANGE -> "Configuration Change"
            UsageEvents.Event.SHORTCUT_INVOCATION -> "Shortcut Invocation"
            UsageEvents.Event.USER_INTERACTION -> "User Interaction"
            UsageEvents.Event.NONE -> "None"
            else -> {
                return "Unknown importance: $importance"
            }
        }
    }

}