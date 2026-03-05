package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.CreatorFollowRepository

class MarkFeedReadUseCase(private val repository: CreatorFollowRepository) {
    suspend operator fun invoke() = repository.markFeedAsRead()
}
