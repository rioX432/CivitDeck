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
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.feature.settings.presentation.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentFilterSettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Content & Filters") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            item { SectionHeader("NSFW") }
            item { NsfwToggleRow(state.nsfwFilterLevel, viewModel::onNsfwFilterChanged) }
            if (state.nsfwFilterLevel != NsfwFilterLevel.Off) {
                item {
                    NsfwBlurSection(
                        settings = state.nsfwBlurSettings,
                        onSettingsChanged = viewModel::onNsfwBlurSettingsChanged,
                    )
                }
            }
            item { SectionHeader("Defaults") }
            item { SortOrderRow(state.defaultSortOrder, viewModel::onSortOrderChanged) }
            item { TimePeriodRow(state.defaultTimePeriod, viewModel::onTimePeriodChanged) }
            item { SectionHeader("Tags") }
            item {
                ExcludedTagsRow(
                    tags = state.excludedTags,
                    onAdd = viewModel::onAddExcludedTag,
                    onRemove = viewModel::onRemoveExcludedTag,
                )
            }
        }
    }
}
