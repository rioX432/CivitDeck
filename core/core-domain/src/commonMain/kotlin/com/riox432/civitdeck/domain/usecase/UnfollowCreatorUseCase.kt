package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.CreatorFollowRepository

class UnfollowCreatorUseCase(private val repository: CreatorFollowRepository) {
    suspend operator fun invoke(username: String) = repository.unfollowCreator(username)
}
