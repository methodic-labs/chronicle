package com.openlattice.chronicle.storage

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverter
import android.arch.persistence.room.TypeConverters
import com.openlattice.chronicle.constants.NotificationType

@Database(entities = [QueueEntry::class, Notification::class], version = 1)
@TypeConverters(Converters::class)
abstract class ChronicleDb : RoomDatabase() {
    abstract fun queueEntryData(): StorageQueue

    abstract fun notificationDao(): NotificationDao
}

class Converters {
    @TypeConverter
    fun notificationTypeToString( value: NotificationType) :String {
        return value.name
    }

    @TypeConverter
    fun stringToNotificationType(value :String) :NotificationType {
        return NotificationType.valueOf(value)
    }
}