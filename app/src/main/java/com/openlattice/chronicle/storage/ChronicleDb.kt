package com.openlattice.chronicle.storage

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [QueueEntry::class, UserQueueEntry::class], version = 1)
abstract class ChronicleDb : RoomDatabase() {
    abstract fun queueEntryData(): StorageQueue
}
