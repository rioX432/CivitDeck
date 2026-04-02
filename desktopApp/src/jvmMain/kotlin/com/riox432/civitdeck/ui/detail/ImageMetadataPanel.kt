package com.riox432.civitdeck.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.riox432.civitdeck.domain.model.ImageGenerationMeta
import com.riox432.civitdeck.domain.model.ModelImage
import com.riox432.civitdeck.ui.theme.CornerRadius
import com.riox432.civitdeck.ui.theme.Spacing

@Composable
internal fun ImageMetadataPanel(
    image: ModelImage,
    onClose: () -> Unit,
    onOpenFullscreen: () -> Unit,
) {
    Surface(
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(topStart = CornerRadius.card, topEnd = CornerRadius.card),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
        ) {
            MetadataPanelHeader(onClose = onClose, onOpenFullscreen = onOpenFullscreen)
            Spacer(modifier = Modifier.height(Spacing.sm))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(METADATA_PANEL_HEIGHT)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                MetadataRow("Dimensions", "${image.width} x ${image.height}")
                image.hash?.let { MetadataRow("Hash", it) }

                image.meta?.let { meta ->
                    HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.xs))
                    Text(
                        "Generation Parameters",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    GenerationMetadataContent(meta)
                }
            }
        }
    }
}

@Composable
private fun MetadataPanelHeader(
    onClose: () -> Unit,
    onOpenFullscreen: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Image Metadata",
            style = MaterialTheme.typography.titleSmall,
        )
        Row {
            IconButton(onClick = onOpenFullscreen) {
                Icon(
                    Icons.Default.OpenInFull,
                    contentDescription = "View full size",
                    modifier = Modifier.size(ICON_SIZE),
                )
            }
            IconButton(onClick = onClose) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    modifier = Modifier.size(ICON_SIZE),
                )
            }
        }
    }
}

@Composable
private fun GenerationMetadataContent(meta: ImageGenerationMeta) {
    meta.model?.let { MetadataRow("Model", it) }
    meta.sampler?.let { MetadataRow("Sampler", it) }
    meta.steps?.let { MetadataRow("Steps", it.toString()) }
    meta.cfgScale?.let { MetadataRow("CFG Scale", it.toString()) }
    meta.seed?.let { MetadataRow("Seed", it.toString()) }
    meta.size?.let { MetadataRow("Size", it) }
    meta.prompt?.let { prompt ->
        MetadataRow("Prompt", prompt, monospace = true)
    }
    meta.negativePrompt?.let { neg ->
        MetadataRow("Negative Prompt", neg, monospace = true)
    }
    if (meta.additionalParams.isNotEmpty()) {
        meta.additionalParams.forEach { (key, value) ->
            MetadataRow(key, value)
        }
    }
}

@Composable
private fun MetadataRow(
    label: String,
    value: String,
    monospace: Boolean = false,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = if (monospace) {
                MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
            } else {
                MaterialTheme.typography.bodySmall
            },
            maxLines = if (monospace) MAX_MONOSPACE_LINES else MAX_NORMAL_LINES,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
