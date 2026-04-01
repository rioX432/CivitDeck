package com.riox432.civitdeck.feature.detail.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.riox432.civitdeck.domain.model.DownloadStatus
import com.riox432.civitdeck.domain.model.InteractionType
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelCollection
import com.riox432.civitdeck.domain.model.ModelDownload
import com.riox432.civitdeck.domain.model.ModelFile
import com.riox432.civitdeck.domain.model.ModelNote
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.domain.model.PersonalTag
import com.riox432.civitdeck.domain.model.RatingTotals
import com.riox432.civitdeck.domain.model.ResourceReview
import com.riox432.civitdeck.domain.model.ReviewSortOrder
import com.riox432.civitdeck.domain.usecase.AddModelToCollectionUseCase
import com.riox432.civitdeck.domain.usecase.AddPersonalTagUseCase
import com.riox432.civitdeck.domain.usecase.CancelDownloadUseCase
import com.riox432.civitdeck.domain.usecase.CreateCollectionUseCase
import com.riox432.civitdeck.domain.usecase.DeleteModelNoteUseCase
import com.riox432.civitdeck.domain.usecase.EnqueueDownloadUseCase
import com.riox432.civitdeck.domain.usecase.EnrichModelImagesUseCase
import com.riox432.civitdeck.domain.usecase.GetModelDetailUseCase
import com.riox432.civitdeck.domain.usecase.GetModelReviewsUseCase
import com.riox432.civitdeck.domain.usecase.GetRatingTotalsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveCollectionsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveIsFavoriteUseCase
import com.riox432.civitdeck.domain.usecase.ObserveModelCollectionsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveModelDownloadsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveModelNoteUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNsfwFilterUseCase
import com.riox432.civitdeck.domain.usecase.ObservePersonalTagsUseCase
import com.riox432.civitdeck.domain.usecase.ObservePowerUserModeUseCase
import com.riox432.civitdeck.domain.usecase.RemoveModelFromCollectionUseCase
import com.riox432.civitdeck.domain.usecase.RemovePersonalTagUseCase
import com.riox432.civitdeck.domain.usecase.SaveModelNoteUseCase
import com.riox432.civitdeck.domain.usecase.SubmitReviewUseCase
import com.riox432.civitdeck.domain.usecase.ToggleFavoriteUseCase
import com.riox432.civitdeck.domain.usecase.TrackModelViewUseCase
import com.riox432.civitdeck.domain.util.currentTimeMillis
import com.riox432.civitdeck.domain.util.suspendRunCatching
import com.riox432.civitdeck.util.Logger
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

data class ModelDetailUiState(
    val model: Model? = null,
    val isFavorite: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val selectedVersionIndex: Int = 0,
    val nsfwFilterLevel: NsfwFilterLevel = NsfwFilterLevel.Off,
    val powerUserMode: Boolean = false,
    val note: ModelNote? = null,
    val personalTags: List<PersonalTag> = emptyList(),
    val downloads: List<ModelDownload> = emptyList(),
    val reviews: List<ResourceReview> = emptyList(),
    val ratingTotals: RatingTotals? = null,
    val reviewSortOrder: ReviewSortOrder = ReviewSortOrder.Newest,
    val isReviewsLoading: Boolean = false,
    val reviewsError: String? = null,
    val isSubmittingReview: Boolean = false,
    val reviewSubmitSuccess: Boolean = false,
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
    private val observePowerUserModeUseCase: ObservePowerUserModeUseCase,
    private val observeModelNoteUseCase: ObserveModelNoteUseCase,
    private val saveModelNoteUseCase: SaveModelNoteUseCase,
    private val deleteModelNoteUseCase: DeleteModelNoteUseCase,
    private val observePersonalTagsUseCase: ObservePersonalTagsUseCase,
    private val addPersonalTagUseCase: AddPersonalTagUseCase,
    private val removePersonalTagUseCase: RemovePersonalTagUseCase,
    private val observeModelDownloadsUseCase: ObserveModelDownloadsUseCase,
    private val enqueueDownloadUseCase: EnqueueDownloadUseCase,
    private val cancelDownloadUseCase: CancelDownloadUseCase,
    private val getModelReviewsUseCase: GetModelReviewsUseCase,
    private val getRatingTotalsUseCase: GetRatingTotalsUseCase,
    private val submitReviewUseCase: SubmitReviewUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ModelDetailUiState())
    val uiState: StateFlow<ModelDetailUiState> = _uiState.asStateFlow()
    private val enrichedVersionIds = MutableStateFlow<Set<Long>>(emptySet())
    private var viewStartTimeMs: Long = 0L

    private val _downloadEnqueuedEvent = MutableSharedFlow<Long>(extraBufferCapacity = 1)
    val downloadEnqueuedEvent: SharedFlow<Long> = _downloadEnqueuedEvent

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
        observePowerUserMode()
        observeNote()
        observePersonalTags()
        observeDownloads()
        loadReviews()
    }

    fun onVersionSelected(index: Int) {
        _uiState.update { it.copy(selectedVersionIndex = index) }
        enrichCurrentVersion()
    }

    override fun onCleared() {
        super.onCleared()
        if (viewStartTimeMs > 0L) {
            val durationMs = currentTimeMillis() - viewStartTimeMs
            viewStartTimeMs = 0L
            viewModelScope.launch {
                withContext(NonCancellable) {
                    try {
                        withTimeoutOrNull(END_VIEW_TIMEOUT) {
                            trackModelViewUseCase.endView(modelId, durationMs)
                        }
                    } catch (e: Exception) {
                        Logger.w(TAG, "End view tracking failed: ${e.message}")
                    }
                }
            }
        }
    }

    fun onFavoriteToggle() {
        val model = _uiState.value.model ?: return
        viewModelScope.launch {
            suspendRunCatching {
                toggleFavoriteUseCase(model)
                trackModelViewUseCase.trackInteraction(modelId, InteractionType.FAVORITE)
            }.onFailure { e ->
                Logger.w(TAG, "Favorite toggle failed: ${e.message}")
            }
        }
    }

    fun retry() {
        loadModel()
    }

    fun saveNote(text: String) {
        viewModelScope.launch {
            suspendRunCatching {
                if (text.isBlank()) {
                    deleteModelNoteUseCase(modelId)
                } else {
                    saveModelNoteUseCase(modelId, text)
                }
            }.onFailure { e ->
                Logger.w(TAG, "Note save failed: ${e.message}")
            }
        }
    }

    fun addTag(tag: String) {
        val trimmed = tag.trim().lowercase()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            suspendRunCatching { addPersonalTagUseCase(modelId, trimmed) }
                .onFailure { e -> Logger.w(TAG, "Add tag failed: ${e.message}") }
        }
    }

    fun removeTag(tag: String) {
        viewModelScope.launch {
            suspendRunCatching { removePersonalTagUseCase(modelId, tag) }
                .onFailure { e -> Logger.w(TAG, "Remove tag failed: ${e.message}") }
        }
    }

    private fun loadModel() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            suspendRunCatching { getModelDetailUseCase(modelId) }
                .onSuccess { model ->
                    _uiState.update {
                        it.copy(model = model, isLoading = false)
                    }
                    enrichCurrentVersion()
                    trackModelViewUseCase(
                        modelId = model.id,
                        modelName = model.name,
                        modelType = model.type.name,
                        creatorName = model.creator?.username,
                        thumbnailUrl = model.modelVersions.firstOrNull()
                            ?.images?.firstOrNull()?.url,
                        tags = model.tags,
                    )
                    viewStartTimeMs = currentTimeMillis()
                }
                .onFailure { e ->
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
        var alreadyEnriched = false
        enrichedVersionIds.update { ids ->
            if (version.id in ids) {
                alreadyEnriched = true
                ids
            } else {
                ids + version.id
            }
        }
        if (alreadyEnriched) return
        viewModelScope.launch {
            suspendRunCatching { enrichModelImagesUseCase(version.id, version.images) }
                .onSuccess { enriched ->
                    _uiState.update { current ->
                        val currentModel = current.model ?: return@update current
                        val updatedVersions = currentModel.modelVersions.map { v ->
                            if (v.id == version.id) v.copy(images = enriched) else v
                        }
                        current.copy(model = currentModel.copy(modelVersions = updatedVersions))
                    }
                }
                .onFailure { e ->
                    Logger.w(TAG, "Enrich version images failed: ${e.message}")
                    enrichedVersionIds.update { it - version.id }
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
            suspendRunCatching {
                if (collectionId in modelCollectionIds.value) {
                    removeModelFromCollectionUseCase(collectionId, model.id)
                } else {
                    addModelToCollectionUseCase(collectionId, model)
                }
            }.onFailure { e ->
                Logger.w(TAG, "Collection toggle failed: ${e.message}")
            }
        }
    }

    fun createCollectionAndAdd(name: String) {
        val model = _uiState.value.model ?: return
        viewModelScope.launch {
            suspendRunCatching {
                val newId = createCollectionUseCase(name)
                addModelToCollectionUseCase(newId, model)
            }.onFailure { e ->
                Logger.w(TAG, "Create collection and add failed: ${e.message}")
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

    private fun observePowerUserMode() {
        viewModelScope.launch {
            observePowerUserModeUseCase().collect { enabled ->
                _uiState.update { it.copy(powerUserMode = enabled) }
            }
        }
    }

    private fun observeNote() {
        viewModelScope.launch {
            observeModelNoteUseCase(modelId).collect { note ->
                _uiState.update { it.copy(note = note) }
            }
        }
    }

    private fun observePersonalTags() {
        viewModelScope.launch {
            observePersonalTagsUseCase(modelId).collect { tags ->
                _uiState.update { it.copy(personalTags = tags) }
            }
        }
    }

    private fun observeDownloads() {
        viewModelScope.launch {
            observeModelDownloadsUseCase(modelId).collect { downloads ->
                _uiState.update { it.copy(downloads = downloads) }
            }
        }
    }

    fun downloadFile(file: ModelFile) {
        val model = _uiState.value.model ?: return
        val version = model.modelVersions.getOrNull(_uiState.value.selectedVersionIndex) ?: return
        viewModelScope.launch {
            suspendRunCatching {
                val download = ModelDownload(
                    modelId = model.id,
                    modelName = model.name,
                    versionId = version.id,
                    versionName = version.name,
                    fileId = file.id,
                    fileName = file.name,
                    fileUrl = file.downloadUrl,
                    fileSizeBytes = (file.sizeKB * KB_TO_BYTES).toLong(),
                    status = DownloadStatus.Pending,
                    modelType = model.type.name,
                )
                val id = enqueueDownloadUseCase(download)
                _downloadEnqueuedEvent.tryEmit(id)
                trackModelViewUseCase.trackInteraction(modelId, InteractionType.DOWNLOAD)
            }.onFailure { e ->
                Logger.w(TAG, "Download enqueue failed: ${e.message}")
            }
        }
    }

    fun cancelDownload(downloadId: Long) {
        viewModelScope.launch {
            suspendRunCatching { cancelDownloadUseCase(downloadId) }
                .onFailure { e -> Logger.w(TAG, "Cancel download failed: ${e.message}") }
        }
    }

    fun onReviewSortChanged(order: ReviewSortOrder) {
        _uiState.update { it.copy(reviewSortOrder = order) }
        loadReviews()
    }

    @Suppress("LongParameterList")
    fun submitReview(
        modelVersionId: Long,
        rating: Int,
        recommended: Boolean,
        details: String?,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingReview = true) }
            suspendRunCatching {
                submitReviewUseCase(modelId, modelVersionId, rating, recommended, details)
            }
                .onSuccess {
                    _uiState.update {
                        it.copy(isSubmittingReview = false, reviewSubmitSuccess = true)
                    }
                    loadReviews()
                }
                .onFailure { e ->
                    Logger.w(TAG, "Submit review failed: ${e.message}")
                    _uiState.update { it.copy(isSubmittingReview = false) }
                }
        }
    }

    fun dismissReviewSuccess() {
        _uiState.update { it.copy(reviewSubmitSuccess = false) }
    }

    private fun loadReviews() {
        viewModelScope.launch {
            _uiState.update { it.copy(isReviewsLoading = true, reviewsError = null) }
            suspendRunCatching {
                val totals = getRatingTotalsUseCase(modelId)
                val page = getModelReviewsUseCase(modelId)
                totals to sortReviews(page.items, _uiState.value.reviewSortOrder)
            }
                .onSuccess { (totals, sorted) ->
                    _uiState.update {
                        it.copy(
                            reviews = sorted,
                            ratingTotals = totals,
                            isReviewsLoading = false,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isReviewsLoading = false, reviewsError = e.message)
                    }
                }
        }
    }

    private fun sortReviews(
        reviews: List<ResourceReview>,
        order: ReviewSortOrder,
    ): List<ResourceReview> = when (order) {
        ReviewSortOrder.Newest -> reviews.sortedByDescending { it.createdAt }
        ReviewSortOrder.HighestRated -> reviews.sortedByDescending { it.rating }
        ReviewSortOrder.LowestRated -> reviews.sortedBy { it.rating }
    }
}

private const val TAG = "ModelDetailViewModel"
private const val STOP_TIMEOUT = 5_000L
private const val END_VIEW_TIMEOUT = 5_000L
private const val KB_TO_BYTES = 1024.0
