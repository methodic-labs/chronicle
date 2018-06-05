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
import org.joda.time.DateTime
import org.joda.time.LocalDateTime
import java.util.*
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

const val USAGE_STATS_POLL_INTERVAL = 60 * 1000L
const val LAST_USAGE_STATS_TIMESTAMP = "com.openlattice.sensors.LastUsageStatsTimestamp"

class UsageStatsChronicleSensor(val context: Context) : ChronicleSensor {
    private val settings = PreferenceManager.getDefaultSharedPreferences(context)
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private var previousPollTimestamp = settings.getLong(LAST_USAGE_STATS_TIMESTAMP, System.currentTimeMillis() - USAGE_EVENTS_POLL_INTERVAL)

    private val lock: Lock = ReentrantLock();

    override fun poll(propertyTypeIds: Map<String, UUID>): List<SetMultimap<UUID, Object>> {
        if (propertyTypeIds.isEmpty()) {
            return ImmutableList.of()
        }

        val currentPollTimestamp = System.currentTimeMillis()

        if (lock.tryLock() && ((currentPollTimestamp - previousPollTimestamp) >= USAGE_STATS_POLL_INTERVAL)) {
            val usageStats: List<UsageStats>

            //critical section is to only allow one thread to query usage stats.
            try {
                usageStats = usageStatsManager
                        .queryUsageStats(INTERVAL_BEST, System.currentTimeMillis() - USAGE_STATS_POLL_INTERVAL, System.currentTimeMillis())
                settings.edit().putLong(LAST_USAGE_STATS_TIMESTAMP, currentPollTimestamp).apply()
                previousPollTimestamp = currentPollTimestamp
            } finally {
                lock.unlock()
            }

            Log.i(javaClass.name, "Collected ${usageStats.size} stats.")

            return usageStats
                    .map {
                        ImmutableSetMultimap.Builder<UUID, Object>()
                                .put(propertyTypeIds[ID]!!, UUID.randomUUID() as Object)
                                .put(propertyTypeIds[NAME]!!, it.packageName as Object)
                                .put(propertyTypeIds[IMPORTANCE]!!, "Usage Stat" as Object)
                                .put(propertyTypeIds[START_TIME]!!, LocalDateTime(it.firstTimeStamp) as Object)
                                .put(propertyTypeIds[END_TIME]!!, LocalDateTime(it.lastTimeStamp) as Object)
                                .put(propertyTypeIds[DURATION]!!, it.totalTimeInForeground as Object)
                                .put(propertyTypeIds[TIMESTAMP]!!, LocalDateTime(it.lastTimeUsed) as Object)
                                .build()

                    }
        } else {
            return ImmutableList.of()
        }
    }
}

