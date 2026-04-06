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

/**
 * Groups callback parameters for model detail composables to reduce parameter count.
 */
data class ModelDetailCallbacks(
    val onRetry: () -> Unit,
    val onVersionSelected: (Int) -> Unit,
    val onViewImages: (Long) -> Unit,
    val onCreatorClick: (String) -> Unit,
    val onTryInComfyUI:
    ((sha256: String, modelName: String, meta: com.riox432.civitdeck.domain.model.ImageGenerationMeta?) -> Unit)?,
    val onSendToPC: () -> Unit = {},
    val onSaveNote: (String) -> Unit = {},
    val onAddTag: (String) -> Unit = {},
    val onRemoveTag: (String) -> Unit = {},
    val onDownloadFile: (ModelFile) -> Unit = {},
    val onCancelDownload: (Long) -> Unit = {},
    val onReviewSortChanged: (com.riox432.civitdeck.domain.model.ReviewSortOrder) -> Unit = {},
    val onWriteReview: () -> Unit = {},
)

@Composable
internal fun DetailStateContent(
    uiState: ModelDetailUiState,
    model: Model?,
    callbacks: ModelDetailCallbacks,
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
                    onRetry = callbacks.onRetry,
                )
            }
            else -> {
                if (model != null) {
                    ModelDetailContentBody(
                        model = model,
                        uiState = uiState,
                        callbacks = callbacks,
                        bottomPadding = bottomPadding,
                        carouselContent = carouselContent,
                    )
                }
            }
        }
    }
}

@Composable
private fun ModelDetailContentBody(
    model: Model,
    uiState: ModelDetailUiState,
    callbacks: ModelDetailCallbacks,
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
            callbacks = callbacks,
            carouselContent = carouselContent,
        )
    }
}

private fun LazyListScope.modelDetailItems(
    model: Model,
    uiState: ModelDetailUiState,
    selectedVersion: ModelVersion,
    images: List<ModelImage>,
    callbacks: ModelDetailCallbacks,
    carouselContent: @Composable () -> Unit,
) {
    item { carouselContent() }
    item { ModelHeader(model = model, onCreatorClick = callbacks.onCreatorClick) }
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
        callbacks = callbacks,
    )
}

private fun LazyListScope.modelDetailActionItems(
    model: Model,
    uiState: ModelDetailUiState,
    selectedVersion: ModelVersion,
    images: List<ModelImage>,
    callbacks: ModelDetailCallbacks,
) {
    item {
        val primaryFile = selectedVersion.files.firstOrNull { it.primary }
            ?: selectedVersion.files.firstOrNull()
        val sha256 = primaryFile?.hashes?.get("SHA256") ?: primaryFile?.hashes?.get("sha256")
        val sampleMeta = images.firstOrNull()?.meta
        ImageActionsRow(
            onViewImages = { callbacks.onViewImages(selectedVersion.id) },
            showTryInComfyUI = callbacks.onTryInComfyUI != null,
            onTryInComfyUI = {
                if (callbacks.onTryInComfyUI != null && sha256 != null) {
                    callbacks.onTryInComfyUI.invoke(sha256, model.name, sampleMeta)
                }
            },
            onSendToPC = callbacks.onSendToPC,
        )
    }
    if (model.tags.isNotEmpty()) { item { TagsSection(tags = model.tags) } }
    item { ModelNotesSection(note = uiState.note, onSaveNote = callbacks.onSaveNote) }
    item {
        PersonalTagsSection(
            tags = uiState.personalTags,
            onAddTag = callbacks.onAddTag,
            onRemoveTag = callbacks.onRemoveTag,
        )
    }
    item {
        ReviewsSection(
            reviews = uiState.reviews,
            ratingTotals = uiState.ratingTotals,
            sortOrder = uiState.reviewSortOrder,
            isLoading = uiState.isReviewsLoading,
            onSortChanged = callbacks.onReviewSortChanged,
            onWriteReview = callbacks.onWriteReview,
        )
    }
    model.description?.takeIf { it.isNotBlank() }?.let { description ->
        item { DescriptionSection(description = description) }
    }
    if (model.modelVersions.size > 1) {
        item {
            VersionSelector(
                versions = model.modelVersions,
                selectedIndex = uiState.selectedVersionIndex,
                onVersionSelected = callbacks.onVersionSelected,
            )
        }
    }
    item {
        VersionDetail(
            version = selectedVersion,
            powerUserMode = uiState.powerUserMode,
            downloads = uiState.downloads.associateBy { it.fileId },
            onDownloadFile = callbacks.onDownloadFile,
            onCancelDownload = callbacks.onCancelDownload,
        )
    }
}
