package com.openlattice.chronicle.sensors

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSetMultimap
import com.google.common.collect.SetMultimap
import com.openlattice.chronicle.R
import com.openlattice.chronicle.preferences.EnrollmentSettings
import com.openlattice.chronicle.utils.Utils.getAppFullName
import org.apache.olingo.commons.api.edm.FullQualifiedName
import org.joda.time.DateTime
import java.util.*
import kotlin.collections.ArrayList

const val USAGE_EVENTS_POLL_INTERVAL = 15 * 60 * 1000L
const val LAST_USAGE_QUERY_TIMESTAMP = "com.openlattice.sensors.LastUsageQueryTimestamp"

/**
 * A sensor that collect information about UsageEvents for uploading to Chronicle.
 */
class UsageEventsChronicleSensor(context: Context) : ChronicleSensor {
    private val settings = PreferenceManager.getDefaultSharedPreferences(context)
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    private val appContext = context

    @Synchronized
    override fun poll(propertyTypeIds: Map<FullQualifiedName, UUID>): List<SetMultimap<UUID, Any>> {
        if (propertyTypeIds.isEmpty()) {
            Log.w(UsageEventsChronicleSensor::class.java.name, "Property type ids is empty!")
            return ImmutableList.of()
        }

        // get current user
        val enrollmentSettings = EnrollmentSettings(appContext)
        val currentUser = enrollmentSettings.getCurrentUser()
        val previousUser = enrollmentSettings.getPreviousUser()
        val currentUserTimestamp = enrollmentSettings.getCurrentUserTimestamp()

        val usageEventsList: MutableList<UsageEvents.Event> = ArrayList()

        val previousPollTimestamp = settings.getLong(LAST_USAGE_QUERY_TIMESTAMP, System.currentTimeMillis() - USAGE_EVENTS_POLL_INTERVAL)
        val currentPollTimestamp = System.currentTimeMillis()

        val usageEvents = usageStatsManager.queryEvents(previousPollTimestamp, currentPollTimestamp)
        settings.edit().putLong(LAST_USAGE_QUERY_TIMESTAMP, currentPollTimestamp).apply()

        while (usageEvents.hasNextEvent()) {
            val event: UsageEvents.Event = UsageEvents.Event()
            usageEvents.getNextEvent(event)
            usageEventsList.add(event)
        }
        Log.i(javaClass.name, "Collected ${usageEventsList.size} usage events.")
        val timezone = TimeZone.getDefault().id
        return usageEventsList
                .map {
                    ImmutableSetMultimap.builder<UUID, Any>()
                            .put(propertyTypeIds[ID]!!, UUID.randomUUID())
                            .put(propertyTypeIds[GENERAL_NAME]!!, it.packageName)
                            .put(propertyTypeIds[IMPORTANCE]!!, mapImportance(it.eventType))
                            .put(propertyTypeIds[TIMESTAMP]!!, DateTime(it.timeStamp).toString())
                            .put(propertyTypeIds[TIMEZONE]!!, timezone)
                            .put(propertyTypeIds[USER]!!, getTargetUser(currentUser, previousUser, it.timeStamp, currentUserTimestamp))
                            .put(propertyTypeIds[APP_NAME]!!, getAppFullName(appContext, it.packageName))
                            .build()

                }
    }

    // returns device user corresponding to usage event
    // if the event occurred before the current user was saved, we return previously saved user
    private fun getTargetUser(currentUser: String?, previousUser: String?, eventTimestamp: Long, currentUserTimestamp: Long): String {
        var user = if (eventTimestamp >= currentUserTimestamp) currentUser else previousUser
        if (user == null || user == appContext.getString(R.string.user_unassigned)) {
            user = ""
        }

        return user
    }

    private fun mapImportance(importance: Int): String {
        return when (importance) {
            UsageEvents.Event.MOVE_TO_BACKGROUND -> "Move to Background"
            UsageEvents.Event.MOVE_TO_FOREGROUND -> "Move to Foreground"
            UsageEvents.Event.CONFIGURATION_CHANGE -> "Configuration Change"
            UsageEvents.Event.SHORTCUT_INVOCATION -> "Shortcut Invocation"
            UsageEvents.Event.USER_INTERACTION -> "User Interaction"
            UsageEvents.Event.NONE -> "None"
            else -> {
                return "Unknown importance: " + importance.toString()
            }
        }
    }

}