package com.openlattice.chronicle.services.notifications

import com.openlattice.chronicle.constants.NotificationType
import java.util.*

class NotificationEntry(
        var id: String,
        var type: NotificationType,
        var recurrenceRule: String,
        var title: String,
        var message: String
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (javaClass != other?.javaClass) return false

        other as NotificationEntry

        if (other.id != id) return false
        if (other.recurrenceRule != recurrenceRule) return false
        if (other.title != title) return false
        if (other.message != message) return false
        if (other.type != type) return false

        return true
    }

    override fun hashCode(): Int {
        return Objects.hash(id, recurrenceRule, title, message)
    }

    override fun toString(): String {
        return "Notification (id=$id, title=$title, message=$message, rrule=$recurrenceRule, type=$type)"
    }
}