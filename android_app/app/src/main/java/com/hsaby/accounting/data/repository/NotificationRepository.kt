package com.hsaby.accounting.data.repository

import com.hsaby.accounting.data.local.dao.NotificationDao
import com.hsaby.accounting.data.local.entity.NotificationEntity
import com.hsaby.accounting.data.model.Notification
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val notificationDao: NotificationDao
) {
    fun getAllNotifications(): Flow<List<Notification>> {
        return notificationDao.getAllNotifications().map { entities ->
            entities.map { it.toModel() }
        }
    }

    suspend fun getNotificationById(id: Long): Notification? {
        return notificationDao.getNotificationById(id)?.toModel()
    }

    suspend fun insertNotification(notification: Notification) {
        notificationDao.insertNotification(notification.toEntity())
    }

    suspend fun updateNotification(notification: Notification) {
        notificationDao.updateNotification(notification.toEntity())
    }

    suspend fun deleteNotification(notification: Notification) {
        notificationDao.deleteNotification(notification.toEntity())
    }

    suspend fun markAsRead(id: Long) {
        notificationDao.markAsRead(id)
    }

    suspend fun markAllAsRead() {
        notificationDao.markAllAsRead()
    }

    private fun NotificationEntity.toModel(): Notification {
        return Notification(
            id = id,
            title = title,
            message = message,
            date = date,
            isRead = isRead
        )
    }

    private fun Notification.toEntity(): NotificationEntity {
        return NotificationEntity(
            id = id,
            title = title,
            message = message,
            date = date,
            isRead = isRead
        )
    }
} 