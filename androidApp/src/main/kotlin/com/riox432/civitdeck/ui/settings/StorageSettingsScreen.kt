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
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.R
import com.riox432.civitdeck.presentation.settings.StorageSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageSettingsScreen(
    viewModel: StorageSettingsViewModel,
    onBack: () -> Unit,
    onNavigateToBackup: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_section_data_storage)) },
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
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            item { SectionHeader(stringResource(R.string.settings_section_offline_cache)) }
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
            item { SectionHeader(stringResource(R.string.settings_section_data_management)) }
            item { HiddenModelsRow(state.hiddenModels.size, state.hiddenModels, viewModel::onUnhideModel) }
            item {
                ClearActionRow(
                    stringResource(R.string.settings_clear_search_history),
                    viewModel::onClearSearchHistory
                )
            }
            item {
                ClearActionRow(
                    stringResource(R.string.settings_clear_browsing_history),
                    viewModel::onClearBrowsingHistory
                )
            }
            item { ClearActionRow(stringResource(R.string.settings_clear_cache), viewModel::onClearCache) }
            item { SectionHeader(stringResource(R.string.settings_section_backup)) }
            item { SubScreenRow(stringResource(R.string.settings_backup_restore), onNavigateToBackup) }
        }
    }
}
