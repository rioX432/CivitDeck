package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.ShareHashtag
import com.riox432.civitdeck.domain.repository.ShareHashtagRepository
import kotlinx.coroutines.flow.Flow

class ObserveShareHashtagsUseCase(
    private val repository: ShareHashtagRepository,
) {
    operator fun invoke(): Flow<List<ShareHashtag>> = repository.observeAll()
}
