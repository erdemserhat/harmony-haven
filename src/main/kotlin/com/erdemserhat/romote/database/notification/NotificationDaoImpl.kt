package com.erdemserhat.romote.database.notification

import com.erdemserhat.models.Notification
import com.erdemserhat.romote.database.DatabaseConfig
import org.ktorm.dsl.*
import org.ktorm.entity.filter
import org.ktorm.entity.sequenceOf
import org.ktorm.entity.toList

class NotificationDaoImpl() : NotificationDao {
    override fun addNotification(notification: Notification): Int {
        return DatabaseConfig.ktormDatabase.insert(DBNotificationTable) {
            set(DBNotificationTable.user_id, notification.userId)
            set(DBNotificationTable.title, notification.title)
            set(DBNotificationTable.content, notification.content)
            set(DBNotificationTable.is_read, notification.isRead)
            set(DBNotificationTable.timeStamp, notification.timeStamp)
        }
    }

    override fun deleteNotification(notificationId: Int): Boolean {
        val affectedRows = DatabaseConfig.ktormDatabase.delete(DBNotificationTable) {
            DBNotificationTable.id eq notificationId
        }
        return affectedRows > 0
    }

    override fun updateNotification(notification: Notification): Boolean {
        try {
            DatabaseConfig.ktormDatabase.update(DBNotificationTable) {
                set(DBNotificationTable.title, notification.title)
                set(DBNotificationTable.content, notification.content)
                where {
                    DBNotificationTable.id eq notification.id
                }


            }
            return true

        } catch (e: Exception) {
            return false
        }

    }



    override fun getNotifications(userId: Int, page: Int, size: Int): List<DBNotificationEntity> {
        // Sayfa numarasına ve boyutuna göre offset hesapla
        val offset = (page - 1) * size
        println("gett")
        // Veritabanı sorgusunu yap
        val notifications = DatabaseConfig.ktormDatabase
            .from(DBNotificationTable)
            .select()
            .where { DBNotificationTable.user_id eq userId }
            .orderBy(DBNotificationTable.timeStamp.desc()) // Sıralama ekleyin
            .limit(offset, size)  // Limit ve offset ile sayfalama yap
            .map { row ->
                println(row)  // Debug çıktısı
                DBNotificationTable.createEntity(row)  // `DBNotificationEntity` döndürür
            }

        println("Fetched notifications: ${notifications.size}")

        return notifications
    }

    override fun markAsRead(notificationId: Int):Boolean {
        return DatabaseConfig.ktormDatabase.update(DBNotificationTable){
            set(DBNotificationTable.is_read, true)
            where {
                DBNotificationTable.id eq notificationId
            }
        }>0
    }
}