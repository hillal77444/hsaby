package com.hsaby.accounting.ui.notifications

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hsaby.accounting.data.local.AppDatabase
import com.hsaby.accounting.data.local.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class NotificationsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val notificationDao = database.notificationDao()

    val notifications: Flow<List<NotificationEntity>> = notificationDao.getAllNotifications()

    fun markAsRead(notificationId: Long) {
        viewModelScope.launch {
            notificationDao.markAsRead(notificationId)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            notificationDao.markAllAsRead()
        }
    }

    fun deleteNotification(notification: NotificationEntity) {
        viewModelScope.launch {
            notificationDao.deleteNotification(notification)
        }
    }
} 