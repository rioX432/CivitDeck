package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.ModelUpdate
import com.riox432.civitdeck.domain.model.UpdateSource
import com.riox432.civitdeck.domain.repository.CreatorFollowRepository
import com.riox432.civitdeck.domain.repository.FavoriteRepository
import com.riox432.civitdeck.domain.repository.ModelRepository
import com.riox432.civitdeck.domain.repository.ModelVersionCheckpointRepository
import com.riox432.civitdeck.util.Logger
import kotlinx.coroutines.flow.firstOrNull

private const val MAX_MODELS_PER_CHECK = 20
private const val MAX_FOLLOWED_MODELS_PER_CHECK = 10

class CheckModelUpdatesUseCase(
    private val favoriteRepository: FavoriteRepository,
    private val modelRepository: ModelRepository,
    private val checkpointRepository: ModelVersionCheckpointRepository,
    private val creatorFollowRepository: CreatorFollowRepository,
) {
    suspend operator fun invoke(): List<ModelUpdate> {
        val favoriteUpdates = checkFavorites()
        val followedUpdates = checkFollowedCreatorModels()
        return favoriteUpdates + followedUpdates
    }

    private suspend fun checkFavorites(): List<ModelUpdate> {
        val favoriteIds = favoriteRepository.getAllFavoriteIds()
        if (favoriteIds.isEmpty()) return emptyList()

        return checkModels(
            modelIds = favoriteIds,
            source = UpdateSource.FAVORITE,
            maxCount = MAX_MODELS_PER_CHECK,
            activeIds = favoriteIds,
        )
    }

    private suspend fun checkFollowedCreatorModels(): List<ModelUpdate> {
        val creators = creatorFollowRepository.getFollowedCreators()
            .firstOrNull() ?: return emptyList()
        if (creators.isEmpty()) return emptyList()

        // Get feed items to find model IDs from followed creators
        val feedItems = creatorFollowRepository.getFeed(forceRefresh = false)
        if (feedItems.isEmpty()) return emptyList()

        val feedModelIds = feedItems.map { it.modelId }.toSet()
        // Exclude models already tracked as favorites to avoid duplicates
        val favoriteIds = favoriteRepository.getAllFavoriteIds()
        val followedOnlyIds = feedModelIds - favoriteIds
        if (followedOnlyIds.isEmpty()) return emptyList()

        return checkModels(
            modelIds = followedOnlyIds,
            source = UpdateSource.FOLLOWED,
            maxCount = MAX_FOLLOWED_MODELS_PER_CHECK,
            activeIds = null,
        )
    }

    @Suppress("LongMethod")
    private suspend fun checkModels(
        modelIds: Set<Long>,
        source: UpdateSource,
        maxCount: Int,
        activeIds: Set<Long>?,
    ): List<ModelUpdate> {
        val checkpoints = checkpointRepository.getAllCheckpoints()

        val sortedIds = modelIds.sortedBy { checkpoints[it]?.second ?: 0L }
        val idsToCheck = sortedIds.take(maxCount)

        val updates = mutableListOf<ModelUpdate>()
        val newCheckpoints = mutableMapOf<Long, Long>()

        for (modelId in idsToCheck) {
            try {
                val model = modelRepository.getModel(modelId)
                val latestVersion = model.modelVersions.firstOrNull() ?: continue
                val previousVersionId = checkpoints[modelId]?.first

                if (previousVersionId != null && latestVersion.id != previousVersionId) {
                    updates.add(
                        ModelUpdate(
                            modelId = model.id,
                            modelName = model.name,
                            newVersionName = latestVersion.name,
                            newVersionId = latestVersion.id,
                            source = source,
                        ),
                    )
                }
                newCheckpoints[modelId] = latestVersion.id
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Logger.w("CheckModelUpdates", "Failed to fetch model $modelId, skipping: ${e.message}")
            }
        }

        if (newCheckpoints.isNotEmpty()) {
            checkpointRepository.saveCheckpoints(newCheckpoints)
        }
        if (activeIds != null) {
            checkpointRepository.deleteStaleCheckpoints(activeIds)
        }

        return updates
    }
}
