package com.hsaby.accounting.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.hsaby.accounting.R
import com.hsaby.accounting.data.local.AppDatabase
import com.hsaby.accounting.data.local.entity.NotificationEntity
import com.hsaby.accounting.data.local.entity.NotificationType
import com.hsaby.accounting.ui.main.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.*

class NotificationManager private constructor(private val context: Context) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val database = AppDatabase.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Accounting Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for accounting app"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    // إشعارات المزامنة
    fun notifySyncSuccess(syncedItems: Int) {
        val notification = NotificationEntity(
            id = UUID.randomUUID().mostSignificantBits,
            title = "تمت المزامنة بنجاح",
            message = "تم مزامنة $syncedItems عنصر بنجاح",
            type = NotificationType.SYNC_SUCCESS,
            date = System.currentTimeMillis(),
            isRead = false
        )
        showNotification(notification)
        saveNotification(notification)
    }

    fun notifySyncError(error: String) {
        val notification = NotificationEntity(
            id = UUID.randomUUID().mostSignificantBits,
            title = "فشل المزامنة",
            message = "حدث خطأ أثناء المزامنة: $error",
            type = NotificationType.SYNC_ERROR,
            date = System.currentTimeMillis(),
            isRead = false
        )
        showNotification(notification)
        saveNotification(notification)
    }

    // إشعارات المعاملات
    fun notifyTransactionAdded(transactionId: String, amount: Double, currency: String) {
        val notification = NotificationEntity(
            id = UUID.randomUUID().mostSignificantBits,
            title = "معاملة جديدة",
            message = "تمت إضافة معاملة جديدة بقيمة $amount $currency",
            type = NotificationType.TRANSACTION_ADDED,
            data = mapOf("transactionId" to transactionId),
            date = System.currentTimeMillis(),
            isRead = false
        )
        showNotification(notification)
        saveNotification(notification)
    }

    fun notifyTransactionUpdated(transactionId: String, amount: Double, currency: String) {
        val notification = NotificationEntity(
            id = UUID.randomUUID().mostSignificantBits,
            title = "تم تحديث المعاملة",
            message = "تم تحديث المعاملة بقيمة $amount $currency",
            type = NotificationType.TRANSACTION_UPDATED,
            data = mapOf("transactionId" to transactionId),
            date = System.currentTimeMillis(),
            isRead = false
        )
        showNotification(notification)
        saveNotification(notification)
    }

    // إشعارات الحسابات
    fun notifyAccountUpdated(accountId: String, accountName: String, newBalance: Double) {
        val notification = NotificationEntity(
            id = UUID.randomUUID().mostSignificantBits,
            title = "تم تحديث الحساب",
            message = "تم تحديث حساب $accountName. الرصيد الجديد: $newBalance",
            type = NotificationType.ACCOUNT_UPDATED,
            data = mapOf("accountId" to accountId),
            date = System.currentTimeMillis(),
            isRead = false
        )
        showNotification(notification)
        saveNotification(notification)
    }

    fun notifyBalanceAlert(accountId: String, accountName: String, balance: Double, threshold: Double) {
        val notification = NotificationEntity(
            id = UUID.randomUUID().mostSignificantBits,
            title = "تنبيه الرصيد",
            message = "رصيد حساب $accountName ($balance) أقل من الحد الأدنى ($threshold)",
            type = NotificationType.BALANCE_ALERT,
            data = mapOf("accountId" to accountId),
            date = System.currentTimeMillis(),
            isRead = false
        )
        showNotification(notification)
        saveNotification(notification)
    }

    // عرض الإشعار في النظام
    private fun showNotification(notification: NotificationEntity) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notificationId", notification.id)
            putExtra("notificationType", notification.type.name)
            notification.data?.forEach { (key, value) ->
                putExtra(key, value.toString())
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notification.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(notification.title)
            .setContentText(notification.message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(notification.id.toInt(), builder.build())
    }

    // حفظ الإشعار في قاعدة البيانات
    private fun saveNotification(notification: NotificationEntity) {
        scope.launch {
            database.notificationDao().insertNotification(notification)
        }
    }

    // الحصول على الإشعارات
    fun getAllNotifications(): Flow<List<NotificationEntity>> {
        return database.notificationDao().getAllNotifications()
    }

    fun getUnreadNotifications(): Flow<List<NotificationEntity>> {
        return database.notificationDao().getUnreadNotifications()
    }

    fun getUnreadCount(): Flow<Int> {
        return database.notificationDao().getUnreadCount()
    }

    // تحديث حالة الإشعارات
    suspend fun markAsRead(notificationId: Long) {
        database.notificationDao().markAsRead(notificationId)
    }

    suspend fun markAllAsRead() {
        database.notificationDao().markAllAsRead()
    }

    companion object {
        private const val CHANNEL_ID = "accounting_notifications"
        
        @Volatile
        private var instance: NotificationManager? = null
        
        fun getInstance(context: Context): NotificationManager {
            return instance ?: synchronized(this) {
                instance ?: NotificationManager(context.applicationContext).also { instance = it }
            }
        }
    }
} 