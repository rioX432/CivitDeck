package com.riox432.civitdeck.ui.externalserver

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.riox432.civitdeck.R
import com.riox432.civitdeck.feature.externalserver.domain.model.ServerImage
import com.riox432.civitdeck.ui.components.CivitAsyncImage
import com.riox432.civitdeck.ui.gallery.ImageViewerOverlay
import com.riox432.civitdeck.ui.gallery.ViewerImage
import com.riox432.civitdeck.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExternalServerImageDetailScreen(
    images: List<ServerImage>,
    initialIndex: Int,
    onBack: () -> Unit,
) {
    if (images.isEmpty()) return
    val pagerState = rememberPagerState(initialPage = initialIndex) { images.size }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${pagerState.currentPage + 1} / ${images.size}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back)
                        )
                    }
                },
            )
        },
    ) { padding ->
        HorizontalPager(state = pagerState, modifier = Modifier.padding(padding)) { page ->
            DetailPage(image = images[page])
        }
    }
}

@Composable
private fun DetailPage(image: ServerImage) {
    var showImageViewer by rememberSaveable { mutableStateOf(false) }
    DetailBody(image = image, onImageClick = { showImageViewer = true })
    if (showImageViewer) {
        ImageViewerOverlay(
            images = listOf(ViewerImage(url = image.file)),
            initialIndex = 0,
            onDismiss = { showImageViewer = false },
        )
    }
}

@Composable
private fun DetailBody(
    image: ServerImage,
    onImageClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        CivitAsyncImage(
            imageUrl = image.file,
            contentDescription = image.character,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onImageClick),
        )
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            image.prompt?.let { prompt -> PromptSection(prompt = prompt) }
            HorizontalDivider()
            MetadataGrid(image = image)
        }
    }
}

@Composable
private fun PromptSection(prompt: String) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Prompt", style = MaterialTheme.typography.titleSmall)
            IconButton(onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("prompt", prompt))
                Toast.makeText(context, "Prompt copied", Toast.LENGTH_SHORT).show()
            }) {
                Icon(Icons.Default.ContentCopy, "Copy prompt")
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = prompt,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(Spacing.md),
            )
        }
    }
}

@Composable
private fun MetadataGrid(image: ServerImage) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text("Details", style = MaterialTheme.typography.titleSmall)

        image.character?.let { MetadataRow("Character", it) }
        image.costume?.let { MetadataRow("Costume", it) }
        image.scenario?.let { MetadataRow("Scenario", it) }
        image.aestheticScore?.let { MetadataRow("Aesthetic Score", "%.2f".format(it)) }
        image.seed?.let { MetadataRow("Seed", it.toString()) }
        image.postStatus?.let { MetadataRow("Post Status", it) }
        MetadataRow("NSFW", if (image.nsfw) "Yes" else "No")
        image.createdAt?.let { MetadataRow("Created", it) }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
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
