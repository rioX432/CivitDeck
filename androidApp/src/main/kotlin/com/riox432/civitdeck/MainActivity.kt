package com.riox432.civitdeck

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.riox432.civitdeck.ui.navigation.CivitDeckNavGraph
import com.riox432.civitdeck.ui.theme.CivitDeckTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false
        super.onCreate(savedInstanceState)
        setContent {
            CivitDeckTheme {
                CivitDeckNavGraph()
            }
        }
    }
}
