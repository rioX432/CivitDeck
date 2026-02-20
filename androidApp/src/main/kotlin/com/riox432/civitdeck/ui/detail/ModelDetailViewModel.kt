package com.riox432.civitdeck.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelCollection
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.domain.usecase.AddModelToCollectionUseCase
import com.riox432.civitdeck.domain.usecase.CreateCollectionUseCase
import com.riox432.civitdeck.domain.usecase.EnrichModelImagesUseCase
import com.riox432.civitdeck.domain.usecase.GetModelDetailUseCase
import com.riox432.civitdeck.domain.usecase.ObserveCollectionsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveIsFavoriteUseCase
import com.riox432.civitdeck.domain.usecase.ObserveModelCollectionsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNsfwFilterUseCase
import com.riox432.civitdeck.domain.usecase.RemoveModelFromCollectionUseCase
import com.riox432.civitdeck.domain.usecase.ToggleFavoriteUseCase
import com.riox432.civitdeck.domain.usecase.TrackModelViewUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ModelDetailUiState(
    val model: Model? = null,
    val isFavorite: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedVersionIndex: Int = 0,
    val nsfwFilterLevel: NsfwFilterLevel = NsfwFilterLevel.Off,
)

@Suppress("LongParameterList")
class ModelDetailViewModel(
    private val modelId: Long,
    private val getModelDetailUseCase: GetModelDetailUseCase,
    private val observeIsFavoriteUseCase: ObserveIsFavoriteUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val trackModelViewUseCase: TrackModelViewUseCase,
    private val observeNsfwFilterUseCase: ObserveNsfwFilterUseCase,
    private val enrichModelImagesUseCase: EnrichModelImagesUseCase,
    observeCollectionsUseCase: ObserveCollectionsUseCase,
    private val observeModelCollectionsUseCase: ObserveModelCollectionsUseCase,
    private val addModelToCollectionUseCase: AddModelToCollectionUseCase,
    private val removeModelFromCollectionUseCase: RemoveModelFromCollectionUseCase,
    private val createCollectionUseCase: CreateCollectionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModelDetailUiState())
    val uiState: StateFlow<ModelDetailUiState> = _uiState.asStateFlow()
    private val enrichedVersionIds = mutableSetOf<Long>()

    val collections: StateFlow<List<ModelCollection>> =
        observeCollectionsUseCase()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    val modelCollectionIds: StateFlow<List<Long>> =
        observeModelCollectionsUseCase(modelId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT), emptyList())

    init {
        loadModel()
        observeFavorite()
        observeNsfwFilter()
    }

    fun onVersionSelected(index: Int) {
        _uiState.update { it.copy(selectedVersionIndex = index) }
        enrichCurrentVersion()
    }

    fun onFavoriteToggle() {
        val model = _uiState.value.model ?: return
        viewModelScope.launch {
            try {
                toggleFavoriteUseCase(model)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Favorite toggle failure is non-critical
            }
        }
    }

    fun retry() {
        loadModel()
    }

    private fun loadModel() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val model = getModelDetailUseCase(modelId)
                _uiState.update {
                    it.copy(model = model, isLoading = false)
                }
                enrichCurrentVersion()
                trackModelViewUseCase(
                    modelId = model.id,
                    modelType = model.type.name,
                    creatorName = model.creator?.username,
                    tags = model.tags,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Unknown error")
                }
            }
        }
    }

    private fun enrichCurrentVersion() {
        val state = _uiState.value
        val model = state.model ?: return
        val version = model.modelVersions.getOrNull(state.selectedVersionIndex) ?: return
        if (!enrichedVersionIds.add(version.id)) return
        viewModelScope.launch {
            try {
                val enriched = enrichModelImagesUseCase(version.id, version.images)
                _uiState.update { current ->
                    val currentModel = current.model ?: return@update current
                    val updatedVersions = currentModel.modelVersions.map { v ->
                        if (v.id == version.id) v.copy(images = enriched) else v
                    }
                    current.copy(model = currentModel.copy(modelVersions = updatedVersions))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                enrichedVersionIds.remove(version.id)
            }
        }
    }

    private fun observeFavorite() {
        viewModelScope.launch {
            observeIsFavoriteUseCase(modelId).collect { isFavorite ->
                _uiState.update { it.copy(isFavorite = isFavorite) }
            }
        }
    }

    fun toggleCollection(collectionId: Long) {
        val model = _uiState.value.model ?: return
        viewModelScope.launch {
            try {
                if (collectionId in modelCollectionIds.value) {
                    removeModelFromCollectionUseCase(collectionId, model.id)
                } else {
                    addModelToCollectionUseCase(collectionId, model)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Collection toggle failure is non-critical
            }
        }
    }

    fun createCollectionAndAdd(name: String) {
        val model = _uiState.value.model ?: return
        viewModelScope.launch {
            try {
                val newId = createCollectionUseCase(name)
                addModelToCollectionUseCase(newId, model)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Create + add failure is non-critical
            }
        }
    }

    private fun observeNsfwFilter() {
        viewModelScope.launch {
            observeNsfwFilterUseCase().collect { level ->
                _uiState.update { it.copy(nsfwFilterLevel = level) }
            }
        }
    }
}

private const val STOP_TIMEOUT = 5_000L
