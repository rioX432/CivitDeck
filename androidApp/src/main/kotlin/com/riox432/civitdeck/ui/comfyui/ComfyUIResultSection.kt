package com.riox432.civitdeck.ui.comfyui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.GenerationStatus
import com.riox432.civitdeck.feature.comfyui.presentation.GenerationUiState
import com.riox432.civitdeck.ui.adaptive.adaptiveGridColumns
import com.riox432.civitdeck.ui.components.CivitAsyncImage
import com.riox432.civitdeck.ui.theme.Spacing

@Composable
internal fun GenerateButton(
    state: GenerationUiState,
    onGenerate: () -> Unit,
    onInterrupt: () -> Unit,
) {
    val isGenerating = state.generationStatus == GenerationStatus.Submitting ||
        state.generationStatus == GenerationStatus.Running
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Button(
            onClick = onGenerate,
            enabled = !isGenerating && state.canGenerate,
            modifier = Modifier.weight(1f),
        ) {
            if (isGenerating) {
                CircularProgressIndicator(modifier = Modifier.padding(end = Spacing.sm))
            }
            Text(
                stringResource(
                    if (isGenerating) R.string.label_generating else R.string.action_generate,
                ),
            )
        }
        if (isGenerating) {
            Button(
                onClick = onInterrupt,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Icon(Icons.Default.Stop, contentDescription = stringResource(R.string.cd_stop_generation))
            }
        }
    }
}

@Composable
internal fun GenerationStatusSection(state: GenerationUiState) {
    when (state.generationStatus) {
        GenerationStatus.Running -> GenerationProgressSection(state)
        GenerationStatus.Error -> {
            Text(
                state.error ?: "Generation failed",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        GenerationStatus.Completed -> {
            Text(
                "Generation complete!",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        else -> {}
    }
}

@Composable
private fun GenerationProgressSection(state: GenerationUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        if (state.totalSteps > 0) {
            LinearProgressIndicator(
                progress = { state.progressFraction },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Step ${state.currentStep} / ${state.totalSteps}",
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(stringResource(R.string.comfyui_result_generating), style = MaterialTheme.typography.bodySmall)
        }
        if (state.currentNodeName.isNotEmpty()) {
            Text(
                "Node: ${state.currentNodeName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        PreviewImageView(state.previewImageBytes)
    }
}

@Composable
private fun PreviewImageView(imageBytes: ByteArray?) {
    if (imageBytes == null) return
    val bitmap = remember(imageBytes) {
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?.asImageBitmap()
    } ?: return
    Card(modifier = Modifier.fillMaxWidth()) {
        Image(
            bitmap = bitmap,
            contentDescription = "Generation preview",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        )
    }
}

/**
 * Not a lazy grid: this sits in an item of the screen's LazyColumn, which measures it with an unbounded height,
 * and a nested lazy grid throws on that. A generation returns a handful of images, so composing them all is fine.
 */
@Composable
internal fun ResultGrid(imageUrls: List<String>, onSaveImage: (String) -> Unit) {
    val gridColumns = adaptiveGridColumns()
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        imageUrls.chunked(gridColumns).forEach { rowUrls ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                rowUrls.forEach { url ->
                    key(url) { ResultCell(url, onSaveImage, Modifier.weight(1f)) }
                }
                // Keeps the cells of a short last row as wide as the ones above.
                repeat(gridColumns - rowUrls.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ResultCell(url: String, onSaveImage: (String) -> Unit, modifier: Modifier) {
    Card(modifier = modifier) {
        Column {
            CivitAsyncImage(
                imageUrl = url,
                contentDescription = stringResource(R.string.cd_generated_image),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
            )
            IconButton(
                onClick = { onSaveImage(url) },
                modifier = Modifier.align(Alignment.End),
            ) {
                Icon(Icons.Default.Download, contentDescription = stringResource(R.string.cd_save_to_gallery))
            }
        }
    }
}
