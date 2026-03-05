package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.CreatorFollowRepository
import kotlinx.coroutines.flow.Flow

class GetUnreadFeedCountUseCase(private val repository: CreatorFollowRepository) {
    operator fun invoke(): Flow<Int> = repository.getUnreadCount()
}
