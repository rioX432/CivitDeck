package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.ModelUpdate
import com.riox432.civitdeck.domain.model.UpdateSource
import com.riox432.civitdeck.domain.repository.ModelUpdateNotificationRepository

class CheckAndStoreModelUpdatesUseCase(
    private val checkModelUpdatesUseCase: CheckModelUpdatesUseCase,
    private val notificationRepository: ModelUpdateNotificationRepository,
) {
    suspend operator fun invoke(): List<ModelUpdate> {
        val updates = checkModelUpdatesUseCase()
        if (updates.isNotEmpty()) {
            val favoriteUpdates = updates.filter { it.source == UpdateSource.FAVORITE }
            val followedUpdates = updates.filter { it.source == UpdateSource.FOLLOWED }
            notificationRepository.saveNotifications(favoriteUpdates, UpdateSource.FAVORITE)
            notificationRepository.saveNotifications(followedUpdates, UpdateSource.FOLLOWED)
            notificationRepository.cleanupOldNotifications()
        }
        return updates
    }
}
