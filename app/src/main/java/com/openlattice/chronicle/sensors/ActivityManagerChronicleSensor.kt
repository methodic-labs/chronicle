package com.openlattice.chronicle.sensors

import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo.*
import android.content.Context
import com.google.common.collect.HashMultimap
import com.google.common.collect.ImmutableSetMultimap
import com.google.common.collect.SetMultimap
import org.joda.time.DateTime
import java.util.*
import kotlin.collections.ArrayList


class ActivityManagerChronicleSensor(val context: Context) : ChronicleSensor {
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    override fun poll(propertyTypeIds: Map<String, UUID>): List<SetMultimap<UUID, Object>> {
        return activityManager.runningAppProcesses.map {
            ImmutableSetMultimap.of(
                    propertyTypeIds[ID]!!, UUID.randomUUID() as Object,
                    propertyTypeIds[NAME]!!, it.processName as Object,
                    propertyTypeIds[IMPORTANCE]!!, mapImportance(it.importance) as Object,
                    propertyTypeIds[TIMESTAMP]!!, DateTime().toString() as Object)
        }
    }

    fun mapImportance(importance: Int): String {
        return when (importance) {
            IMPORTANCE_FOREGROUND -> "Foreground UI"
            IMPORTANCE_FOREGROUND_SERVICE -> "Foreground Service"
            IMPORTANCE_PERCEPTIBLE -> "Perceptible"
            IMPORTANCE_PERCEPTIBLE_PRE_26 -> "Perceptible Pre-O"
            IMPORTANCE_TOP_SLEEPING -> "Foreground UI - Sleeping"
            IMPORTANCE_VISIBLE -> "Visible - Not In Foreground"
            else -> {
                return "Unknown importance: " + importance.toString()
            }
        }
    }


}