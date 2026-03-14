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
import com.riox432.civitdeck.feature.settings.presentation.AppBehaviorSettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettingsScreen(
    viewModel: AppBehaviorSettingsViewModel,
    onBack: () -> Unit,
    onNavigateToIntegrations: () -> Unit = {},
    onNavigateToModelFiles: () -> Unit = {},
    onNavigateToPlugins: () -> Unit = {},
    onNavigateToNavShortcuts: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Advanced & Integrations") },
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
            item { SectionHeader("Power User") }
            item { PowerUserModeRow(state.powerUserMode, viewModel::onPowerUserModeChanged) }
            if (state.powerUserMode) {
                item { SectionHeader("Integrations") }
                item { SubScreenRow("Server Integrations", onNavigateToIntegrations) }
                item { SectionHeader("Model Files") }
                item { SubScreenRow("Model File Browser", onNavigateToModelFiles) }
                item { SectionHeader("Navigation") }
                item { SubScreenRow("Navigation Shortcuts", onNavigateToNavShortcuts) }
            }
            item { SectionHeader("Plugins") }
            item { SubScreenRow("Plugins", onNavigateToPlugins) }
        }
    }
}
