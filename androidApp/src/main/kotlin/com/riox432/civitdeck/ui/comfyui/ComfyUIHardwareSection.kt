package com.riox432.civitdeck.ui.comfyui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.SystemStats
import com.riox432.civitdeck.domain.util.OptimizationSuggestion
import com.riox432.civitdeck.ui.theme.CornerRadius
import com.riox432.civitdeck.ui.theme.Spacing

@Composable
internal fun ServerHardwareSection(stats: SystemStats) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                stringResource(R.string.comfyui_server_hardware),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            HardwareGpuRow(stats)
            VramProgressRow(stats)
            HardwareInfoRows(stats)
        }
    }
}

@Composable
private fun HardwareGpuRow(stats: SystemStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(R.string.comfyui_gpu), style = MaterialTheme.typography.labelMedium)
        Text(stats.gpuName, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun VramProgressRow(stats: SystemStats) {
    val vramUsed = stats.vramTotalMB - stats.vramFreeMB
    val progress = if (stats.vramTotalMB > 0) vramUsed.toFloat() / stats.vramTotalMB else 0f
    Spacer(modifier = Modifier.height(Spacing.xs))
    Text(stringResource(R.string.comfyui_vram_usage), style = MaterialTheme.typography.labelMedium)
    Spacer(modifier = Modifier.height(Spacing.xs))
    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier.fillMaxWidth().height(Spacing.sm),
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
    )
    Text(
        stringResource(R.string.comfyui_vram_format, vramUsed, stats.vramTotalMB),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun HardwareInfoRows(stats: SystemStats) {
    Spacer(modifier = Modifier.height(Spacing.xs))
    HardwareInfoRow(stringResource(R.string.comfyui_ram), stringResource(R.string.comfyui_ram_format, stats.ramTotalMB))
    stats.comfyuiVersion?.let { HardwareInfoRow(stringResource(R.string.comfyui_comfyui_version), it) }
    stats.pytorchVersion?.let { HardwareInfoRow(stringResource(R.string.comfyui_pytorch_version), it) }
    HardwareInfoRow(stringResource(R.string.comfyui_os), stats.os)
}

@Composable
private fun HardwareInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
internal fun OptimizationSuggestionsSection(
    suggestions: List<OptimizationSuggestion>,
    onDismiss: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                stringResource(R.string.vram_optimization_suggestions),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            suggestions.forEach { suggestion ->
                SuggestionCard(suggestion = suggestion, onDismiss = onDismiss)
                Spacer(modifier = Modifier.height(Spacing.xs))
            }
        }
    }
}

@Composable
private fun SuggestionCard(
    suggestion: OptimizationSuggestion,
    onDismiss: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        shape = RoundedCornerShape(CornerRadius.card),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(Spacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    suggestion.title,
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    suggestion.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { onDismiss(suggestion.id) }) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.cd_dismiss),
                    modifier = Modifier.size(Spacing.lg),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
