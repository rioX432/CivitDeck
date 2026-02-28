package com.riox432.civitdeck.feature.comfyui.domain.usecase

import com.riox432.civitdeck.domain.model.CivitaiLinkActivity
import com.riox432.civitdeck.domain.model.CivitaiLinkResource
import com.riox432.civitdeck.domain.model.CivitaiLinkStatus
import com.riox432.civitdeck.domain.repository.CivitaiLinkRepository
import com.riox432.civitdeck.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow

class ObserveCivitaiLinkStatusUseCase(private val repository: CivitaiLinkRepository) {
    operator fun invoke(): Flow<CivitaiLinkStatus> = repository.observeStatus()
}

class ObserveCivitaiLinkActivitiesUseCase(private val repository: CivitaiLinkRepository) {
    operator fun invoke(): Flow<List<CivitaiLinkActivity>> = repository.observeActivities()
}

class ConnectCivitaiLinkUseCase(
    private val repository: CivitaiLinkRepository,
    private val prefs: UserPreferencesRepository,
) {
    suspend operator fun invoke(): Boolean {
        val key = prefs.getCivitaiLinkKey() ?: return false
        repository.connect(key)
        return true
    }
}

class DisconnectCivitaiLinkUseCase(private val repository: CivitaiLinkRepository) {
    operator fun invoke() = repository.disconnect()
}

class SendResourceToPCUseCase(private val repository: CivitaiLinkRepository) {
    suspend operator fun invoke(resource: CivitaiLinkResource) =
        repository.sendResourceToPC(resource)
}

class CancelLinkActivityUseCase(private val repository: CivitaiLinkRepository) {
    suspend operator fun invoke(activityId: String) = repository.cancelActivity(activityId)
}
