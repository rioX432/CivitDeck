package com.riox432.civitdeck.feature.externalserver.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.util.UiLoadingState
import com.riox432.civitdeck.domain.util.suspendRunCatching
import com.riox432.civitdeck.feature.externalserver.domain.model.ExternalServerImageFilters
import com.riox432.civitdeck.feature.externalserver.domain.model.GenerationChoice
import com.riox432.civitdeck.feature.externalserver.domain.model.GenerationJob
import com.riox432.civitdeck.feature.externalserver.domain.model.GenerationOption
import com.riox432.civitdeck.feature.externalserver.domain.model.ServerCapabilities
import com.riox432.civitdeck.feature.externalserver.domain.model.ServerImage
import com.riox432.civitdeck.feature.externalserver.domain.usecase.DeleteServerImagesUseCase
import com.riox432.civitdeck.feature.externalserver.domain.usecase.ExecuteGenerationUseCase
import com.riox432.civitdeck.feature.externalserver.domain.usecase.GetDependentChoicesUseCase
import com.riox432.civitdeck.feature.externalserver.domain.usecase.GetExternalServerCapabilitiesUseCase
import com.riox432.civitdeck.feature.externalserver.domain.usecase.GetExternalServerImagesUseCase
import com.riox432.civitdeck.feature.externalserver.domain.usecase.GetGenerationOptionsUseCase
import com.riox432.civitdeck.feature.externalserver.domain.usecase.GetGenerationStatusUseCase
import com.riox432.civitdeck.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExternalServerGalleryUiState(
    val images: List<ServerImage> = emptyList(),
    val filters: ExternalServerImageFilters = ExternalServerImageFilters(),
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    override val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    override val error: String? = null,
    // Capabilities
    val capabilities: ServerCapabilities = ServerCapabilities(),
    val supportsFilters: Boolean = false,
    val supportsGeneration: Boolean = false,
    val supportsGenerationOptions: Boolean = false,
    // Generation
    val generationOptions: List<GenerationOption> = emptyList(),
    val generationParams: Map<String, String> = emptyMap(),
    val dependentChoices: Map<String, List<GenerationChoice>> = emptyMap(),
    val isLoadingOptions: Boolean = false,
    val isSubmittingGeneration: Boolean = false,
    val activeJob: GenerationJob? = null,
    val generationError: String? = null,
    // Filter UI state
    val showFilterSheet: Boolean = false,
    val showGenerationSheet: Boolean = false,
    // Selection mode
    val isSelectionMode: Boolean = false,
    val selectedCloudKeys: Set<String> = emptySet(),
    val isDeleting: Boolean = false,
    val deleteError: String? = null,
) : UiLoadingState

private const val TAG = "ExternalServerGalleryViewModel"
private const val PAGE_SIZE = 96

@Suppress("TooManyFunctions")
class ExternalServerGalleryViewModel(
    private val getImages: GetExternalServerImagesUseCase,
    private val getCapabilities: GetExternalServerCapabilitiesUseCase,
    getGenerationOptions: GetGenerationOptionsUseCase,
    getDependentChoices: GetDependentChoicesUseCase,
    executeGeneration: ExecuteGenerationUseCase,
    getGenerationStatus: GetGenerationStatusUseCase,
    private val deleteServerImages: DeleteServerImagesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExternalServerGalleryUiState())
    val uiState: StateFlow<ExternalServerGalleryUiState> = _uiState.asStateFlow()

    private val generationDelegate = GalleryGenerationDelegate(
        scope = viewModelScope,
        uiState = _uiState,
        getGenerationOptions = getGenerationOptions,
        getDependentChoices = getDependentChoices,
        executeGeneration = executeGeneration,
        getGenerationStatus = getGenerationStatus,
        onGenerationCompleted = ::onRefresh,
    )

    init {
        loadCapabilities()
        loadFirstPage()
    }

    // region Capabilities

    private fun loadCapabilities() {
        viewModelScope.launch {
            suspendRunCatching { getCapabilities() }
                .onSuccess { caps ->
                    _uiState.update {
                        it.copy(
                            capabilities = caps,
                            supportsFilters = caps.supports("images.filters"),
                            supportsGeneration = caps.supports("generation"),
                            supportsGenerationOptions = caps.supports("generation.options"),
                        )
                    }
                }
                .onFailure { e -> Logger.w(TAG, "Load capabilities failed: ${e.message}") }
        }
    }

    // endregion

    // region Gallery

    fun onFiltersChanged(filters: ExternalServerImageFilters) {
        _uiState.update { it.copy(filters = filters, images = emptyList(), currentPage = 1) }
        loadFirstPage()
    }

    fun onLoadMore() {
        val state = _uiState.value
        if (state.isLoadingMore || state.currentPage >= state.totalPages) return
        loadPage(state.currentPage + 1)
    }

    fun onRetry() = loadFirstPage()

    fun onRefresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            suspendRunCatching {
                getImages(1, PAGE_SIZE, _uiState.value.filters)
            }.onSuccess { response ->
                _uiState.update {
                    it.copy(
                        images = response.images,
                        currentPage = response.page,
                        totalPages = response.totalPages,
                        isRefreshing = false,
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(isRefreshing = false, error = e.message ?: "Refresh failed")
                }
            }
        }
    }

    // Filter UI
    fun onShowFilterSheet() {
        _uiState.update { it.copy(showFilterSheet = true) }
    }

    fun onDismissFilterSheet() {
        _uiState.update { it.copy(showFilterSheet = false) }
    }

    fun onSearchChanged(search: String) {
        val newFilters = _uiState.value.filters.copy(search = search)
        onFiltersChanged(newFilters)
    }

    fun onSortChanged(sort: String) {
        val newFilters = _uiState.value.filters.copy(sort = sort)
        onFiltersChanged(newFilters)
    }

    fun onCharacterFilterChanged(character: String) {
        val newFilters = _uiState.value.filters.copy(character = character)
        onFiltersChanged(newFilters)
    }

    fun onScenarioFilterChanged(scenario: String) {
        val newFilters = _uiState.value.filters.copy(scenario = scenario)
        onFiltersChanged(newFilters)
    }

    fun onNsfwFilterChanged(nsfw: String) {
        val newFilters = _uiState.value.filters.copy(nsfw = nsfw)
        onFiltersChanged(newFilters)
    }

    fun onResetFilters() {
        onFiltersChanged(ExternalServerImageFilters())
    }

    private fun loadFirstPage() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            suspendRunCatching {
                getImages(1, PAGE_SIZE, _uiState.value.filters)
            }.onSuccess { response ->
                _uiState.update {
                    it.copy(
                        images = response.images,
                        currentPage = response.page,
                        totalPages = response.totalPages,
                        isLoading = false,
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load images")
                }
            }
        }
    }

    private fun loadPage(page: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            suspendRunCatching {
                getImages(page, PAGE_SIZE, _uiState.value.filters)
            }.onSuccess { response ->
                _uiState.update {
                    it.copy(
                        images = it.images + response.images,
                        currentPage = response.page,
                        totalPages = response.totalPages,
                        isLoadingMore = false,
                    )
                }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(isLoadingMore = false, error = e.message ?: "Failed to load more")
                }
            }
        }
    }

    // endregion

    // region Generation (delegated)

    fun onShowGenerationSheet() = generationDelegate.onShowGenerationSheet()
    fun onDismissGenerationSheet() = generationDelegate.onDismissGenerationSheet()
    fun onGenerationParamChanged(key: String, value: String) =
        generationDelegate.onGenerationParamChanged(key, value)
    fun onSubmitGeneration() = generationDelegate.onSubmitGeneration()
    fun onDismissJobStatus() = generationDelegate.onDismissJobStatus()

    // endregion

    // region Selection & Delete

    fun onEnterSelectionMode(cloudKey: String) {
        _uiState.update { it.copy(isSelectionMode = true, selectedCloudKeys = setOf(cloudKey)) }
    }

    fun onToggleSelection(cloudKey: String) {
        _uiState.update { state ->
            val newSet = state.selectedCloudKeys.toMutableSet()
            if (cloudKey in newSet) newSet.remove(cloudKey) else newSet.add(cloudKey)
            if (newSet.isEmpty()) {
                state.copy(isSelectionMode = false, selectedCloudKeys = emptySet())
            } else {
                state.copy(selectedCloudKeys = newSet)
            }
        }
    }

    fun onSelectAll() {
        _uiState.update { state ->
            state.copy(selectedCloudKeys = state.images.map { it.cloudKey }.toSet())
        }
    }

    fun onExitSelectionMode() {
        _uiState.update { it.copy(isSelectionMode = false, selectedCloudKeys = emptySet()) }
    }

    fun onDeleteSelected() {
        val keys = _uiState.value.selectedCloudKeys.toList()
        if (keys.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, deleteError = null) }
            suspendRunCatching { deleteServerImages(keys) }
                .onSuccess {
                    _uiState.update { state ->
                        state.copy(
                            images = state.images.filterNot { it.cloudKey in keys },
                            isSelectionMode = false,
                            selectedCloudKeys = emptySet(),
                            isDeleting = false,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isDeleting = false, deleteError = e.message ?: "Delete failed")
                    }
                }
        }
    }

    // endregion

    override fun onCleared() {
        super.onCleared()
        generationDelegate.cancelPolling()
    }
}
