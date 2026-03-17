package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.UpdateRepository

class SetAutoUpdateCheckUseCase(
    private val repository: UpdateRepository,
) {
    suspend operator fun invoke(enabled: Boolean) = repository.setAutoUpdateCheckEnabled(enabled)
}
