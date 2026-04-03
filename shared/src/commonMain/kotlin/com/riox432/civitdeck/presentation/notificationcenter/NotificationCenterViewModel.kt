package com.riox432.civitdeck.presentation.notificationcenter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.ModelUpdateNotification
import com.riox432.civitdeck.domain.usecase.GetModelUpdateNotificationsUseCase
import com.riox432.civitdeck.domain.usecase.MarkAllNotificationsReadUseCase
import com.riox432.civitdeck.domain.usecase.MarkNotificationReadUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class NotificationCenterUiState(
    val notifications: List<ModelUpdateNotification> = emptyList(),
    val isLoading: Boolean = true,
)

class NotificationCenterViewModel(
    private val getNotificationsUseCase: GetModelUpdateNotificationsUseCase,
    private val markReadUseCase: MarkNotificationReadUseCase,
    private val markAllReadUseCase: MarkAllNotificationsReadUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationCenterUiState())
    val uiState: StateFlow<NotificationCenterUiState> = _uiState

    init {
        observeNotifications()
    }

    fun markRead(notificationId: Long) {
        viewModelScope.launch { markReadUseCase(notificationId) }
    }

    fun markAllRead() {
        viewModelScope.launch { markAllReadUseCase() }
    }

    private fun observeNotifications() {
        viewModelScope.launch {
            getNotificationsUseCase().collect { notifications ->
                _uiState.value = NotificationCenterUiState(
                    notifications = notifications,
                    isLoading = false,
                )
            }
        }
    }
}
