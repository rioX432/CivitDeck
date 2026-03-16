package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.ShareHashtagRepository

class AddShareHashtagUseCase(
    private val repository: ShareHashtagRepository,
) {
    suspend operator fun invoke(tag: String) = repository.addCustom(tag)
}
