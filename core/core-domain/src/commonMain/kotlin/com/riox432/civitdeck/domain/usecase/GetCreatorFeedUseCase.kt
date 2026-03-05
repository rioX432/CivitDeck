package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.FeedItem
import com.riox432.civitdeck.domain.repository.CreatorFollowRepository

class GetCreatorFeedUseCase(private val repository: CreatorFollowRepository) {
    suspend operator fun invoke(forceRefresh: Boolean = false): List<FeedItem> =
        repository.getFeed(forceRefresh)
}
