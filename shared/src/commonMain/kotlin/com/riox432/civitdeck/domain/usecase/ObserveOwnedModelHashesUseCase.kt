package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.LocalModelFileRepository
import kotlinx.coroutines.flow.Flow

class ObserveOwnedModelHashesUseCase(private val repository: LocalModelFileRepository) {
    operator fun invoke(): Flow<Set<String>> = repository.observeOwnedHashes()
}
