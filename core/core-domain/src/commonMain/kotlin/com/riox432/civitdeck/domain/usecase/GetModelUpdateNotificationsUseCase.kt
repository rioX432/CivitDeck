package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.ModelUpdateNotification
import com.riox432.civitdeck.domain.repository.ModelUpdateNotificationRepository
import kotlinx.coroutines.flow.Flow

class GetModelUpdateNotificationsUseCase(
    private val repository: ModelUpdateNotificationRepository,
) {
    operator fun invoke(): Flow<List<ModelUpdateNotification>> =
        repository.observeNotifications()
}
