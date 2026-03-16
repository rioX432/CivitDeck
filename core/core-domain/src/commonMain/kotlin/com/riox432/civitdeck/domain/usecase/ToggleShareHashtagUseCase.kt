package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.ShareHashtagRepository

class ToggleShareHashtagUseCase(
    private val repository: ShareHashtagRepository,
) {
    suspend operator fun invoke(tag: String, isEnabled: Boolean) =
        repository.setEnabled(tag, isEnabled)
}
