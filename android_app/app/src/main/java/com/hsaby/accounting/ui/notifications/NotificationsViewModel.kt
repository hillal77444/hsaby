package com.hsaby.accounting.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hsaby.accounting.data.model.Notification
import com.hsaby.accounting.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotificationsUiState>(NotificationsUiState.Loading)
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        loadNotifications()
    }

    fun refresh() {
        loadNotifications()
    }

    fun markAsRead(notification: Notification) {
        viewModelScope.launch {
            try {
                notificationRepository.markAsRead(notification.id)
            } catch (e: Exception) {
                _uiState.value = NotificationsUiState.Error(e.message ?: "حدث خطأ أثناء تحديث حالة الإشعار")
            }
        }
    }

    private fun loadNotifications() {
        viewModelScope.launch {
            try {
                notificationRepository.getAllNotifications()
                    .map { notifications ->
                        NotificationsUiState.Success(
                            notifications = notifications.sortedByDescending { it.date }
                        )
                    }
                    .catch { e ->
                        _uiState.value = NotificationsUiState.Error(e.message ?: "حدث خطأ أثناء تحميل الإشعارات")
                    }
                    .collect { state ->
                        _uiState.value = state
                    }
            } catch (e: Exception) {
                _uiState.value = NotificationsUiState.Error(e.message ?: "حدث خطأ أثناء تحميل الإشعارات")
            }
        }
    }
}

sealed class NotificationsUiState {
    object Loading : NotificationsUiState()
    data class Success(val notifications: List<Notification>) : NotificationsUiState()
    data class Error(val message: String) : NotificationsUiState()
} 