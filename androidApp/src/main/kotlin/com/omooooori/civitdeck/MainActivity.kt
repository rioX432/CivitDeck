package com.omooooori.civitdeck

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import com.omooooori.civitdeck.ui.search.ModelSearchScreen
import com.omooooori.civitdeck.ui.search.ModelSearchViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val modelSearchViewModel: ModelSearchViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ModelSearchScreen(
                    viewModel = modelSearchViewModel,
                )
            }
        }
    }
}
