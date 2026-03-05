package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.CreatorFollowRepository

class FollowCreatorUseCase(private val repository: CreatorFollowRepository) {
    suspend operator fun invoke(username: String, displayName: String, avatarUrl: String?) =
        repository.followCreator(username, displayName, avatarUrl)
}
