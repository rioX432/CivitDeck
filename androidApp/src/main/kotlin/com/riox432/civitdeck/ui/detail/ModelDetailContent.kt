@file:Suppress("TooManyFunctions")

package com.riox432.civitdeck.ui.detail

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelFile
import com.riox432.civitdeck.domain.model.ModelImage
import com.riox432.civitdeck.domain.model.ModelVersion
import com.riox432.civitdeck.domain.model.filterByNsfwLevel
import com.riox432.civitdeck.feature.detail.presentation.ModelDetailUiState
import com.riox432.civitdeck.ui.components.EmptyStateMessage
import com.riox432.civitdeck.ui.components.ErrorStateView
import com.riox432.civitdeck.ui.components.LoadingStateOverlay
import com.riox432.civitdeck.ui.components.ModelStatsRow
import com.riox432.civitdeck.ui.theme.Duration
import com.riox432.civitdeck.ui.theme.Easing
import com.riox432.civitdeck.ui.theme.Spacing

@Suppress("LongParameterList")
@Composable
internal fun DetailStateContent(
    uiState: ModelDetailUiState,
    model: Model?,
    onRetry: () -> Unit,
    onVersionSelected: (Int) -> Unit,
    onViewImages: (Long) -> Unit,
    onCreatorClick: (String) -> Unit,
    onTryInComfyUI:
    ((sha256: String, modelName: String, meta: com.riox432.civitdeck.domain.model.ImageGenerationMeta?) -> Unit)?,
    onSendToPC: () -> Unit = {},
    onSaveNote: (String) -> Unit = {},
    onAddTag: (String) -> Unit = {},
    onRemoveTag: (String) -> Unit = {},
    onDownloadFile: (ModelFile) -> Unit = {},
    onCancelDownload: (Long) -> Unit = {},
    onReviewSortChanged: (com.riox432.civitdeck.domain.model.ReviewSortOrder) -> Unit = {},
    onWriteReview: () -> Unit = {},
    bottomPadding: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    carouselContent: @Composable () -> Unit = {},
) {
    val stateKey = when {
        uiState.isLoading -> "loading"
        uiState.error != null -> "error"
        model != null -> "content"
        else -> "loading"
    }

    AnimatedContent(
        targetState = stateKey,
        transitionSpec = {
            fadeIn(tween(Duration.normal, easing = Easing.standard)) togetherWith
                fadeOut(tween(Duration.normal, easing = Easing.standard))
        },
        modifier = modifier,
        label = "detailBody",
    ) { state ->
        when (state) {
            "loading" -> LoadingStateOverlay()
            "error" -> {
                ErrorStateView(
                    message = uiState.error ?: "Unknown error",
                    onRetry = onRetry,
                )
            }
            else -> {
                if (model != null) {
                    ModelDetailContentBody(
                        model = model,
                        uiState = uiState,
                        onVersionSelected = onVersionSelected,
                        onViewImages = onViewImages,
                        onCreatorClick = onCreatorClick,
                        onTryInComfyUI = onTryInComfyUI,
                        onSendToPC = onSendToPC,
                        onSaveNote = onSaveNote,
                        onAddTag = onAddTag,
                        onRemoveTag = onRemoveTag,
                        onDownloadFile = onDownloadFile,
                        onCancelDownload = onCancelDownload,
                        onReviewSortChanged = onReviewSortChanged,
                        onWriteReview = onWriteReview,
                        bottomPadding = bottomPadding,
                        carouselContent = carouselContent,
                    )
                }
            }
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun ModelDetailContentBody(
    model: Model,
    uiState: ModelDetailUiState,
    onVersionSelected: (Int) -> Unit,
    onViewImages: (Long) -> Unit,
    onCreatorClick: (String) -> Unit,
    onTryInComfyUI:
    ((sha256: String, modelName: String, meta: com.riox432.civitdeck.domain.model.ImageGenerationMeta?) -> Unit)?,
    onSendToPC: () -> Unit = {},
    onSaveNote: (String) -> Unit = {},
    onAddTag: (String) -> Unit = {},
    onRemoveTag: (String) -> Unit = {},
    onDownloadFile: (ModelFile) -> Unit = {},
    onCancelDownload: (Long) -> Unit = {},
    onReviewSortChanged: (com.riox432.civitdeck.domain.model.ReviewSortOrder) -> Unit = {},
    onWriteReview: () -> Unit = {},
    bottomPadding: androidx.compose.ui.unit.Dp,
    carouselContent: @Composable () -> Unit = {},
) {
    val selectedVersion = model.modelVersions.getOrNull(uiState.selectedVersionIndex)
    val images = (selectedVersion?.images ?: emptyList()).let { allImages ->
        allImages.filterByNsfwLevel(uiState.nsfwFilterLevel)
    }

    if (selectedVersion == null) {
        EmptyStateMessage(
            icon = Icons.Outlined.Info,
            title = "Version not available",
            subtitle = "The selected version is no longer available.",
            modifier = Modifier.fillMaxSize(),
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomPadding + Spacing.lg),
    ) {
        modelDetailItems(
            model = model,
            uiState = uiState,
            selectedVersion = selectedVersion,
            images = images,
            onVersionSelected = onVersionSelected,
            onViewImages = onViewImages,
            onCreatorClick = onCreatorClick,
            onTryInComfyUI = onTryInComfyUI,
            onSendToPC = onSendToPC,
            onSaveNote = onSaveNote,
            onAddTag = onAddTag,
            onRemoveTag = onRemoveTag,
            onDownloadFile = onDownloadFile,
            onCancelDownload = onCancelDownload,
            onReviewSortChanged = onReviewSortChanged,
            onWriteReview = onWriteReview,
            carouselContent = carouselContent,
        )
    }
}

@Suppress("LongParameterList")
private fun LazyListScope.modelDetailItems(
    model: Model,
    uiState: ModelDetailUiState,
    selectedVersion: ModelVersion,
    images: List<ModelImage>,
    onVersionSelected: (Int) -> Unit,
    onViewImages: (Long) -> Unit,
    onCreatorClick: (String) -> Unit,
    onTryInComfyUI:
    ((sha256: String, modelName: String, meta: com.riox432.civitdeck.domain.model.ImageGenerationMeta?) -> Unit)?,
    onSendToPC: () -> Unit = {},
    onSaveNote: (String) -> Unit = {},
    onAddTag: (String) -> Unit = {},
    onRemoveTag: (String) -> Unit = {},
    onDownloadFile: (ModelFile) -> Unit = {},
    onCancelDownload: (Long) -> Unit = {},
    onReviewSortChanged: (com.riox432.civitdeck.domain.model.ReviewSortOrder) -> Unit = {},
    onWriteReview: () -> Unit = {},
    carouselContent: @Composable () -> Unit,
) {
    item { carouselContent() }
    item { ModelHeader(model = model, onCreatorClick = onCreatorClick) }
    item {
        ModelStatsRow(
            downloadCount = model.stats.downloadCount,
            favoriteCount = model.stats.favoriteCount,
            rating = model.stats.rating,
            commentCount = model.stats.commentCount,
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        )
    }
    modelDetailActionItems(
        model = model,
        uiState = uiState,
        selectedVersion = selectedVersion,
        images = images,
        onViewImages = onViewImages,
        onTryInComfyUI = onTryInComfyUI,
        onSendToPC = onSendToPC,
        onSaveNote = onSaveNote,
        onAddTag = onAddTag,
        onRemoveTag = onRemoveTag,
        onReviewSortChanged = onReviewSortChanged,
        onWriteReview = onWriteReview,
        onVersionSelected = onVersionSelected,
        onDownloadFile = onDownloadFile,
        onCancelDownload = onCancelDownload,
    )
}

@Suppress("LongParameterList")
private fun LazyListScope.modelDetailActionItems(
    model: Model,
    uiState: ModelDetailUiState,
    selectedVersion: ModelVersion,
    images: List<ModelImage>,
    onViewImages: (Long) -> Unit,
    onTryInComfyUI:
    ((sha256: String, modelName: String, meta: com.riox432.civitdeck.domain.model.ImageGenerationMeta?) -> Unit)?,
    onSendToPC: () -> Unit,
    onSaveNote: (String) -> Unit,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    onReviewSortChanged: (com.riox432.civitdeck.domain.model.ReviewSortOrder) -> Unit,
    onWriteReview: () -> Unit,
    onVersionSelected: (Int) -> Unit,
    onDownloadFile: (ModelFile) -> Unit,
    onCancelDownload: (Long) -> Unit,
) {
    item {
        val primaryFile = selectedVersion.files.firstOrNull { it.primary }
            ?: selectedVersion.files.firstOrNull()
        val sha256 = primaryFile?.hashes?.get("SHA256") ?: primaryFile?.hashes?.get("sha256")
        val sampleMeta = images.firstOrNull()?.meta
        ImageActionsRow(
            onViewImages = { onViewImages(selectedVersion.id) },
            showTryInComfyUI = onTryInComfyUI != null,
            onTryInComfyUI = {
                if (onTryInComfyUI != null && sha256 != null) {
                    onTryInComfyUI(sha256, model.name, sampleMeta)
                }
            },
            onSendToPC = onSendToPC,
        )
    }
    if (model.tags.isNotEmpty()) { item { TagsSection(tags = model.tags) } }
    item { ModelNotesSection(note = uiState.note, onSaveNote = onSaveNote) }
    item {
        PersonalTagsSection(
            tags = uiState.personalTags,
            onAddTag = onAddTag,
            onRemoveTag = onRemoveTag,
        )
    }
    item {
        ReviewsSection(
            reviews = uiState.reviews,
            ratingTotals = uiState.ratingTotals,
            sortOrder = uiState.reviewSortOrder,
            isLoading = uiState.isReviewsLoading,
            onSortChanged = onReviewSortChanged,
            onWriteReview = onWriteReview,
        )
    }
    if (!model.description.isNullOrBlank()) {
        item { DescriptionSection(description = model.description!!) }
    }
    if (model.modelVersions.size > 1) {
        item {
            VersionSelector(
                versions = model.modelVersions,
                selectedIndex = uiState.selectedVersionIndex,
                onVersionSelected = onVersionSelected,
            )
        }
    }
    item {
        VersionDetail(
            version = selectedVersion,
            powerUserMode = uiState.powerUserMode,
            downloads = uiState.downloads.associateBy { it.fileId },
            onDownloadFile = onDownloadFile,
            onCancelDownload = onCancelDownload,
        )
    }
}
