package com.openlattice.chronicle.sensors

import android.app.usage.UsageStatsManager
import android.app.usage.UsageStatsManager.INTERVAL_BEST
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.common.collect.ImmutableList
import com.openlattice.chronicle.android.ChronicleDataUpload
import com.openlattice.chronicle.models.ExtractUsageStat
import com.openlattice.chronicle.utils.Utils.getAppFullName
import com.openlattice.chronicle.utils.Utils.offsetDateTimeFromEpochMillis
import org.apache.olingo.commons.api.edm.FullQualifiedName
import org.joda.time.DateMidnight
import java.util.*


class UsageStatsChronicleSensor(val context: Context) : ChronicleSensor {
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    @Synchronized
    override fun poll(propertyTypeIds: Map<FullQualifiedName, UUID>): List<ChronicleDataUpload> {
        if (propertyTypeIds.isEmpty()) {
            return ImmutableList.of()
        }

        val usageStats = usageStatsManager.queryUsageStats(INTERVAL_BEST, DateMidnight.now().millis, System.currentTimeMillis())

        Log.i(javaClass.name, "Collected ${usageStats.size} stats.")

        val timezone = TimeZone.getDefault().id
        //If we start seeing serialization oddities revert to doing DateTime.toString() here
        return usageStats
                .map {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ExtractUsageStat(
                            it.packageName,
                            offsetDateTimeFromEpochMillis(it.firstTimeStamp),
                            offsetDateTimeFromEpochMillis(it.lastTimeStamp),
                            offsetDateTimeFromEpochMillis(it.lastTimeUsed),
                            totalTimeInForeground = it.totalTimeForegroundServiceUsed,
                            totalTimeVisible = it.totalTimeVisible,
                            totalTimeForegroundServiceUsed = it.totalTimeForegroundServiceUsed,
                            applicationLabel = getAppFullName(context, it.packageName)
                        )
                    } else {
                        ExtractUsageStat(
                            it.packageName,
                            offsetDateTimeFromEpochMillis(it.firstTimeStamp),
                            offsetDateTimeFromEpochMillis(it.lastTimeStamp),
                            offsetDateTimeFromEpochMillis(it.lastTimeUsed),
                            totalTimeInForeground = 0,
                            totalTimeVisible = 0,
                            totalTimeForegroundServiceUsed = 0,
                            applicationLabel = getAppFullName(context, it.packageName)
                        )
                    }
                }
    }
}

