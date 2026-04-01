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
import com.riox432.civitdeck.domain.model.ShareHashtag
import com.riox432.civitdeck.feature.settings.presentation.AppBehaviorSettingsViewModel
import com.riox432.civitdeck.ui.share.ShareSettingsSection

@Suppress("LongParameterList")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettingsScreen(
    viewModel: AppBehaviorSettingsViewModel,
    onBack: () -> Unit,
    onNavigateToIntegrations: () -> Unit = {},
    onNavigateToModelFiles: () -> Unit = {},
    onNavigateToPlugins: () -> Unit = {},
    onNavigateToNavShortcuts: () -> Unit = {},
    shareHashtags: List<ShareHashtag> = emptyList(),
    onToggleShareHashtag: (String, Boolean) -> Unit = { _, _ -> },
    onAddShareHashtag: (String) -> Unit = {},
    onRemoveShareHashtag: (String) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_section_advanced_integrations)) },
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
            item { SectionHeader(stringResource(R.string.settings_section_power_user)) }
            item { PowerUserModeRow(state.powerUserMode, viewModel::onPowerUserModeChanged) }
            if (state.powerUserMode) {
                item { SectionHeader(stringResource(R.string.settings_section_integrations)) }
                item { SubScreenRow(stringResource(R.string.settings_server_integrations), onNavigateToIntegrations) }
                item { SectionHeader(stringResource(R.string.settings_section_model_files)) }
                item { SubScreenRow(stringResource(R.string.settings_model_file_browser), onNavigateToModelFiles) }
                item { SectionHeader(stringResource(R.string.settings_section_navigation)) }
                item { SubScreenRow(stringResource(R.string.settings_navigation_shortcuts), onNavigateToNavShortcuts) }
            }
            item { SectionHeader(stringResource(R.string.settings_section_sharing)) }
            item {
                ShareSettingsSection(
                    hashtags = shareHashtags,
                    onToggle = onToggleShareHashtag,
                    onAdd = onAddShareHashtag,
                    onRemove = onRemoveShareHashtag,
                )
            }
            item { SectionHeader(stringResource(R.string.settings_section_plugins)) }
            item { SubScreenRow(stringResource(R.string.settings_section_plugins), onNavigateToPlugins) }
        }
    }
}
