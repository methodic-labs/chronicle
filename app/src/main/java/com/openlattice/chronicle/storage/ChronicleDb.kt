package com.openlattice.chronicle.storage

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [QueueEntry::class, UserQueueEntry::class],
    version = 2,
    autoMigrations = [
        AutoMigration (from = 1, to = 2)
    ]
)
abstract class ChronicleDb : RoomDatabase() {
    abstract fun queueEntryData(): StorageQueue
    abstract fun userQueueEntryData(): UserStorageQueue
}
