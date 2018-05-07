package com.openlattice.chronicle.sensors

import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo.*
import android.app.usage.UsageStatsManager
import android.app.usage.UsageStatsManager.INTERVAL_BEST
import android.content.Context
import com.google.common.collect.HashMultimap
import com.google.common.collect.ImmutableSetMultimap
import com.google.common.collect.SetMultimap
import org.joda.time.DateTime
import java.util.*
import kotlin.collections.ArrayList


class UsageStatsChronicleSensor(val context: Context) : ChronicleSensor {
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    override fun poll(propertyTypeIds: Map<String, UUID>): List<SetMultimap<UUID, Object>> {
        return usageStatsManager
                .queryUsageStats(INTERVAL_BEST, System.currentTimeMillis() - 1000, System.currentTimeMillis())
                .map {
                    ImmutableSetMultimap.of(
                            propertyTypeIds[ID]!!, UUID.randomUUID() as Object,
                            propertyTypeIds[NAME]!!, it.packageName as Object,
                            propertyTypeIds[IMPORTANCE]!!, it.totalTimeInForeground.toString() as Object,
                            propertyTypeIds[TIMESTAMP]!!, DateTime().toString() as Object)
                }
    }

}