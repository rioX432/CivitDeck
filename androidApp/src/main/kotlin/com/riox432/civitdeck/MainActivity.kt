package com.riox432.civitdeck

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riox432.civitdeck.domain.model.AccentColor
import com.riox432.civitdeck.domain.usecase.ObserveAccentColorUseCase
import com.riox432.civitdeck.domain.usecase.ObserveAmoledDarkModeUseCase
import com.riox432.civitdeck.ui.navigation.CivitDeckNavGraph
import com.riox432.civitdeck.ui.theme.CivitDeckTheme
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MainActivity : ComponentActivity(), KoinComponent {

    private val observeAccentColor: ObserveAccentColorUseCase by inject()
    private val observeAmoledDarkMode: ObserveAmoledDarkModeUseCase by inject()

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

            CivitDeckTheme(
                accentColor = accentColor,
                amoledDarkMode = amoledDarkMode,
            ) {
                CivitDeckNavGraph()
            }
        }
    }
}
