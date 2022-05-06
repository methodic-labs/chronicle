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
interface UserStorageQueue {
    @Query("SELECT * FROM userQueue WHERE writeTimestamp > :limitTimestamp ORDER BY writeTimestamp DESC LIMIT 1")
    fun getNextEntries(limitTimestamp: Long) : List<UserQueueEntry>

    @Insert
    fun insertEntry( entry: UserQueueEntry)

    @Insert
    fun insertEntries( entries: List<UserQueueEntry> )

    @Delete
    fun deleteEntry( entry : UserQueueEntry)

    @Delete
    fun deleteEntries( entries : List<UserQueueEntry> )
}