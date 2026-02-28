package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.domain.repository.UserPreferencesRepository

class SetNsfwFilterUseCase(private val repository: UserPreferencesRepository) {
    suspend operator fun invoke(level: NsfwFilterLevel) = repository.setNsfwFilterLevel(level)
}
