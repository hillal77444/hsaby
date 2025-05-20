package com.hsaby.accounting.ui.notifications

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.hsaby.accounting.data.local.entity.NotificationEntity
import com.hsaby.accounting.data.local.entity.NotificationType
import com.hsaby.accounting.databinding.ItemNotificationBinding
import java.text.SimpleDateFormat
import java.util.*

class NotificationsAdapter(
    private val onNotificationClick: (NotificationEntity) -> Unit
) : ListAdapter<NotificationEntity, NotificationsAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class NotificationViewHolder(
        private val binding: ItemNotificationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onNotificationClick(getItem(position))
                }
            }
        }

        fun bind(notification: NotificationEntity) {
            binding.apply {
                titleTextView.text = notification.title
                messageTextView.text = notification.message
                timeTextView.text = formatDate(notification.createdAt)
                
                // تعيين أيقونة حسب نوع الإشعار
                iconImageView.setImageResource(
                    when (notification.type) {
                        NotificationType.SYNC_SUCCESS -> R.drawable.ic_sync_success
                        NotificationType.SYNC_ERROR -> R.drawable.ic_sync_error
                        NotificationType.TRANSACTION_ADDED -> R.drawable.ic_transaction_add
                        NotificationType.TRANSACTION_UPDATED -> R.drawable.ic_transaction_edit
                        NotificationType.ACCOUNT_UPDATED -> R.drawable.ic_account_edit
                        NotificationType.BALANCE_ALERT -> R.drawable.ic_balance_alert
                        NotificationType.SYSTEM -> R.drawable.ic_system
                    }
                )

                // تعيين حالة القراءة
                unreadIndicator.visibility = if (notification.isRead) View.GONE else View.VISIBLE
            }
        }

        private fun formatDate(timestamp: Long): String {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            return dateFormat.format(Date(timestamp))
        }
    }

    private class NotificationDiffCallback : DiffUtil.ItemCallback<NotificationEntity>() {
        override fun areItemsTheSame(oldItem: NotificationEntity, newItem: NotificationEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: NotificationEntity, newItem: NotificationEntity): Boolean {
            return oldItem == newItem
        }
    }
} 