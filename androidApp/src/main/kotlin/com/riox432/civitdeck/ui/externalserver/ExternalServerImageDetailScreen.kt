package com.riox432.civitdeck.ui.externalserver

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.riox432.civitdeck.domain.model.ShareHashtag
import com.riox432.civitdeck.feature.externalserver.domain.model.ServerImage
import com.riox432.civitdeck.ui.gallery.ImageViewerOverlay
import com.riox432.civitdeck.ui.gallery.ViewerImage
import com.riox432.civitdeck.ui.share.SocialShareSheet
import com.riox432.civitdeck.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExternalServerImageDetailScreen(
    image: ServerImage,
    onBack: () -> Unit,
    shareHashtags: List<ShareHashtag> = emptyList(),
    onToggleShareHashtag: (String, Boolean) -> Unit = { _, _ -> },
    onAddShareHashtag: (String) -> Unit = {},
    onRemoveShareHashtag: (String) -> Unit = {},
) {
    var showShareSheet by remember { mutableStateOf(false) }
    var showImageViewer by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(image.character ?: "Image Detail") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showShareSheet = true }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                },
            )
        },
    ) { padding ->
        DetailBody(
            image = image,
            onImageClick = { showImageViewer = true },
            modifier = Modifier.padding(padding),
        )
    }
    if (showImageViewer) {
        ImageViewerOverlay(
            images = listOf(ViewerImage(url = image.file)),
            initialIndex = 0,
            onDismiss = { showImageViewer = false },
        )
    }
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

@Composable
private fun DetailBody(
    image: ServerImage,
    onImageClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(image.file)
                .crossfade(true)
                .build(),
            contentDescription = image.character,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clickable(onClick = onImageClick),
        )
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            image.prompt?.let { prompt -> PromptSection(prompt = prompt, context = context) }
            HorizontalDivider()
            MetadataGrid(image = image)
        }
    }
}

@Composable
private fun PromptSection(prompt: String, context: Context) {
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
