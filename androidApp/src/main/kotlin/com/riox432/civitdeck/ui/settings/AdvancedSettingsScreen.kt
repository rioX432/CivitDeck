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
import com.riox432.civitdeck.feature.settings.presentation.SettingsViewModel

@Suppress("LongParameterList")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onNavigateToComfyUI: () -> Unit = {},
    onNavigateToModelFiles: () -> Unit = {},
    onNavigateToTemplates: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Advanced") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            item { SectionHeader("Power User") }
            item { PowerUserModeRow(state.powerUserMode, viewModel::onPowerUserModeChanged) }
            if (state.powerUserMode) {
                item { SectionHeader("ComfyUI") }
                item { SubScreenRow("Server Connections", onNavigateToComfyUI) }
                item { SubScreenRow("Workflow Templates", onNavigateToTemplates) }
                item { SectionHeader("Model Files") }
                item { SubScreenRow("Model File Browser", onNavigateToModelFiles) }
            }
        }
    }
}
