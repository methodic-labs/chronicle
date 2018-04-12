package com.openlattice.chronicle.storage

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.Index
import android.arch.persistence.room.PrimaryKey
import java.sql.Timestamp
import java.util.*

@Entity(tableName = "dataQueue",
        primaryKeys = ["writeTimestamp", "id"],
        indices = [Index("writeTimestamp"), Index(value = ["writeTimestamp", "id"], unique = true)])
class QueueEntry(
        val writeTimestamp: Long,
        val id: Long,
        @ColumnInfo(name = "data") val data: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as QueueEntry

        if (writeTimestamp != other.writeTimestamp) return false
        if (id != other.id) return false
        if (!Arrays.equals(data, other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = writeTimestamp?.hashCode() ?: 0
        result = 31 * result + (id?.hashCode() ?: 0)
        result = 31 * result + Arrays.hashCode(data)
        return result
    }
}