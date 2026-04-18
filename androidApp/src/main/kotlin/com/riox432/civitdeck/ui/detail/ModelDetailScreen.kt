package com.riox432.civitdeck.ui.detail

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.HapticFeedbackType
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelImage
import com.riox432.civitdeck.domain.model.ShareHashtag
import com.riox432.civitdeck.domain.model.filterByNsfwLevel
import com.riox432.civitdeck.domain.model.stripCdnWidth
import com.riox432.civitdeck.download.DownloadScheduler
import com.riox432.civitdeck.feature.detail.presentation.ModelDetailUiState
import com.riox432.civitdeck.feature.detail.presentation.ModelDetailViewModel
import com.riox432.civitdeck.ui.collections.AddToCollectionSheet
import com.riox432.civitdeck.ui.components.rememberHapticFeedback
import com.riox432.civitdeck.ui.qrcode.QRCodeSheet
import com.riox432.civitdeck.ui.share.SocialShareSheet

@Composable
@Suppress("LongParameterList", "LongMethod")
fun ModelDetailScreen(
    viewModel: ModelDetailViewModel,
    modelId: Long,
    initialThumbnailUrl: String?,
    onBack: () -> Unit,
    onViewImages: (Long) -> Unit = {},
    onCreatorClick: (String) -> Unit = {},
    onTryInComfyUI:
    (
        (
            sha256: String,
            modelName: String,
            meta: com.riox432.civitdeck.domain.model.ImageGenerationMeta?
        ) -> Unit
    )? = null,
    sharedElementSuffix: String = "",
    shareHashtags: List<ShareHashtag> = emptyList(),
    onToggleShareHashtag: (String, Boolean) -> Unit = { _, _ -> },
    onAddShareHashtag: (String) -> Unit = {},
    onRemoveShareHashtag: (String) -> Unit = {},
    onFindSimilar: ((Long) -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val modelCollectionIds by viewModel.modelCollectionIds.collectAsStateWithLifecycle()
    var showCollectionSheet by remember { mutableStateOf(false) }
    var showSendToPCSheet by remember { mutableStateOf(false) }
    var showQRCodeSheet by remember { mutableStateOf(false) }
    var showSubmitReviewSheet by remember { mutableStateOf(false) }
    var showShareSheet by remember { mutableStateOf(false) }
    val haptic = rememberHapticFeedback()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.downloadEnqueuedEvent.collect { downloadId ->
            DownloadScheduler.enqueue(context, downloadId)
        }
    }

    val detailCallbacks = remember(viewModel, onViewImages, onCreatorClick, onTryInComfyUI) {
        ModelDetailCallbacks(
            onRetry = viewModel::retry,
            onVersionSelected = viewModel::onVersionSelected,
            onViewImages = onViewImages,
            onCreatorClick = onCreatorClick,
            onTryInComfyUI = onTryInComfyUI,
            onSendToPC = { showSendToPCSheet = true },
            onSaveNote = viewModel::saveNote,
            onAddTag = viewModel::addTag,
            onRemoveTag = viewModel::removeTag,
            onDownloadFile = viewModel::downloadFile,
            onCancelDownload = viewModel::cancelDownload,
            onReviewSortChanged = viewModel::onReviewSortChanged,
            onWriteReview = { showSubmitReviewSheet = true },
        )
    }

    ModelDetailScaffold(
        uiState = uiState,
        viewModel = viewModel,
        modelId = modelId,
        initialThumbnailUrl = initialThumbnailUrl,
        sharedElementSuffix = sharedElementSuffix,
        haptic = haptic,
        onBack = onBack,
        detailCallbacks = detailCallbacks,
        onShowCollectionSheet = { showCollectionSheet = true },
        onShowQRCodeSheet = { showQRCodeSheet = true },
        onShowShareSheet = { showShareSheet = true },
        onFindSimilar = onFindSimilar,
    )

    ReviewSubmitHandler(
        uiState = uiState,
        viewModel = viewModel,
        showSubmitReviewSheet = showSubmitReviewSheet,
        onDismissSubmitReview = { showSubmitReviewSheet = false },
    )

    ModelDetailSheets(
        showSendToPCSheet = showSendToPCSheet,
        onDismissSendToPC = { showSendToPCSheet = false },
        uiState = uiState,
        showCollectionSheet = showCollectionSheet,
        onDismissCollection = { showCollectionSheet = false },
        collections = collections,
        modelCollectionIds = modelCollectionIds,
        viewModel = viewModel,
        showQRCodeSheet = showQRCodeSheet,
        onDismissQRCode = { showQRCodeSheet = false },
        modelId = modelId,
    )

    if (showShareSheet) {
        SocialShareSheet(
            hashtags = shareHashtags,
            onToggleHashtag = onToggleShareHashtag,
            onAddHashtag = onAddShareHashtag,
            onRemoveHashtag = onRemoveShareHashtag,
            onDismiss = { showShareSheet = false },
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun ModelDetailScaffold(
    uiState: ModelDetailUiState,
    viewModel: ModelDetailViewModel,
    modelId: Long,
    initialThumbnailUrl: String?,
    sharedElementSuffix: String,
    haptic: (HapticFeedbackType) -> Unit,
    onBack: () -> Unit,
    detailCallbacks: ModelDetailCallbacks,
    onShowCollectionSheet: () -> Unit,
    onShowQRCodeSheet: () -> Unit,
    onShowShareSheet: () -> Unit = {},
    onFindSimilar: ((Long) -> Unit)? = null,
) {
    Scaffold(
        topBar = {
            ModelDetailTopBar(
                uiState = uiState,
                onBack = onBack,
                onFavoriteToggle = {
                    haptic(HapticFeedbackType.Impact)
                    viewModel.onFavoriteToggle()
                },
                onAddToCollection = onShowCollectionSheet,
                onShowQRCode = onShowQRCodeSheet,
                onShareClick = onShowShareSheet,
                onFindSimilar = if (onFindSimilar != null) {
                    { onFindSimilar(modelId) }
                } else {
                    null
                },
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        ModelDetailBody(
            uiState = uiState,
            modelId = modelId,
            initialThumbnailUrl = initialThumbnailUrl,
            sharedElementSuffix = sharedElementSuffix,
            detailCallbacks = detailCallbacks,
            contentPadding = padding,
        )
    }
}

@Composable
private fun ReviewSubmitHandler(
    uiState: ModelDetailUiState,
    viewModel: ModelDetailViewModel,
    showSubmitReviewSheet: Boolean,
    onDismissSubmitReview: () -> Unit,
) {
    if (showSubmitReviewSheet) {
        SubmitReviewSheet(
            isSubmitting = uiState.isSubmittingReview,
            onSubmit = { rating, recommended, details ->
                val versionId = uiState.model?.modelVersions
                    ?.getOrNull(uiState.selectedVersionIndex)?.id ?: return@SubmitReviewSheet
                viewModel.submitReview(versionId, rating, recommended, details)
            },
            onDismiss = onDismissSubmitReview,
        )
    }

    LaunchedEffect(uiState.reviewSubmitSuccess) {
        if (uiState.reviewSubmitSuccess) {
            onDismissSubmitReview()
            viewModel.dismissReviewSuccess()
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun ModelDetailSheets(
    showSendToPCSheet: Boolean,
    onDismissSendToPC: () -> Unit,
    uiState: ModelDetailUiState,
    showCollectionSheet: Boolean,
    onDismissCollection: () -> Unit,
    collections: List<com.riox432.civitdeck.domain.model.ModelCollection>,
    modelCollectionIds: List<Long>,
    viewModel: ModelDetailViewModel,
    showQRCodeSheet: Boolean,
    onDismissQRCode: () -> Unit,
    modelId: Long,
) {
    if (showSendToPCSheet) {
        CivitaiLinkSendSheet(
            model = uiState.model,
            selectedVersionIndex = uiState.selectedVersionIndex,
            onDismiss = onDismissSendToPC,
        )
    }

    if (showCollectionSheet) {
        AddToCollectionSheet(
            collections = collections,
            modelCollectionIds = modelCollectionIds,
            onToggleCollection = viewModel::toggleCollection,
            onCreateCollection = viewModel::createCollectionAndAdd,
            onDismiss = onDismissCollection,
        )
    }

    if (showQRCodeSheet) {
        QRCodeSheet(
            modelId = modelId,
            modelName = uiState.model?.name ?: "",
            onDismiss = onDismissQRCode,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelDetailTopBar(
    uiState: ModelDetailUiState,
    onBack: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onAddToCollection: () -> Unit,
    onShowQRCode: () -> Unit,
    onShareClick: () -> Unit,
    onFindSimilar: (() -> Unit)? = null,
) {
    TopAppBar(
        windowInsets = WindowInsets(0, 0, 0, 0),
        title = {
            Text(
                text = uiState.model?.name ?: "",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_navigate_back)
                )
            }
        },
        actions = {
            // Primary actions: Favorite and Share
            IconButton(onClick = onFavoriteToggle) {
                Icon(
                    imageVector = if (uiState.isFavorite) {
                        Icons.Default.Favorite
                    } else {
                        Icons.Default.FavoriteBorder
                    },
                    contentDescription = stringResource(R.string.cd_favorite),
                    tint = if (uiState.isFavorite) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
            IconButton(onClick = onShareClick) {
                Icon(Icons.Default.Share, contentDescription = stringResource(R.string.cd_share))
            }
            // Secondary actions in overflow menu
            DetailOverflowMenu(
                onFindSimilar = onFindSimilar,
                onAddToCollection = onAddToCollection,
                onShowQRCode = onShowQRCode,
            )
        },
    )
}

@Composable
private fun DetailOverflowMenu(
    onFindSimilar: (() -> Unit)?,
    onAddToCollection: () -> Unit,
    onShowQRCode: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = "More options")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (onFindSimilar != null) {
                DropdownMenuItem(
                    text = { Text("Find similar") },
                    leadingIcon = { Icon(Icons.Default.ImageSearch, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onFindSimilar()
                    },
                )
            }
            DropdownMenuItem(
                text = { Text("Add to collection") },
                leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
                onClick = {
                    expanded = false
                    onAddToCollection()
                },
            )
            DropdownMenuItem(
                text = { Text("QR code") },
                leadingIcon = { Icon(Icons.Default.QrCode2, contentDescription = null) },
                onClick = {
                    expanded = false
                    onShowQRCode()
                },
            )
        }
    }
}

private fun prepareImages(
    allImages: List<ModelImage>?,
    uiState: ModelDetailUiState,
    initialThumbnailUrl: String?,
): List<ModelImage> {
    val filtered = (allImages ?: emptyList()).filterByNsfwLevel(uiState.nsfwFilterLevel)
    if (initialThumbnailUrl == null || uiState.selectedVersionIndex != 0) return filtered
    val normalizedThumbnail = initialThumbnailUrl.stripCdnWidth()
    val idx = filtered.indexOfFirst { it.url.stripCdnWidth() == normalizedThumbnail }
    if (idx <= 0) return filtered
    return listOf(filtered[idx]) + filtered.subList(0, idx) + filtered.subList(idx + 1, filtered.size)
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ModelDetailBody(
    uiState: ModelDetailUiState,
    modelId: Long,
    initialThumbnailUrl: String?,
    sharedElementSuffix: String,
    detailCallbacks: ModelDetailCallbacks,
    contentPadding: PaddingValues,
) {
    val model = uiState.model
    val selectedVersion = model?.modelVersions?.getOrNull(uiState.selectedVersionIndex)
    val allImages = prepareImages(selectedVersion?.images, uiState, initialThumbnailUrl)
    var failedImageUrls by remember { mutableStateOf(emptySet<String>()) }
    val images = allImages.filter { it.url !in failedImageUrls }
    var selectedCarouselIndex by remember { mutableStateOf<Int?>(null) }
    var showImageGrid by remember { mutableStateOf(false) }
    var gridSelectedIndex by remember { mutableStateOf<Int?>(null) }

    ModelDetailBodyLayout(
        uiState = uiState, model = model, modelId = modelId,
        initialThumbnailUrl = initialThumbnailUrl, sharedElementSuffix = sharedElementSuffix,
        images = images, contentPadding = contentPadding,
        detailCallbacks = detailCallbacks,
        onImageClick = { selectedCarouselIndex = it },
        onImageError = { url -> failedImageUrls = failedImageUrls + url },
        onShowGrid = { showImageGrid = true },
    )

    DetailOverlays(
        images = images,
        selectedCarouselIndex = selectedCarouselIndex,
        onDismissCarousel = { selectedCarouselIndex = null },
        showImageGrid = showImageGrid,
        onDismissGrid = { showImageGrid = false },
        onGridImageClick = { gridSelectedIndex = it },
        gridSelectedIndex = gridSelectedIndex,
        onDismissGridViewer = { gridSelectedIndex = null },
    )
}

@Suppress("LongParameterList")
@Composable
private fun ModelDetailBodyLayout(
    uiState: ModelDetailUiState,
    model: Model?,
    modelId: Long,
    initialThumbnailUrl: String?,
    sharedElementSuffix: String,
    images: List<ModelImage>,
    contentPadding: PaddingValues,
    detailCallbacks: ModelDetailCallbacks,
    onImageClick: (Int) -> Unit,
    onImageError: (String) -> Unit,
    onShowGrid: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding()),
    ) {
        if (model == null && initialThumbnailUrl != null) {
            SharedThumbnailPlaceholder(
                thumbnailUrl = initialThumbnailUrl,
                modelId = modelId,
                sharedElementSuffix = sharedElementSuffix,
            )
        }
        DetailStateContent(
            uiState = uiState,
            model = model,
            callbacks = detailCallbacks,
            bottomPadding = contentPadding.calculateBottomPadding(),
            modifier = Modifier.weight(1f),
            carouselContent = {
                CarouselWithGridButton(
                    images = images,
                    modelId = modelId,
                    sharedElementSuffix = sharedElementSuffix,
                    onImageClick = onImageClick,
                    onImageError = onImageError,
                    onShowGrid = onShowGrid,
                )
            },
        )
    }
}
