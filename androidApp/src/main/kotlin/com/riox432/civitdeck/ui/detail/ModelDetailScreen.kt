package com.riox432.civitdeck.ui.detail

import android.content.Intent
import android.text.Html
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.model.ModelFile
import com.riox432.civitdeck.domain.model.ModelImage
import com.riox432.civitdeck.domain.model.ModelVersion
import com.riox432.civitdeck.domain.model.filterByNsfwLevel
import com.riox432.civitdeck.domain.model.stripCdnWidth
import com.riox432.civitdeck.ui.adaptive.adaptiveGridColumns
import com.riox432.civitdeck.ui.components.ImageErrorPlaceholder
import com.riox432.civitdeck.ui.gallery.ImageViewerOverlay
import com.riox432.civitdeck.ui.gallery.ViewerImage
import com.riox432.civitdeck.ui.navigation.LocalSharedTransitionScope
import com.riox432.civitdeck.ui.navigation.SharedElementKeys
import com.riox432.civitdeck.ui.theme.CornerRadius
import com.riox432.civitdeck.ui.theme.Duration
import com.riox432.civitdeck.ui.theme.Easing
import com.riox432.civitdeck.ui.theme.Spacing
import com.riox432.civitdeck.ui.theme.shimmer
import com.riox432.civitdeck.util.FormatUtils

@Composable
fun ModelDetailScreen(
    viewModel: ModelDetailViewModel,
    modelId: Long,
    initialThumbnailUrl: String?,
    onBack: () -> Unit,
    onViewImages: (Long) -> Unit = {},
    onCreatorClick: (String) -> Unit = {},
    sharedElementSuffix: String = "",
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            ModelDetailTopBar(
                uiState = uiState,
                onBack = onBack,
                onFavoriteToggle = viewModel::onFavoriteToggle,
            )
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        ModelDetailBody(
            uiState = uiState,
            modelId = modelId,
            initialThumbnailUrl = initialThumbnailUrl,
            sharedElementSuffix = sharedElementSuffix,
            onRetry = viewModel::retry,
            onVersionSelected = viewModel::onVersionSelected,
            onViewImages = onViewImages,
            onCreatorClick = onCreatorClick,
            contentPadding = padding,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelDetailTopBar(
    uiState: ModelDetailUiState,
    onBack: () -> Unit,
    onFavoriteToggle: () -> Unit,
) {
    val context = LocalContext.current
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            IconButton(
                onClick = {
                    val model = uiState.model ?: return@IconButton
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "https://civitai.com/models/${model.id}")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share model"))
                },
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share")
            }
            IconButton(onClick = onFavoriteToggle) {
                Icon(
                    imageVector = if (uiState.isFavorite) {
                        Icons.Default.Favorite
                    } else {
                        Icons.Default.FavoriteBorder
                    },
                    contentDescription = "Favorite",
                    tint = if (uiState.isFavorite) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        },
    )
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
    onRetry: () -> Unit,
    onVersionSelected: (Int) -> Unit,
    onViewImages: (Long) -> Unit,
    onCreatorClick: (String) -> Unit,
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding()),
    ) {
        when {
            model != null -> {
                ImageCarousel(
                    images = images,
                    modelId = modelId,
                    sharedElementSuffix = sharedElementSuffix,
                    onImageClick = { selectedCarouselIndex = it },
                    onImageError = { url -> failedImageUrls = failedImageUrls + url },
                )
            }
            initialThumbnailUrl != null -> {
                SharedThumbnailPlaceholder(
                    thumbnailUrl = initialThumbnailUrl,
                    modelId = modelId,
                    sharedElementSuffix = sharedElementSuffix,
                )
            }
        }
        DetailStateContent(
            uiState = uiState,
            model = model,
            onRetry = onRetry,
            onVersionSelected = onVersionSelected,
            onViewImages = onViewImages,
            onCreatorClick = onCreatorClick,
            onShowImageGrid = { showImageGrid = true },
            bottomPadding = contentPadding.calculateBottomPadding(),
            modifier = Modifier.weight(1f),
        )
    }

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
private fun DetailOverlays(
    images: List<ModelImage>,
    selectedCarouselIndex: Int?,
    onDismissCarousel: () -> Unit,
    showImageGrid: Boolean,
    onDismissGrid: () -> Unit,
    onGridImageClick: (Int) -> Unit,
    gridSelectedIndex: Int?,
    onDismissGridViewer: () -> Unit,
) {
    if (selectedCarouselIndex != null && images.isNotEmpty()) {
        ImageViewerOverlay(
            images = images.map { ViewerImage(url = it.url, meta = it.meta) },
            initialIndex = selectedCarouselIndex,
            onDismiss = onDismissCarousel,
        )
    }

    if (showImageGrid && images.isNotEmpty()) {
        ImageGridBottomSheet(
            images = images,
            onDismiss = onDismissGrid,
            onImageClick = onGridImageClick,
        )
    }

    if (gridSelectedIndex != null && images.isNotEmpty()) {
        ImageViewerOverlay(
            images = images.map { ViewerImage(url = it.url, meta = it.meta) },
            initialIndex = gridSelectedIndex,
            onDismiss = onDismissGridViewer,
        )
    }
}

@Composable
private fun DetailStateContent(
    uiState: ModelDetailUiState,
    model: Model?,
    onRetry: () -> Unit,
    onVersionSelected: (Int) -> Unit,
    onViewImages: (Long) -> Unit,
    onCreatorClick: (String) -> Unit,
    onShowImageGrid: () -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
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
            "loading" -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            "error" -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.error ?: "Unknown error",
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(modifier = Modifier.height(Spacing.lg))
                        Button(onClick = onRetry) {
                            Text("Retry")
                        }
                    }
                }
            }
            else -> {
                if (model != null) {
                    ModelDetailContentBody(
                        model = model,
                        uiState = uiState,
                        onVersionSelected = onVersionSelected,
                        onViewImages = onViewImages,
                        onCreatorClick = onCreatorClick,
                        onShowImageGrid = onShowImageGrid,
                        bottomPadding = bottomPadding,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun SharedThumbnailPlaceholder(
    thumbnailUrl: String,
    modelId: Long,
    sharedElementSuffix: String = "",
) {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedContentScope = LocalNavAnimatedContentScope.current

    val imageModifier = if (sharedTransitionScope != null) {
        with(sharedTransitionScope) {
            Modifier
                .fillMaxWidth()
                .aspectRatio(CAROUSEL_ASPECT_RATIO)
                .sharedElement(
                    rememberSharedContentState(
                        key = SharedElementKeys.modelThumbnail(modelId, sharedElementSuffix),
                    ),
                    animatedVisibilityScope = animatedContentScope,
                )
        }
    } else {
        Modifier
            .fillMaxWidth()
            .aspectRatio(CAROUSEL_ASPECT_RATIO)
    }

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(thumbnailUrl)
            .crossfade(Duration.normal)
            .build(),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = imageModifier
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(CAROUSEL_ASPECT_RATIO)
                    .shimmer(),
            )
        },
        error = {
            ImageErrorPlaceholder(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(CAROUSEL_ASPECT_RATIO),
            )
        },
    )
}

@Composable
private fun ModelDetailContentBody(
    model: Model,
    uiState: ModelDetailUiState,
    onVersionSelected: (Int) -> Unit,
    onViewImages: (Long) -> Unit,
    onCreatorClick: (String) -> Unit,
    onShowImageGrid: () -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp,
) {
    val selectedVersion = model.modelVersions.getOrNull(uiState.selectedVersionIndex)
    val images = (selectedVersion?.images ?: emptyList()).let { allImages ->
        allImages.filterByNsfwLevel(uiState.nsfwFilterLevel)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomPadding + Spacing.lg),
    ) {
        // Model header
        item {
            ModelHeader(model = model, onCreatorClick = onCreatorClick)
        }

        // Stats row
        item {
            StatsRow(model = model)
        }

        // View Images + Grid button
        item {
            if (selectedVersion != null) {
                ImageActionsRow(
                    onViewImages = { onViewImages(selectedVersion.id) },
                    onShowGrid = onShowImageGrid,
                    hasImages = images.isNotEmpty(),
                )
            }
        }

        // Tags
        if (model.tags.isNotEmpty()) {
            item {
                TagsSection(tags = model.tags)
            }
        }

        // Description
        if (!model.description.isNullOrBlank()) {
            item {
                DescriptionSection(description = model.description!!)
            }
        }

        // Version selector
        if (model.modelVersions.size > 1) {
            item {
                VersionSelector(
                    versions = model.modelVersions,
                    selectedIndex = uiState.selectedVersionIndex,
                    onVersionSelected = onVersionSelected,
                )
            }
        }

        // Version detail
        if (selectedVersion != null) {
            item {
                VersionDetail(version = selectedVersion)
            }
        }
    }
}

@Composable
private fun ImageActionsRow(
    onViewImages: () -> Unit,
    onShowGrid: () -> Unit,
    hasImages: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = onViewImages,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            ),
        ) {
            Text("View Community Images")
        }
        if (hasImages) {
            IconButton(onClick = onShowGrid) {
                Icon(
                    imageVector = Icons.Default.GridView,
                    contentDescription = "View version images as grid",
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ImageCarousel(
    images: List<ModelImage>,
    modelId: Long,
    sharedElementSuffix: String = "",
    onImageClick: (Int) -> Unit = {},
    onImageError: (String) -> Unit = {},
) {
    if (images.isEmpty()) return

    val pagerState = rememberPagerState { images.size }

    Column {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            CarouselPage(
                image = images[page],
                modelId = modelId,
                sharedElementSuffix = sharedElementSuffix,
                applySharedElement = page == pagerState.currentPage,
                onClick = { onImageClick(page) },
                onError = { onImageError(images[page].url) },
            )
        }

        if (images.size > 1) {
            Text(
                text = "${pagerState.currentPage + 1} / ${images.size}",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = Spacing.sm),
            )
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun CarouselPage(
    image: ModelImage,
    modelId: Long,
    sharedElementSuffix: String = "",
    applySharedElement: Boolean,
    onClick: () -> Unit = {},
    onError: () -> Unit = {},
) {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedContentScope = LocalNavAnimatedContentScope.current

    val pageModifier = if (applySharedElement && sharedTransitionScope != null) {
        with(sharedTransitionScope) {
            Modifier
                .fillMaxWidth()
                .aspectRatio(CAROUSEL_ASPECT_RATIO)
                .clip(MaterialTheme.shapes.medium)
                .sharedElement(
                    rememberSharedContentState(
                        key = SharedElementKeys.modelThumbnail(modelId, sharedElementSuffix),
                    ),
                    animatedVisibilityScope = animatedContentScope,
                )
        }
    } else {
        Modifier
            .fillMaxWidth()
            .aspectRatio(CAROUSEL_ASPECT_RATIO)
            .clip(MaterialTheme.shapes.medium)
    }

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(image.url)
            .crossfade(Duration.normal)
            .build(),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = pageModifier
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onClick),
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(CAROUSEL_ASPECT_RATIO)
                    .shimmer(),
            )
        },
        error = {
            LaunchedEffect(image.url) { onError() }
            ImageErrorPlaceholder(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(CAROUSEL_ASPECT_RATIO),
            )
        },
    )
}

@Composable
private fun ModelHeader(model: Model, onCreatorClick: (String) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md)) {
        Text(
            text = model.name,
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            SuggestionChip(
                onClick = {},
                label = {
                    Text(
                        text = model.type.name,
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
            )
            if (model.creator != null) {
                Text(
                    text = "by ${model.creator!!.username}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        onCreatorClick(model.creator!!.username)
                    },
                )
            }
        }
    }
}

@Composable
private fun StatsRow(model: Model) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        StatColumn(
            value = FormatUtils.formatCount(model.stats.downloadCount),
            label = "Downloads",
        )
        StatColumn(
            value = FormatUtils.formatCount(model.stats.favoriteCount),
            label = "Favorites",
        )
        StatColumn(
            value = FormatUtils.formatRating(model.stats.rating),
            label = "Rating",
        )
        StatColumn(
            value = FormatUtils.formatCount(model.stats.commentCount),
            label = "Comments",
        )
    }
}

@Composable
private fun StatColumn(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsSection(tags: List<String>) {
    Column(modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)) {
        Text(
            text = "Tags",
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            tags.forEach { tag ->
                SuggestionChip(
                    onClick = {},
                    label = { Text(tag, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }
    }
}

@Composable
private fun DescriptionSection(description: String) {
    var isExpanded by remember { mutableStateOf(false) }
    val plainText = remember(description) {
        Html.fromHtml(description, Html.FROM_HTML_MODE_COMPACT).toString()
    }

    Column(modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)) {
        HorizontalDivider(modifier = Modifier.padding(bottom = Spacing.sm))
        Text(
            text = "Description",
            style = MaterialTheme.typography.titleSmall,
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            text = AnnotatedString(plainText),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = if (isExpanded) Int.MAX_VALUE else DESCRIPTION_COLLAPSED_LINES,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.animateContentSize(
                animationSpec = tween(
                    durationMillis = Duration.normal,
                    easing = Easing.standard,
                ),
            ),
        )
        Text(
            text = if (isExpanded) "Show less" else "Show more",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = Spacing.xs),
        )
    }
}

private const val CAROUSEL_ASPECT_RATIO = 1f
private const val DESCRIPTION_COLLAPSED_LINES = 4

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageGridBottomSheet(
    images: List<ModelImage>,
    onDismiss: () -> Unit,
    onImageClick: (Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Text(
            text = "Version Images (${images.size})",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        )
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(adaptiveGridColumns()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalItemSpacing = Spacing.sm,
            contentPadding = PaddingValues(Spacing.sm),
        ) {
            itemsIndexed(images, key = { _, img -> img.url }) { index, image ->
                ImageGridItem(
                    image = image,
                    onClick = { onImageClick(index) },
                )
            }
        }
    }
}

@Composable
private fun ImageGridItem(
    image: ModelImage,
    onClick: () -> Unit,
) {
    val aspectRatio = if (image.width > 0 && image.height > 0) {
        image.width.toFloat() / image.height.toFloat()
    } else {
        1f
    }

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(image.url)
            .crossfade(Duration.normal)
            .build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(CornerRadius.image))
            .clickable(onClick = onClick),
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
                    .shimmer(),
            )
        },
        error = {
            ImageErrorPlaceholder(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio),
            )
        },
    )
}

@Composable
private fun VersionSelector(
    versions: List<ModelVersion>,
    selectedIndex: Int,
    onVersionSelected: (Int) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = Spacing.sm)) {
        HorizontalDivider(modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm))
        Text(
            text = "Versions",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = Spacing.lg),
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        LazyRow(
            contentPadding = PaddingValues(horizontal = Spacing.lg),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            itemsIndexed(versions) { index, version ->
                FilterChip(
                    selected = index == selectedIndex,
                    onClick = { onVersionSelected(index) },
                    label = { Text(version.name) },
                )
            }
        }
    }
}

@Composable
private fun VersionDetail(version: ModelVersion) {
    Column(modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm)) {
        if (version.baseModel != null) {
            DetailRow(label = "Base Model", value = version.baseModel!!)
        }

        if (version.trainedWords.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = "Trained Words",
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = version.trainedWords.joinToString(", "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (version.files.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                text = "Files",
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            version.files.forEach { file ->
                FileInfoRow(file = file)
                Spacer(modifier = Modifier.height(Spacing.xs))
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun FileInfoRow(file: ModelFile) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Text(
                    text = FormatUtils.formatFileSize(file.sizeKB),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                file.format?.let { format ->
                    Text(
                        text = format,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                file.fp?.let { fp ->
                    Text(
                        text = fp,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
