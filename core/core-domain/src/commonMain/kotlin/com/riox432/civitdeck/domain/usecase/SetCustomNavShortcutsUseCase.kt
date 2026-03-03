package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.NavShortcut
import com.riox432.civitdeck.domain.repository.AppBehaviorPreferencesRepository

class SetCustomNavShortcutsUseCase(private val repository: AppBehaviorPreferencesRepository) {
    suspend operator fun invoke(items: List<NavShortcut>) = repository.setCustomNavShortcuts(items)
}
