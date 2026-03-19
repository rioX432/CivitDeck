package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.ModelUpdateNotificationRepository

class MarkAllNotificationsReadUseCase(
    private val repository: ModelUpdateNotificationRepository,
) {
    suspend operator fun invoke() {
        repository.markAllRead()
    }
}
