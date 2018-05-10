package com.openlattice.chronicle.sensors

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.app.usage.UsageStatsManager.INTERVAL_BEST
import android.content.Context
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

class UsageStatsChronicleSensor(val context: Context) : ChronicleSensor {
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private var previousPollTimestamp = System.currentTimeMillis() - 5 * 1000
    private val currentPollTimestamp = System.currentTimeMillis()
    private val lock: Lock = ReentrantLock();

    override fun poll(propertyTypeIds: Map<String, UUID>): List<SetMultimap<UUID, Object>> {
        if (propertyTypeIds.isEmpty()) {
            return ImmutableList.of()
        }

        if (lock.tryLock() && ((currentPollTimestamp - previousPollTimestamp) >= USAGE_STATS_POLL_INTERVAL)) {
            val usageStats: List<UsageStats>

            //critical section is to only allow one thread to query usage stats.
            try {
                usageStats = usageStatsManager
                        .queryUsageStats(INTERVAL_BEST, System.currentTimeMillis() - USAGE_STATS_POLL_INTERVAL, System.currentTimeMillis())
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
                                .put(propertyTypeIds[START_TIME]!!, DateTime(it.firstTimeStamp) as Object)
                                .put(propertyTypeIds[END_TIME]!!, DateTime(it.lastTimeStamp) as Object)
                                .put(propertyTypeIds[DURATION]!!, it.totalTimeInForeground as Object)
                                .put(propertyTypeIds[TIMESTAMP]!!, DateTime(it.lastTimeUsed) as Object)
                                .build()

                    }
        } else {
            return ImmutableList.of()
        }
    }
}

