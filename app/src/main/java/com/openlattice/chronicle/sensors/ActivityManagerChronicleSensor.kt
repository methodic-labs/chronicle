package com.openlattice.chronicle.sensors

import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo.*
import android.content.Context
import com.google.common.collect.HashMultimap
import com.google.common.collect.SetMultimap
import org.joda.time.DateTime
import java.util.*



class ActivityManagerChronicleSensor(val context: Context) : ChronicleSensor {
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    override fun poll(propertyTypeIds: Map<String, UUID>): SetMultimap<UUID, Object> {
        val data: SetMultimap<UUID, Object> = HashMultimap.create()
        activityManager.runningAppProcesses.forEach {
            data.put(propertyTypeIds[ID], UUID.randomUUID() as Object )
            data.put(propertyTypeIds[NAME], it.processName as Object)
            data.put(propertyTypeIds[IMPORTANCE], mapImportance(it.importance) as Object)
            data.put(propertyTypeIds[TIMESTAMP], DateTime().toString() as Object)
        }

        return data
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