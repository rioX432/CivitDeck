package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.CreatorFollowRepository
import kotlinx.coroutines.flow.Flow

class IsFollowingCreatorUseCase(private val repository: CreatorFollowRepository) {
    operator fun invoke(username: String): Flow<Boolean> = repository.isFollowing(username)
}
