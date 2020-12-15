package com.openlattice.chronicle.sensors

import android.app.usage.UsageStatsManager
import android.app.usage.UsageStatsManager.INTERVAL_BEST
import android.content.Context
import android.util.Log
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSetMultimap
import com.google.common.collect.SetMultimap
import com.openlattice.chronicle.utils.Utils.getAppFullName
import org.apache.olingo.commons.api.edm.FullQualifiedName
import org.joda.time.DateMidnight
import org.joda.time.DateTime
import java.util.*


class UsageStatsChronicleSensor(val context: Context) : ChronicleSensor {
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    @Synchronized
    override fun poll(propertyTypeIds: Map<FullQualifiedName, UUID>): List<SetMultimap<UUID, Any>> {
        if (propertyTypeIds.isEmpty()) {
            return ImmutableList.of()
        }


        val usageStats = usageStatsManager.queryUsageStats(INTERVAL_BEST, DateMidnight.now().millis, System.currentTimeMillis())

        Log.i(javaClass.name, "Collected ${usageStats.size} stats.")

        val timezone = TimeZone.getDefault().id
        //If we start seeing serialization oddities revert to doing DateTime.toString() here
        return usageStats
                .map {
                    ImmutableSetMultimap.Builder<UUID, Any>()
                            .put(propertyTypeIds[ID]!!, UUID.randomUUID())
                            .put(propertyTypeIds[GENERAL_NAME]!!, it.packageName)
                            .put(propertyTypeIds[IMPORTANCE]!!, "Usage Stat")
                            .put(propertyTypeIds[START_TIME]!!, DateTime(it.firstTimeStamp).toString())
                            .put(propertyTypeIds[END_TIME]!!, DateTime(it.lastTimeStamp).toString())
                            .put(propertyTypeIds[DURATION]!!, it.totalTimeInForeground)
                            .put(propertyTypeIds[TIMESTAMP]!!, DateTime(it.lastTimeUsed).toString())
                            .put(propertyTypeIds[TIMEZONE]!!, timezone)
                            .put(propertyTypeIds[APP_NAME]!!, getAppFullName(context, it.packageName))
                            .build()

                }
    }
}

