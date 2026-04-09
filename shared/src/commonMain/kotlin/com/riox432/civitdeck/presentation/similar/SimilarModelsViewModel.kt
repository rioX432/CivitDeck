package com.riox432.civitdeck.presentation.similar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.repository.ModelEmbeddingRepository
import com.riox432.civitdeck.domain.usecase.FindSimilarModelsByEmbeddingUseCase
import com.riox432.civitdeck.domain.usecase.GetModelDetailUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SimilarModelsUiState(
    val sourceModel: Model? = null,
    val similarModels: List<Model> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

/**
 * Drives the "Find Similar Models" screen using cached SigLIP-2 image embeddings.
 *
 * Flow:
 *  1. Fetch the source [Model] for the top bar title and context.
 *  2. Look up its cached embedding. If the source has not been embedded yet, the
 *     screen falls back to an empty state — this is expected while the on-device
 *     embedder is still rolling out (#602 phases C/D).
 *  3. Run a cosine-similarity search over the rest of the embedding cache and
 *     resolve each [com.riox432.civitdeck.domain.model.SimilarModelHit] to a full
 *     [Model] via per-id detail lookups in parallel.
 */
class SimilarModelsViewModel(
    private val modelId: Long,
    private val getModelDetail: GetModelDetailUseCase,
    private val embeddingRepository: ModelEmbeddingRepository,
    private val findSimilarByEmbedding: FindSimilarModelsByEmbeddingUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SimilarModelsUiState())
    val uiState: StateFlow<SimilarModelsUiState> = _uiState.asStateFlow()

    init {
        loadSimilarModels()
    }

    fun retry() {
        loadSimilarModels()
    }

    @Suppress("TooGenericExceptionCaught")
    private fun loadSimilarModels() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, similarModels = emptyList()) }
            try {
                val source = getModelDetail(modelId)
                _uiState.update { it.copy(sourceModel = source) }

                val sourceEmbedding = embeddingRepository.get(modelId)
                if (sourceEmbedding == null) {
                    // No embedding cached for this model yet — show empty state.
                    _uiState.update { it.copy(similarModels = emptyList(), isLoading = false) }
                    return@launch
                }

                val hits = findSimilarByEmbedding(
                    query = sourceEmbedding.vector,
                    embeddingModel = sourceEmbedding.embeddingModel,
                    sourceModelId = modelId,
                )

                val resolved: List<Model> = coroutineScope {
                    hits
                        .map { hit -> async { runCatching { getModelDetail(hit.modelId) }.getOrNull() } }
                        .awaitAll()
                        .filterNotNull()
                }

                _uiState.update { it.copy(similarModels = resolved, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = e.message ?: "Failed to load similar models", isLoading = false)
                }
            }
        }
    }
}
