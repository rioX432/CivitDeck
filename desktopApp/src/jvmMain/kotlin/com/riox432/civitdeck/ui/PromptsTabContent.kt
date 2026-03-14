package com.riox432.civitdeck

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.riox432.civitdeck.feature.prompts.presentation.SavedPromptsViewModel
import com.riox432.civitdeck.ui.prompts.DesktopPromptsScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PromptsTabContent(
    modifier: Modifier = Modifier,
) {
    val promptsVm: SavedPromptsViewModel = koinViewModel()
    DesktopPromptsScreen(
        viewModel = promptsVm,
        modifier = modifier,
    )
}
