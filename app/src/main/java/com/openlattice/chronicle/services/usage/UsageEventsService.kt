package com.openlattice.chronicle.services.usage

import android.app.IntentService
import android.app.Notification
import android.app.Notification.PRIORITY_LOW
import android.app.NotificationChannel
import android.app.PendingIntent
import androidx.room.Room
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.util.Log
import android.view.Display
import com.google.common.base.Stopwatch
import com.google.common.collect.ImmutableMap
import com.openlattice.chronicle.ChronicleApi
import com.openlattice.chronicle.sensors.ChronicleSensor
import com.openlattice.chronicle.sensors.PROPERTY_TYPES
import com.openlattice.chronicle.sensors.UsageEventsChronicleSensor
import com.openlattice.chronicle.sensors.UsageStatsChronicleSensor
import com.openlattice.chronicle.serialization.JsonSerializer.serializeQueueEntry
import com.openlattice.chronicle.services.upload.PRODUCTION
import com.openlattice.chronicle.services.upload.createRetrofitAdapter
import com.openlattice.chronicle.storage.ChronicleDb
import com.openlattice.chronicle.storage.QueueEntry
import com.openlattice.chronicle.storage.StorageQueue
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


const val USAGE_PERIOD_MILLIS = 15 * 60 * 1000L //This is how long the service will run in the background before re-scheduling itself
const val POLL_INTERVAL = 5 * 1000L  //This is how long frequently the service will poll.
const val ONGOING_NOTIFICATION_ID = 0
const val USAGE_EVENTS_MONITORING_SERVICE = "usageEventsMonitoringService"

class UsageEventsService : IntentService(USAGE_EVENTS_MONITORING_SERVICE) {

    private val executor = Executors.newSingleThreadExecutor()
    private val sw = Stopwatch.createStarted()
    private val handler = Handler()
    private val latch = CountDownLatch(1);
    private val chronicleApi = createRetrofitAdapter(PRODUCTION).create(ChronicleApi::class.java)
    private val rand = Random()
    private val serviceId = rand.nextLong()

    private lateinit var propertyTypeIds: Map<String, UUID>
    private lateinit var chronicleDb: ChronicleDb
    private lateinit var storageQueue: StorageQueue
    private lateinit var sensors: Set<ChronicleSensor>
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val notificationIntent = Intent(this, UsageEventsService::class.java)
        val pendingIntent = PendingIntent.getActivity(this, MONITOR_USAGE_REQUEST, notificationIntent, 0)

        //TODO: Consider using our own notification channel in Android O or later
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, NotificationChannel.DEFAULT_CHANNEL_ID)
        } else {
            Notification.Builder(this)
                    .setPriority(PRIORITY_LOW)
        }
                .setContentTitle("Chronicle Study Title")
                .setContentText("Chronicle context text")
                .setContentIntent(pendingIntent)
                .setTicker("This is ticker text")
                .build()

        startForeground(ONGOING_NOTIFICATION_ID, notification)
        Log.i(javaClass.name, "Usage service is created.")
        return START_STICKY
    }

    override fun onHandleIntent(intent: Intent?) {
        executor.execute {
            propertyTypeIds = getPropertyTypeIds()
            chronicleDb = Room.databaseBuilder(applicationContext, ChronicleDb::class.java, "chronicle").build()
            storageQueue = chronicleDb.queueEntryData()
            sensors = mutableSetOf(
                    UsageStatsChronicleSensor(applicationContext),
                    UsageEventsChronicleSensor(applicationContext)
            )
            Log.i(javaClass.name, "Usage service is initialized")
            latch.countDown()
        }

        latch.await()
        Log.i(javaClass.name, "Usage service is running.")
        doWork()
        while (true)
            Thread.sleep(1000);
    }

    private fun isScreenOff(): Boolean {
        val dm = applicationContext.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        for (display in dm.displays) {
            if (display.state == Display.STATE_ON) {
                return false
            }
        }
        return true
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(javaClass.name, "Destroy requested after ${sw.elapsed(TimeUnit.SECONDS)}")
        executor.shutdown()
        executor.awaitTermination(1, TimeUnit.MINUTES)
        chronicleDb.close()
        Log.i(javaClass.name, "Usage collection gracefully shutdown.")
    }

    private fun doWork() {
        if (isScreenOff()) {
            return
        }
        if (propertyTypeIds == null) {
            propertyTypeIds = getPropertyTypeIds()
            if (propertyTypeIds == null) {
                return
            }
        }

        Log.d(javaClass.name, "Collecting Usage Information. Service ${serviceId} has been running for ${sw.elapsed(TimeUnit.SECONDS)} seconds.")

        //Since this is running on the main thread we shouldn't have worry that shutdown will be called
        if (!executor.isShutdown) {
            executor.execute {
                Log.i(javaClass.name, "Starting usage information collection. ")
                val w = Stopwatch.createStarted()
                sensors
                        .map { it.poll(propertyTypeIds) }
                        .filter { it.isNotEmpty() }
                        .forEach {
                            storageQueue.insertEntry(QueueEntry(System.currentTimeMillis(), rand.nextLong(), serializeQueueEntry(it)))
                        }
                Log.d(javaClass.name, "Persisting usage information took ${w.elapsed(TimeUnit.MILLISECONDS)} millis.")
                handler.postDelayed(this::doWork, POLL_INTERVAL)
            }
        }
    }

    private fun getPropertyTypeIds(): Map<String, UUID> {
        return chronicleApi.getPropertyTypeIds(PROPERTY_TYPES) ?: ImmutableMap.of()
    }
}

const val MONITOR_USAGE_REQUEST = 0;
fun scheduleUsageMonitoringService(context: Context) {
    context.startService(Intent(context, UsageEventsService::class.java))
}
