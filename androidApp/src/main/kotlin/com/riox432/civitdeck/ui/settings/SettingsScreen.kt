package com.riox432.civitdeck.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.BuildConfig
import com.riox432.civitdeck.R
import com.riox432.civitdeck.feature.settings.presentation.AppBehaviorSettingsViewModel
import com.riox432.civitdeck.feature.settings.presentation.AuthSettingsUiState
import com.riox432.civitdeck.feature.settings.presentation.AuthSettingsViewModel
import com.riox432.civitdeck.feature.settings.presentation.StorageSettingsViewModel
import com.riox432.civitdeck.ui.components.EmptyStateMessage
import com.riox432.civitdeck.ui.theme.Spacing
import com.riox432.civitdeck.ui.update.UpdateBanner
import com.riox432.civitdeck.ui.update.UpdateUiState
import com.riox432.civitdeck.ui.update.UpdateViewModel

@Suppress("LongParameterList")
@Composable
fun SettingsScreen(
    authViewModel: AuthSettingsViewModel,
    storageViewModel: StorageSettingsViewModel,
    appBehaviorViewModel: AppBehaviorSettingsViewModel,
    updateViewModel: UpdateViewModel,
    onNavigateToAppearance: () -> Unit = {},
    onNavigateToContentFilter: () -> Unit = {},
    onNavigateToStorage: () -> Unit = {},
    onNavigateToAdvanced: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToNotificationCenter: () -> Unit = {},
    onNavigateToBrowsingHistory: () -> Unit = {},
    onNavigateToLicenses: () -> Unit = {},
    onOpenUrl: (String) -> Unit = {},
    scrollToTopTrigger: Int = 0,
) {
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()
    val storageState by storageViewModel.uiState.collectAsStateWithLifecycle()
    val appBehaviorState by appBehaviorViewModel.uiState.collectAsStateWithLifecycle()
    val updateState by updateViewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var lastHandledTrigger by rememberSaveable { mutableIntStateOf(scrollToTopTrigger) }
    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger != lastHandledTrigger) {
            lastHandledTrigger = scrollToTopTrigger
            listState.animateScrollToItem(0)
        }
    }
    val isEmpty by remember { derivedStateOf { listState.layoutInfo.totalItemsCount == 0 } }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            if (!storageState.isOnline) {
                item { OfflineBanner() }
            }
            settingsAccountItems(authState, authViewModel)
            item { SectionHeader(stringResource(R.string.settings_section_appearance)) }
            item { SubScreenRow(stringResource(R.string.settings_section_appearance), onNavigateToAppearance) }
            item { SectionHeader(stringResource(R.string.settings_section_content_behavior)) }
            item { SubScreenRow(stringResource(R.string.settings_section_content_behavior), onNavigateToContentFilter) }
            item { SectionHeader(stringResource(R.string.settings_section_notifications)) }
            item { SubScreenRow(stringResource(R.string.settings_model_updates), onNavigateToNotificationCenter) }
            item { SectionHeader(stringResource(R.string.settings_section_history)) }
            item { SubScreenRow(stringResource(R.string.settings_browsing_history), onNavigateToBrowsingHistory) }
            item { SectionHeader(stringResource(R.string.settings_section_data_storage)) }
            item { SubScreenRow(stringResource(R.string.settings_section_data_storage), onNavigateToStorage) }
            item { SectionHeader(stringResource(R.string.settings_section_advanced_integrations)) }
            item { SubScreenRow(stringResource(R.string.settings_section_advanced_integrations), onNavigateToAdvanced) }
            if (appBehaviorState.powerUserMode) {
                item { SectionHeader(stringResource(R.string.settings_section_analytics)) }
                item { SubScreenRow(stringResource(R.string.settings_usage_stats), onNavigateToAnalytics) }
            }
            settingsUpdateItems(updateState, updateViewModel, onOpenUrl)
            settingsAboutItems(
                appBehaviorState.powerUserMode,
                updateState,
                updateViewModel,
                onNavigateToLicenses,
            )
        }
        if (isEmpty) {
            EmptyStateMessage(
                icon = Icons.Default.Settings,
                title = stringResource(R.string.settings_no_settings_available),
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

internal fun LazyListScope.settingsAccountItems(
    state: AuthSettingsUiState,
    viewModel: AuthSettingsViewModel,
) {
    item { SectionHeader(stringResource(R.string.settings_section_account)) }
    item {
        AccountSection(
            apiKey = state.apiKey,
            connectedUsername = state.connectedUsername,
            isValidating = state.isValidatingApiKey,
            error = state.apiKeyError,
            onValidateAndSave = viewModel::onValidateAndSaveApiKey,
            onClear = viewModel::onClearApiKey,
        )
    }
}

private fun LazyListScope.settingsUpdateItems(
    updateState: UpdateUiState,
    updateViewModel: UpdateViewModel,
    onOpenUrl: (String) -> Unit,
) {
    val updateResult = updateState.updateResult
    if (updateState.showBanner && updateResult != null) {
        item {
            UpdateBanner(
                updateResult = updateResult,
                onDownload = { onOpenUrl(updateResult.htmlUrl) },
                onDismiss = updateViewModel::dismissBanner,
            )
        }
    }
}

@Suppress("LongParameterList")
private fun LazyListScope.settingsAboutItems(
    powerUserMode: Boolean,
    updateState: UpdateUiState,
    updateViewModel: UpdateViewModel,
    onNavigateToLicenses: () -> Unit,
) {
    item { SectionHeader(stringResource(R.string.settings_section_about)) }
    item { InfoRow(stringResource(R.string.settings_app_version), BuildConfig.VERSION_NAME) }
    item {
        UpdateCheckRow(
            autoCheckEnabled = updateState.autoCheckEnabled,
            isChecking = updateState.isChecking,
            onAutoCheckChanged = updateViewModel::setAutoCheckEnabled,
            onCheckNow = updateViewModel::checkForUpdate,
        )
    }
    item { NavigationRow(stringResource(R.string.settings_open_source_licenses), onNavigateToLicenses) }
    if (!powerUserMode) {
        item {
            Text(
                stringResource(R.string.settings_power_user_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
            )
        }
    }
}
