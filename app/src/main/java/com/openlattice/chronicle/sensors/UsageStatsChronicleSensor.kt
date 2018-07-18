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


class UsageStatsChronicleSensor(val context: Context) : ChronicleSensor {
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    @Synchronized
    override fun poll(propertyTypeIds: Map<String, UUID>): List<SetMultimap<UUID, Any>> {
        if (propertyTypeIds.isEmpty()) {
            return ImmutableList.of()
        }


        val usageStats = usageStatsManager.queryUsageStats(INTERVAL_BEST, DateMidnight.now().millis, System.currentTimeMillis())

        Log.i(javaClass.name, "Collected ${usageStats.size} stats.")

        //If we start seeing serialization oddities revert to doing DateTime.toString() here
        return usageStats
                .map {
                    ImmutableSetMultimap.Builder<UUID, Any>()
                            .put(propertyTypeIds[ID]!!, UUID.randomUUID())
                            .put(propertyTypeIds[NAME]!!, it.packageName)
                            .put(propertyTypeIds[IMPORTANCE]!!, "Usage Stat")
                            .put(propertyTypeIds[START_TIME]!!, DateTime(it.firstTimeStamp).toString())
                            .put(propertyTypeIds[END_TIME]!!, DateTime(it.lastTimeStamp).toString())
                            .put(propertyTypeIds[DURATION]!!, it.totalTimeInForeground)
                            .put(propertyTypeIds[TIMESTAMP]!!, DateTime(it.lastTimeUsed).toString())
                            .build()

                }
    }
}

