package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.ModelUpdateNotificationRepository
import kotlinx.coroutines.flow.Flow

class GetUnreadNotificationCountUseCase(
    private val repository: ModelUpdateNotificationRepository,
) {
    operator fun invoke(): Flow<Int> = repository.observeUnreadCount()
}
