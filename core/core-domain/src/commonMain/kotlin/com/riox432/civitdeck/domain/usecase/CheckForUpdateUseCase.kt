package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.UpdateResult
import com.riox432.civitdeck.domain.repository.UpdateRepository

class CheckForUpdateUseCase(
    private val updateRepository: UpdateRepository,
) {
    suspend operator fun invoke(): UpdateResult = updateRepository.checkForUpdate()
}
