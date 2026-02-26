package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.ModelUpdate
import com.riox432.civitdeck.domain.repository.FavoriteRepository
import com.riox432.civitdeck.domain.repository.ModelRepository
import com.riox432.civitdeck.domain.repository.ModelVersionCheckpointRepository

private const val MAX_MODELS_PER_CHECK = 20

class CheckModelUpdatesUseCase(
    private val favoriteRepository: FavoriteRepository,
    private val modelRepository: ModelRepository,
    private val checkpointRepository: ModelVersionCheckpointRepository,
) {
    @Suppress("TooGenericExceptionCaught")
    suspend operator fun invoke(): List<ModelUpdate> {
        val favoriteIds = favoriteRepository.getAllFavoriteIds()
        if (favoriteIds.isEmpty()) return emptyList()

        val checkpoints = checkpointRepository.getAllCheckpoints()
        val idsToCheck = favoriteIds.take(MAX_MODELS_PER_CHECK)
        val updates = mutableListOf<ModelUpdate>()
        val newCheckpoints = mutableMapOf<Long, Long>()

        for (modelId in idsToCheck) {
            try {
                val model = modelRepository.getModel(modelId)
                val latestVersion = model.modelVersions.firstOrNull() ?: continue
                val previousVersionId = checkpoints[modelId]

                if (previousVersionId != null && latestVersion.id != previousVersionId) {
                    updates.add(
                        ModelUpdate(
                            modelId = model.id,
                            modelName = model.name,
                            newVersionName = latestVersion.name,
                            newVersionId = latestVersion.id,
                        ),
                    )
                }
                newCheckpoints[modelId] = latestVersion.id
            } catch (_: Exception) {
                // Skip models that fail to fetch (deleted, network error, etc.)
            }
        }

        if (newCheckpoints.isNotEmpty()) {
            checkpointRepository.saveCheckpoints(newCheckpoints)
        }
        checkpointRepository.deleteStaleCheckpoints(favoriteIds)

        return updates
    }
}
