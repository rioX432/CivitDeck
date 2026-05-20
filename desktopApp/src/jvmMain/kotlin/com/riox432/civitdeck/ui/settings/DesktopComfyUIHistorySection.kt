package com.riox432.civitdeck.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.riox432.civitdeck.feature.comfyui.presentation.ComfyUIHistoryViewModel
import com.riox432.civitdeck.feature.comfyui.presentation.HistorySortOrder
import com.riox432.civitdeck.ui.theme.Spacing

@Composable
fun ComfyUIHistorySection(viewModel: ComfyUIHistoryViewModel) {
    val state by viewModel.uiState.collectAsState()
    val images = viewModel.filteredImages()

    SettingsCard(title = "ComfyUI History") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsDropdown(
                label = "Sort",
                selected = state.selectedSort.name,
                options = HistorySortOrder.entries.map { it.name },
                onSelected = { viewModel.onSelectSort(HistorySortOrder.valueOf(it)) },
            )
            TextButton(onClick = viewModel::refresh, enabled = !state.isLoading) {
                Text(if (state.isLoading) "Loading..." else "Refresh")
            }
        }
        Spacer(modifier = Modifier.height(Spacing.sm))
        if (images.isEmpty() && !state.isLoading) {
            Text("No history yet", style = MaterialTheme.typography.bodySmall)
        } else {
            Text(
                "${images.size} image(s) in history",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        state.error?.let { error ->
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}
