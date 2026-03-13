package com.riox432.civitdeck

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.domain.model.AccentColor
import com.riox432.civitdeck.domain.model.ThemeMode
import com.riox432.civitdeck.domain.usecase.ObserveAccentColorUseCase
import com.riox432.civitdeck.domain.usecase.ObserveAmoledDarkModeUseCase
import com.riox432.civitdeck.domain.usecase.ObserveThemeModeUseCase
import com.riox432.civitdeck.ui.navigation.CivitDeckNavGraph
import com.riox432.civitdeck.ui.navigation.Tab
import com.riox432.civitdeck.ui.theme.CivitDeckTheme
import com.riox432.civitdeck.ui.tutorial.GestureTutorialScreen
import com.riox432.civitdeck.ui.tutorial.GestureTutorialViewModel
import com.riox432.civitdeck.usecase.GetActiveThemeUseCase
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MainActivity : ComponentActivity(), KoinComponent {

    private val observeAccentColor: ObserveAccentColorUseCase by inject()
    private val observeAmoledDarkMode: ObserveAmoledDarkModeUseCase by inject()
    private val observeThemeMode: ObserveThemeModeUseCase by inject()
    private val getActiveTheme: GetActiveThemeUseCase by inject()

    companion object {
        const val EXTRA_INITIAL_ROUTE = "extra_initial_route"
        const val ROUTE_SEARCH = "search"
        const val ROUTE_SETTINGS = "settings"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        super.onCreate(savedInstanceState)
        setContent {
            val accentColor by observeAccentColor()
                .collectAsStateWithLifecycle(AccentColor.Blue)
            val amoledDarkMode by observeAmoledDarkMode()
                .collectAsStateWithLifecycle(false)
            val themeMode by observeThemeMode()
                .collectAsStateWithLifecycle(ThemeMode.SYSTEM)
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> systemDark
            }
            val activeTheme by getActiveTheme.observeColorScheme(darkTheme)
                .collectAsStateWithLifecycle(null)

            CivitDeckTheme(
                darkTheme = darkTheme,
                accentColor = accentColor,
                amoledDarkMode = amoledDarkMode,
                customTheme = activeTheme,
            ) {
                val tutorialVm: GestureTutorialViewModel = koinViewModel()
                val showTutorial by tutorialVm.shouldShowTutorial.collectAsStateWithLifecycle()

                if (showTutorial) {
                    GestureTutorialScreen(onDismiss = tutorialVm::dismissTutorial)
                } else {
                    val initialTab = when (
                        intent.getStringExtra(EXTRA_INITIAL_ROUTE)
                    ) {
                        ROUTE_SETTINGS -> Tab.Settings
                        else -> Tab.Search
                    }
                    CivitDeckNavGraph(initialTab = initialTab)
                }
            }
        }
    }
}
