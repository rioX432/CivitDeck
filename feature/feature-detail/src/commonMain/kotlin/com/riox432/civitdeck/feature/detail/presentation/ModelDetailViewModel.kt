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
import com.riox432.civitdeck.domain.usecase.AddPersonalTagUseCase
import com.riox432.civitdeck.domain.usecase.CancelDownloadUseCase
import com.riox432.civitdeck.domain.usecase.DeleteModelNoteUseCase
import com.riox432.civitdeck.domain.usecase.EnqueueDownloadUseCase
import com.riox432.civitdeck.domain.usecase.GetModelDetailUseCase
import com.riox432.civitdeck.domain.usecase.GetModelReviewsUseCase
import com.riox432.civitdeck.domain.usecase.GetRatingTotalsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveIsFavoriteUseCase
import com.riox432.civitdeck.domain.usecase.ObserveModelDownloadsUseCase
import com.riox432.civitdeck.domain.usecase.ObserveModelNoteUseCase
import com.riox432.civitdeck.domain.usecase.ObserveNsfwFilterUseCase
import com.riox432.civitdeck.domain.usecase.ObservePersonalTagsUseCase
import com.riox432.civitdeck.domain.usecase.ObservePowerUserModeUseCase
import com.riox432.civitdeck.domain.usecase.RemovePersonalTagUseCase
import com.riox432.civitdeck.domain.usecase.SaveModelNoteUseCase
import com.riox432.civitdeck.domain.usecase.SubmitReviewUseCase
import com.riox432.civitdeck.domain.usecase.ToggleFavoriteUseCase
import com.riox432.civitdeck.domain.usecase.TrackModelViewUseCase
import com.riox432.civitdeck.domain.util.currentTimeMillis
import com.riox432.civitdeck.feature.collections.domain.usecase.AddModelToCollectionUseCase
import com.riox432.civitdeck.feature.collections.domain.usecase.CreateCollectionUseCase
import com.riox432.civitdeck.feature.collections.domain.usecase.ObserveCollectionsUseCase
import com.riox432.civitdeck.feature.collections.domain.usecase.ObserveModelCollectionsUseCase
import com.riox432.civitdeck.feature.collections.domain.usecase.RemoveModelFromCollectionUseCase
import com.riox432.civitdeck.feature.gallery.domain.usecase.EnrichModelImagesUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
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
    private val enrichedVersionIds = mutableSetOf<Long>()
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
            kotlinx.coroutines.MainScope().launch {
                try {
                    trackModelViewUseCase.endView(modelId, durationMs)
                } catch (_: Exception) {
                    // Duration update failure is non-critical
                }
            }
        }
    }

    fun onFavoriteToggle() {
        val model = _uiState.value.model ?: return
        viewModelScope.launch {
            try {
                toggleFavoriteUseCase(model)
                trackModelViewUseCase.trackInteraction(modelId, InteractionType.FAVORITE)
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

    fun saveNote(text: String) {
        viewModelScope.launch {
            try {
                if (text.isBlank()) {
                    deleteModelNoteUseCase(modelId)
                } else {
                    saveModelNoteUseCase(modelId, text)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Note save failure is non-critical
            }
        }
    }

    fun addTag(tag: String) {
        val trimmed = tag.trim().lowercase()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            try {
                addPersonalTagUseCase(modelId, trimmed)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Tag add failure is non-critical
            }
        }
    }

    fun removeTag(tag: String) {
        viewModelScope.launch {
            try {
                removePersonalTagUseCase(modelId, tag)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Tag remove failure is non-critical
            }
        }
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
                    modelName = model.name,
                    modelType = model.type.name,
                    creatorName = model.creator?.username,
                    thumbnailUrl = model.modelVersions.firstOrNull()
                        ?.images?.firstOrNull()?.url,
                    tags = model.tags,
                )
                viewStartTimeMs = currentTimeMillis()
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
            try {
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
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Download enqueue failure is non-critical
            }
        }
    }

    fun cancelDownload(downloadId: Long) {
        viewModelScope.launch {
            try {
                cancelDownloadUseCase(downloadId)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Cancel failure is non-critical
            }
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
            try {
                submitReviewUseCase(modelId, modelVersionId, rating, recommended, details)
                _uiState.update {
                    it.copy(isSubmittingReview = false, reviewSubmitSuccess = true)
                }
                loadReviews()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
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
            try {
                val totals = getRatingTotalsUseCase(modelId)
                val page = getModelReviewsUseCase(modelId)
                val sorted = sortReviews(page.items, _uiState.value.reviewSortOrder)
                _uiState.update {
                    it.copy(
                        reviews = sorted,
                        ratingTotals = totals,
                        isReviewsLoading = false,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
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

private const val STOP_TIMEOUT = 5_000L
private const val KB_TO_BYTES = 1024.0
