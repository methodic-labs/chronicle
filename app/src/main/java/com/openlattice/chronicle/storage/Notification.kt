package com.openlattice.chronicle.storage

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import com.openlattice.chronicle.constants.NotificationType

@Entity(tableName = "notifications")
data class Notification (
        @PrimaryKey var id :String,
        @ColumnInfo(name = "notification_type") var type :NotificationType,
        @ColumnInfo(name = "cron_expression") var cronExpression :String,
        @ColumnInfo(name = "title") var title :String,
        @ColumnInfo(name = "sub_title") var subTitle :String
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (javaClass != other?.javaClass) return false

        other as Notification

        if (other.id != id) return false
        if (other.cronExpression != cronExpression) return false
        if (other.id != id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result  + cronExpression.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }

    fun getRequestCode(): Int {
        return hashCode()
    }

    override fun toString(): String {
        return "Notification(id=$id, cron=$cronExpression type=$type"
    }
}