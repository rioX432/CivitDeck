package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.UpdateRepository
import kotlinx.coroutines.flow.Flow

class ObserveAutoUpdateCheckUseCase(
    private val repository: UpdateRepository,
) {
    operator fun invoke(): Flow<Boolean> = repository.observeAutoUpdateCheckEnabled()
}
