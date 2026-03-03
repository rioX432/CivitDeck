package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.NavShortcut
import com.riox432.civitdeck.domain.repository.AppBehaviorPreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveCustomNavShortcutsUseCase(private val repository: AppBehaviorPreferencesRepository) {
    operator fun invoke(): Flow<List<NavShortcut>> = repository.observeCustomNavShortcuts()
}
