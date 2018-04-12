package com.openlattice.chronicle.storage

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase

@Database(entities = [QueueEntry::class], version = 1)
abstract class ChronicleDb : RoomDatabase() {
    abstract fun queueEntryData(): StorageQueue
}