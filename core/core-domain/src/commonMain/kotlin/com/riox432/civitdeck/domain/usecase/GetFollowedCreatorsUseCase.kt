package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.FollowedCreator
import com.riox432.civitdeck.domain.repository.CreatorFollowRepository
import kotlinx.coroutines.flow.Flow

class GetFollowedCreatorsUseCase(private val repository: CreatorFollowRepository) {
    operator fun invoke(): Flow<List<FollowedCreator>> = repository.getFollowedCreators()
}
