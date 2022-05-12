package com.openlattice.chronicle.storage

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import java.util.*

@Entity(
    tableName = "userQueue",
    primaryKeys = ["writeTimestamp"],
    indices = [Index("writeTimestamp", unique = true)]
)
data class UserQueueEntry(
    val writeTimestamp: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "user") val user: String
)