package com.riox432.civitdeck.ui.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
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
import com.riox432.civitdeck.feature.settings.presentation.AppBehaviorSettingsUiState
import com.riox432.civitdeck.feature.settings.presentation.AppBehaviorSettingsViewModel
import com.riox432.civitdeck.feature.settings.presentation.ContentFilterSettingsUiState
import com.riox432.civitdeck.feature.settings.presentation.ContentFilterSettingsViewModel
import com.riox432.civitdeck.feature.settings.presentation.DisplaySettingsUiState
import com.riox432.civitdeck.feature.settings.presentation.DisplaySettingsViewModel

@Suppress("LongParameterList")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentFilterSettingsScreen(
    viewModel: ContentFilterSettingsViewModel,
    displayViewModel: DisplaySettingsViewModel,
    appBehaviorViewModel: AppBehaviorSettingsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val displayState by displayViewModel.uiState.collectAsStateWithLifecycle()
    val behaviorState by appBehaviorViewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Content & Behavior") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding)) {
            contentFilterItems(state, viewModel, displayState, displayViewModel)
            feedQualityItems(behaviorState, appBehaviorViewModel)
            notificationItems(behaviorState, appBehaviorViewModel)
        }
    }
}

internal fun LazyListScope.contentFilterItems(
    state: ContentFilterSettingsUiState,
    viewModel: ContentFilterSettingsViewModel,
    displayState: DisplaySettingsUiState,
    displayViewModel: DisplaySettingsViewModel,
) {
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
    item { SortOrderRow(displayState.defaultSortOrder, displayViewModel::onSortOrderChanged) }
    item { TimePeriodRow(displayState.defaultTimePeriod, displayViewModel::onTimePeriodChanged) }
    item { SectionHeader("Tags") }
    item {
        ExcludedTagsRow(
            tags = state.excludedTags,
            onAdd = viewModel::onAddExcludedTag,
            onRemove = viewModel::onRemoveExcludedTag,
        )
    }
}

internal fun LazyListScope.notificationItems(
    state: AppBehaviorSettingsUiState,
    viewModel: AppBehaviorSettingsViewModel,
) {
    item { SectionHeader("Notifications") }
    item { NotificationsToggleRow(state.notificationsEnabled, viewModel::onNotificationsEnabledChanged) }
    if (state.notificationsEnabled) {
        item { PollingIntervalRow(state.pollingInterval, viewModel::onPollingIntervalChanged) }
    }
}

internal fun LazyListScope.feedQualityItems(
    state: AppBehaviorSettingsUiState,
    viewModel: AppBehaviorSettingsViewModel,
) {
    item { SectionHeader("Search Quality Filter") }
    item {
        FeedQualityThresholdRow(
            threshold = state.feedQualityThreshold,
            onChanged = viewModel::onFeedQualityThresholdChanged,
        )
    }
}
