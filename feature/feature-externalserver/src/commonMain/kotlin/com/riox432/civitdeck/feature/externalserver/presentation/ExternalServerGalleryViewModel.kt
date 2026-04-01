package com.riox432.civitdeck.feature.externalserver.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.feature.externalserver.domain.model.ExternalServerImageFilters
import com.riox432.civitdeck.feature.externalserver.domain.model.GenerationChoice
import com.riox432.civitdeck.feature.externalserver.domain.model.GenerationJob
import com.riox432.civitdeck.feature.externalserver.domain.model.GenerationJobStatus
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class ExternalServerGalleryUiState(
    val images: List<ServerImage> = emptyList(),
    val filters: ExternalServerImageFilters = ExternalServerImageFilters(),
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
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
)

private const val PAGE_SIZE = 96
private const val POLL_INTERVAL_MS = 2000L
private const val MAX_POLL_TIMEOUT_MS = 600_000L

class ExternalServerGalleryViewModel(
    private val getImages: GetExternalServerImagesUseCase,
    private val getCapabilities: GetExternalServerCapabilitiesUseCase,
    private val getGenerationOptions: GetGenerationOptionsUseCase,
    private val getDependentChoices: GetDependentChoicesUseCase,
    private val executeGeneration: ExecuteGenerationUseCase,
    private val getGenerationStatus: GetGenerationStatusUseCase,
    private val deleteServerImages: DeleteServerImagesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExternalServerGalleryUiState())
    val uiState: StateFlow<ExternalServerGalleryUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null

    init {
        loadCapabilities()
        loadFirstPage()
    }

    // region Capabilities

    private fun loadCapabilities() {
        viewModelScope.launch {
            runCatching { getCapabilities() }
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
            runCatching {
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
            runCatching {
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
            runCatching {
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

    // region Generation

    fun onShowGenerationSheet() {
        _uiState.update { it.copy(showGenerationSheet = true, generationError = null) }
        if (_uiState.value.generationOptions.isEmpty()) {
            loadGenerationOptions()
        }
    }

    fun onDismissGenerationSheet() {
        _uiState.update { it.copy(showGenerationSheet = false) }
    }

    fun onGenerationParamChanged(key: String, value: String) {
        val newParams = _uiState.value.generationParams.toMutableMap()
        newParams[key] = value
        _uiState.update { it.copy(generationParams = newParams) }

        // Check if any option depends on this key and reload choices
        val dependents = _uiState.value.generationOptions.filter { it.dependsOn == key }
        dependents.forEach { option ->
            option.choicesEndpoint?.let { endpoint ->
                loadDependentChoices(option.key, endpoint.replace("{$key}", value))
            }
        }
    }

    fun onSubmitGeneration() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingGeneration = true, generationError = null) }
            runCatching {
                executeGeneration(_uiState.value.generationParams)
            }.onSuccess { job ->
                _uiState.update {
                    it.copy(
                        activeJob = job,
                        isSubmittingGeneration = false,
                        showGenerationSheet = false,
                    )
                }
                startPollingJobStatus(job.jobId)
            }.onFailure { e ->
                _uiState.update {
                    it.copy(
                        isSubmittingGeneration = false,
                        generationError = e.message ?: "Generation failed",
                    )
                }
            }
        }
    }

    fun onDismissJobStatus() {
        pollJob?.cancel()
        _uiState.update { it.copy(activeJob = null) }
    }

    private fun loadGenerationOptions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingOptions = true) }
            runCatching { getGenerationOptions() }
                .onSuccess { options ->
                    // Set default values
                    val defaults = options.mapNotNull { option ->
                        option.defaultValue?.let { option.key to it }
                    }.toMap()
                    _uiState.update {
                        it.copy(
                            generationOptions = options,
                            generationParams = defaults,
                            isLoadingOptions = false,
                        )
                    }
                }.onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoadingOptions = false,
                            generationError = e.message ?: "Failed to load options",
                        )
                    }
                }
        }
    }

    private fun loadDependentChoices(key: String, endpoint: String) {
        viewModelScope.launch {
            runCatching { getDependentChoices(endpoint) }
                .onSuccess { choices ->
                    _uiState.update {
                        it.copy(
                            dependentChoices = it.dependentChoices + (key to choices),
                        )
                    }
                }
        }
    }

    @Suppress("MagicNumber")
    private fun startPollingJobStatus(jobId: String) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            val result = withTimeoutOrNull(MAX_POLL_TIMEOUT_MS) {
                while (true) {
                    delay(POLL_INTERVAL_MS)
                    runCatching { getGenerationStatus(jobId) }
                        .onSuccess { job ->
                            _uiState.update { it.copy(activeJob = job) }
                            if (job.status == GenerationJobStatus.COMPLETED ||
                                job.status == GenerationJobStatus.ERROR
                            ) {
                                if (job.status == GenerationJobStatus.COMPLETED) {
                                    onRefresh()
                                }
                                return@withTimeoutOrNull
                            }
                        }
                        .onFailure { return@withTimeoutOrNull }
                }
            }
            if (result == null) {
                _uiState.update {
                    it.copy(
                        activeJob = null,
                        generationError = "Generation timed out after 10 minutes",
                    )
                }
            }
        }
    }

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
            runCatching { deleteServerImages(keys) }
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
        pollJob?.cancel()
    }
}
