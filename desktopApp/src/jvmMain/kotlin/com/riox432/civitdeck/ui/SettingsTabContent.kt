package com.riox432.civitdeck

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.riox432.civitdeck.feature.settings.presentation.AuthSettingsViewModel
import com.riox432.civitdeck.ui.analytics.DesktopAnalyticsViewModel
import com.riox432.civitdeck.ui.settings.DesktopSettingsScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsTabContent(
    modifier: Modifier = Modifier,
) {
    val authVm: AuthSettingsViewModel = koinViewModel()
    val analyticsVm: DesktopAnalyticsViewModel = koinViewModel()
    DesktopSettingsScreen(
        authSettingsViewModel = authVm,
        analyticsViewModel = analyticsVm,
        modifier = modifier,
    )
}
