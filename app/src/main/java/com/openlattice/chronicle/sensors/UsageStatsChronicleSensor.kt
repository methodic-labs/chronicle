package com.openlattice.chronicle.sensors

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.app.usage.UsageStatsManager.INTERVAL_BEST
import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSetMultimap
import com.google.common.collect.SetMultimap
import org.joda.time.DateMidnight
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

const val USAGE_STATS_POLL_INTERVAL = 15 * 60 * 1000L
const val LAST_USAGE_STATS_TIMESTAMP = "com.openlattice.sensors.LastUsageStatsTimestamp"

class UsageStatsChronicleSensor(val context: Context) : ChronicleSensor {
    private val settings = PreferenceManager.getDefaultSharedPreferences(context)
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    @Synchronized
    override fun poll(propertyTypeIds: Map<String, UUID>): List<SetMultimap<UUID, Object>> {
        if (propertyTypeIds.isEmpty()) {
            return ImmutableList.of()
        }


        val usageStats = usageStatsManager.queryUsageStats(INTERVAL_BEST, DateMidnight.now().millis, System.currentTimeMillis())

        Log.i(javaClass.name, "Collected ${usageStats.size} stats.")

        return usageStats
                .map {
                    ImmutableSetMultimap.Builder<UUID, Object>()
                            .put(propertyTypeIds[ID]!!, UUID.randomUUID() as Object)
                            .put(propertyTypeIds[NAME]!!, it.packageName as Object)
                            .put(propertyTypeIds[IMPORTANCE]!!, "Usage Stat" as Object)
                            .put(propertyTypeIds[START_TIME]!!, DateTime(it.firstTimeStamp).toString() as Object)
                            .put(propertyTypeIds[END_TIME]!!, DateTime(it.lastTimeStamp).toString() as Object)
                            .put(propertyTypeIds[DURATION]!!, it.totalTimeInForeground as Object)
                            .put(propertyTypeIds[TIMESTAMP]!!, DateTime(it.lastTimeUsed).toString() as Object)
                            .build()

                }
    }
}

