package com.openlattice.chronicle.services.notifications

import com.openlattice.chronicle.constants.NotificationType
import java.util.*

class NotificationEntry(
        var id: String,
        var type: NotificationType,
        var cronExpression: String,
        var title: String,
        var message: String
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (javaClass != other?.javaClass) return false

        other as NotificationEntry

        if (other.id != id) return false
        if (other.cronExpression != cronExpression) return false
        if (other.title != title) return false
        if (other.message != message) return false
        if (other.type != type) return false

        return true
    }

    override fun hashCode(): Int {
        return Objects.hash(id, cronExpression, title, message)
    }

    override fun toString(): String {
        return "Notification (id=$id, title=$title, message=$message, cron=$cronExpression, type=$type)"
    }
}