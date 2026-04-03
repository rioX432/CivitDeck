package com.riox432.civitdeck.ui.comfyui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.GenerationStatus
import com.riox432.civitdeck.feature.comfyui.presentation.GenerationUiState
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
    val canGenerate = state.customWorkflowJson != null ||
        (state.selectedCheckpoint.isNotBlank() && state.prompt.isNotBlank())
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Button(
            onClick = onGenerate,
            enabled = !isGenerating && canGenerate,
            modifier = Modifier.weight(1f),
        ) {
            if (isGenerating) {
                CircularProgressIndicator(modifier = Modifier.padding(end = Spacing.sm))
            }
            Text(if (isGenerating) "Generating..." else "Generate")
        }
        if (isGenerating) {
            Button(
                onClick = onInterrupt,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Icon(Icons.Default.Stop, contentDescription = "Stop generation")
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
            Text("Generating...", style = MaterialTheme.typography.bodySmall)
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

@Composable
internal fun ResultGrid(imageUrls: List<String>, onSaveImage: (String) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        userScrollEnabled = false,
    ) {
        items(imageUrls, key = { it }) { url ->
            Card {
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
    }
}
