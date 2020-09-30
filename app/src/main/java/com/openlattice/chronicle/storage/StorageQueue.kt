package com.openlattice.chronicle.storage

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

/*
 * Since database will return sorted elements, we use a list to preserve order, even though items
 * are technically a set. We could have used LinkedHashSet, but there's no need as we have no
 * plans of performing set operations on returned data.
 */
@Dao
interface StorageQueue {
    @Query("SELECT * FROM dataQueue ORDER BY writeTimestamp ASC LIMIT :size")
    fun getNextEntries( size : Int ) : List<QueueEntry>

    @Insert
    fun insertEntry( entry: QueueEntry)

    @Insert
    fun insertEntries( entries: List<QueueEntry> )

    @Delete
    fun deleteEntry( entry : QueueEntry)

    @Delete
    fun deleteEntries( entries : List<QueueEntry> )
}