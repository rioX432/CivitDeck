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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.CivitaiLinkStatus
import com.riox432.civitdeck.domain.model.HapticFeedbackType
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelImage
import com.riox432.civitdeck.domain.model.ShareHashtag
import com.riox432.civitdeck.domain.model.filterByNsfwLevel
import com.riox432.civitdeck.domain.model.stripCdnWidth
import com.riox432.civitdeck.download.DownloadScheduler
import com.riox432.civitdeck.feature.comfyui.presentation.CivitaiLinkSendViewModel
import com.riox432.civitdeck.feature.detail.presentation.ModelDetailUiState
import com.riox432.civitdeck.feature.detail.presentation.ModelDetailViewModel
import com.riox432.civitdeck.ui.collections.AddToCollectionSheet
import com.riox432.civitdeck.ui.components.rememberHapticFeedback
import com.riox432.civitdeck.ui.qrcode.QRCodeSheet
import com.riox432.civitdeck.ui.share.SocialShareSheet
import com.riox432.civitdeck.ui.testing.DiscoveryTestTags
import org.koin.compose.viewmodel.koinViewModel

// Compose UI: state/callback params are an intrinsic UI contract; a param object only hides them.
@Suppress("LongParameterList")
@Composable
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
    var showShareSheet by remember { mutableStateOf(false) }
    val haptic = rememberHapticFeedback()

    // Demote CV2 send: only expose "Send to PC" when a Civitai Link connection is active.
    val civitaiLinkViewModel: CivitaiLinkSendViewModel = koinViewModel()
    val civitaiLinkStatus by civitaiLinkViewModel.status.collectAsStateWithLifecycle()
    val canSendToPC = civitaiLinkStatus == CivitaiLinkStatus.Connected

    DownloadEnqueueEffect(viewModel)

    val detailCallbacks = rememberModelDetailCallbacks(
        viewModel = viewModel,
        onViewImages = onViewImages,
        onCreatorClick = onCreatorClick,
        onTryInComfyUI = onTryInComfyUI,
        onSendToPC = { showSendToPCSheet = true },
        canSendToPC = canSendToPC,
    )

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

    ModelDetailOverlays(
        uiState = uiState,
        viewModel = viewModel,
        modelId = modelId,
        collections = collections,
        modelCollectionIds = modelCollectionIds,
        sheetVisibility = ModelDetailSheetVisibility(
            showSendToPCSheet = showSendToPCSheet,
            showCollectionSheet = showCollectionSheet,
            showQRCodeSheet = showQRCodeSheet,
            showShareSheet = showShareSheet,
        ),
        onDismissSendToPC = { showSendToPCSheet = false },
        onDismissCollection = { showCollectionSheet = false },
        onDismissQRCode = { showQRCodeSheet = false },
        onDismissShare = { showShareSheet = false },
        shareHashtags = shareHashtags,
        onToggleShareHashtag = onToggleShareHashtag,
        onAddShareHashtag = onAddShareHashtag,
        onRemoveShareHashtag = onRemoveShareHashtag,
    )
}

private data class ModelDetailSheetVisibility(
    val showSendToPCSheet: Boolean,
    val showCollectionSheet: Boolean,
    val showQRCodeSheet: Boolean,
    val showShareSheet: Boolean,
)

@Composable
// Compose UI: state/callback params are an intrinsic UI contract; a param object only hides them.
@Suppress("LongParameterList")
private fun ModelDetailOverlays(
    uiState: ModelDetailUiState,
    viewModel: ModelDetailViewModel,
    modelId: Long,
    collections: List<com.riox432.civitdeck.domain.model.ModelCollection>,
    modelCollectionIds: List<Long>,
    sheetVisibility: ModelDetailSheetVisibility,
    onDismissSendToPC: () -> Unit,
    onDismissCollection: () -> Unit,
    onDismissQRCode: () -> Unit,
    onDismissShare: () -> Unit,
    shareHashtags: List<ShareHashtag>,
    onToggleShareHashtag: (String, Boolean) -> Unit,
    onAddShareHashtag: (String) -> Unit,
    onRemoveShareHashtag: (String) -> Unit,
) {
    ModelDetailSheets(
        showSendToPCSheet = sheetVisibility.showSendToPCSheet,
        onDismissSendToPC = onDismissSendToPC,
        uiState = uiState,
        showCollectionSheet = sheetVisibility.showCollectionSheet,
        onDismissCollection = onDismissCollection,
        collections = collections,
        modelCollectionIds = modelCollectionIds,
        viewModel = viewModel,
        showQRCodeSheet = sheetVisibility.showQRCodeSheet,
        onDismissQRCode = onDismissQRCode,
        modelId = modelId,
    )

    if (sheetVisibility.showShareSheet) {
        SocialShareSheet(
            hashtags = shareHashtags,
            onToggleHashtag = onToggleShareHashtag,
            onAddHashtag = onAddShareHashtag,
            onRemoveHashtag = onRemoveShareHashtag,
            onDismiss = onDismissShare,
        )
    }
}

@Composable
private fun DownloadEnqueueEffect(viewModel: ModelDetailViewModel) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.downloadEnqueuedEvent.collect { downloadId ->
            DownloadScheduler.enqueue(context, downloadId)
        }
    }
}

@Composable
private fun rememberModelDetailCallbacks(
    viewModel: ModelDetailViewModel,
    onViewImages: (Long) -> Unit,
    onCreatorClick: (String) -> Unit,
    onTryInComfyUI: (
        (
            sha256: String,
            modelName: String,
            meta: com.riox432.civitdeck.domain.model.ImageGenerationMeta?
        ) -> Unit
    )?,
    onSendToPC: () -> Unit,
    canSendToPC: Boolean,
): ModelDetailCallbacks = remember(viewModel, onViewImages, onCreatorClick, onTryInComfyUI, canSendToPC) {
    ModelDetailCallbacks(
        onRetry = viewModel::retry,
        onVersionSelected = viewModel::onVersionSelected,
        onViewImages = onViewImages,
        onCreatorClick = onCreatorClick,
        onTryInComfyUI = onTryInComfyUI,
        onSendToPC = onSendToPC,
        canSendToPC = canSendToPC,
        onSaveNote = viewModel::saveNote,
        onAddTag = viewModel::addTag,
        onRemoveTag = viewModel::removeTag,
        onDownloadFile = viewModel::downloadFile,
        onCancelDownload = viewModel::cancelDownload,
    )
}

@Composable
// Compose UI: state/callback params are an intrinsic UI contract; a param object only hides them.
@Suppress("LongParameterList")
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
        modifier = Modifier.testTag(DiscoveryTestTags.MODEL_DETAIL_ROOT),
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
// Compose UI: state/callback params are an intrinsic UI contract; a param object only hides them.
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
            // Primary action: Favorite. Peripheral actions (share, QR) live in the overflow menu.
            IconButton(
                onClick = onFavoriteToggle,
                modifier = Modifier.testTag(DiscoveryTestTags.MODEL_FAVORITE_BUTTON),
            ) {
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
            // Secondary actions in overflow menu
            DetailOverflowMenu(
                onFindSimilar = onFindSimilar,
                onAddToCollection = onAddToCollection,
                onShowQRCode = onShowQRCode,
                onShareClick = onShareClick,
            )
        },
    )
}

@Composable
private fun DetailOverflowMenu(
    onFindSimilar: (() -> Unit)?,
    onAddToCollection: () -> Unit,
    onShowQRCode: () -> Unit,
    onShareClick: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.cd_more_options),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (onFindSimilar != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.detail_find_similar)) },
                    leadingIcon = { Icon(Icons.Default.ImageSearch, contentDescription = null) },
                    onClick = {
                        expanded = false
                        onFindSimilar()
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.detail_add_to_collection)) },
                leadingIcon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null) },
                onClick = {
                    expanded = false
                    onAddToCollection()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.detail_share)) },
                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                onClick = {
                    expanded = false
                    onShareClick()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.detail_qr_code)) },
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

@Composable
// Compose UI: state/callback params are an intrinsic UI contract; a param object only hides them.
@Suppress("LongParameterList")
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
