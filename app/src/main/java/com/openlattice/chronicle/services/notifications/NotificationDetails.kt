package com.openlattice.chronicle.services.notifications

import com.methodic.chronicle.constants.NotificationType

data class NotificationDetails(
    val id: String,
    val type: NotificationType,
    val recurrenceRule: String,
    val title: String,
    val message: String
)
