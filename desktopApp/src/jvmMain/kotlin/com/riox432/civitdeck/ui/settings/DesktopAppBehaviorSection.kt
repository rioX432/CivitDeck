package com.riox432.civitdeck.ui.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.riox432.civitdeck.domain.model.PollingInterval
import com.riox432.civitdeck.feature.settings.presentation.AppBehaviorSettingsViewModel
import com.riox432.civitdeck.ui.theme.Spacing

@Composable
internal fun AppBehaviorSection(viewModel: AppBehaviorSettingsViewModel) {
    val state by viewModel.uiState.collectAsState()

    SettingsCard(title = "App Behavior") {
        SwitchSetting(
            label = "Power User Mode",
            checked = state.powerUserMode,
            onCheckedChange = viewModel::onPowerUserModeChanged,
        )
        SwitchSetting(
            label = "Notifications",
            checked = state.notificationsEnabled,
            onCheckedChange = viewModel::onNotificationsEnabledChanged,
        )
        if (state.notificationsEnabled) {
            Spacer(modifier = Modifier.height(Spacing.sm))
            SettingsDropdown(
                label = "Polling Interval",
                selected = state.pollingInterval.displayName,
                options = PollingInterval.entries.map { it.displayName },
                onSelected = { name ->
                    PollingInterval.entries.find { it.displayName == name }
                        ?.let { viewModel.onPollingIntervalChanged(it) }
                },
            )
        }
        Spacer(modifier = Modifier.height(Spacing.sm))
        SliderSetting(
            label = "Feed Quality Threshold",
            value = state.feedQualityThreshold.toFloat(),
            valueRange = 0f..100f,
            steps = 9,
            valueLabel = state.feedQualityThreshold.toString(),
            onValueChange = { viewModel.onFeedQualityThresholdChanged(it.toInt()) },
        )
    }
}
