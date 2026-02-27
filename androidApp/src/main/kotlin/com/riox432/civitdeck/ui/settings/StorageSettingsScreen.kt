package com.riox432.civitdeck.ui.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storage") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            item { SectionHeader("Offline Cache") }
            item { OfflineCacheToggleRow(state.offlineCacheEnabled, viewModel::onOfflineCacheEnabledChanged) }
            if (state.offlineCacheEnabled) {
                item {
                    CacheSizeLimitRow(
                        currentLimitMb = state.cacheSizeLimitMb,
                        currentUsage = state.cacheInfo.formattedSize,
                        onChanged = viewModel::onCacheSizeLimitChanged,
                    )
                }
            }
            item { CacheInfoRow(state.cacheInfo.entryCount, state.cacheInfo.formattedSize) }
            item { SectionHeader("Data Management") }
            item { HiddenModelsRow(state.hiddenModels.size, state.hiddenModels, viewModel::onUnhideModel) }
            item { ClearActionRow("Clear Search History", viewModel::onClearSearchHistory) }
            item { ClearActionRow("Clear Browsing History", viewModel::onClearBrowsingHistory) }
            item { ClearActionRow("Clear Cache", viewModel::onClearCache) }
        }
    }
}
