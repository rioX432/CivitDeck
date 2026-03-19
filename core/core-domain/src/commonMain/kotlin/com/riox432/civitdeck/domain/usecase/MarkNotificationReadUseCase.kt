package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.ModelUpdateNotificationRepository

class MarkNotificationReadUseCase(
    private val repository: ModelUpdateNotificationRepository,
) {
    suspend operator fun invoke(notificationId: Long) {
        repository.markRead(notificationId)
    }
}
