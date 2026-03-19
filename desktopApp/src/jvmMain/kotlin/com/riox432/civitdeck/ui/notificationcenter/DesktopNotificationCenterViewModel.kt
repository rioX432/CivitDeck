package com.riox432.civitdeck.ui.notificationcenter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.ModelUpdateNotification
import com.riox432.civitdeck.domain.usecase.GetModelUpdateNotificationsUseCase
import com.riox432.civitdeck.domain.usecase.MarkAllNotificationsReadUseCase
import com.riox432.civitdeck.domain.usecase.MarkNotificationReadUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DesktopNotificationCenterUiState(
    val notifications: List<ModelUpdateNotification> = emptyList(),
    val isLoading: Boolean = true,
)

class DesktopNotificationCenterViewModel(
    private val getNotificationsUseCase: GetModelUpdateNotificationsUseCase,
    private val markReadUseCase: MarkNotificationReadUseCase,
    private val markAllReadUseCase: MarkAllNotificationsReadUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DesktopNotificationCenterUiState())
    val uiState: StateFlow<DesktopNotificationCenterUiState> = _uiState

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
                _uiState.value = DesktopNotificationCenterUiState(
                    notifications = notifications,
                    isLoading = false,
                )
            }
        }
    }

    public override fun onCleared() {
        super.onCleared()
    }
}
