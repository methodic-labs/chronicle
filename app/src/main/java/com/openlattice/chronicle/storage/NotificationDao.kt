package com.openlattice.chronicle.storage

import android.arch.persistence.room.*

@Dao
interface NotificationDao {
    @Query("select * from notifications")
    fun getAllNotifications() :List<Notification>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertNotifications(vararg notification: Notification)

    @Delete
    fun deleteNotifications(vararg  notification: Notification)

    @Query("select * from notifications where id not in (:ids)")
    fun getNotificationsNotOfIds(ids :List<String>) :List<Notification>

    @Query("select * from notifications where id = :id limit 1")
    fun getNotificationById(id :String) :Notification
}