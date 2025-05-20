package com.hsaby.accounting.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val message: String,
    val type: NotificationType,
    val isRead: Boolean = false,
    val createdAt: Date = Date(),
    val data: Map<String, String> = emptyMap() // بيانات إضافية مثل معرف الحساب أو المعاملة
)

enum class NotificationType {
    SYNC_SUCCESS,
    SYNC_ERROR,
    TRANSACTION_ADDED,
    TRANSACTION_UPDATED,
    ACCOUNT_UPDATED,
    BALANCE_ALERT,
    SYSTEM
} 