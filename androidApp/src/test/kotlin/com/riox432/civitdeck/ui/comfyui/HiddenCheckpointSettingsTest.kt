package com.riox432.civitdeck.ui.comfyui

import com.riox432.civitdeck.feature.comfyui.presentation.GenerationModelSource
import com.riox432.civitdeck.feature.comfyui.presentation.GenerationUiState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HiddenCheckpointSettingsTest {

    private var controlNetTurnedOff = false
    private var maskCleared = false

    private fun drop(state: GenerationUiState) = dropHiddenCheckpointSettings(
        state = state,
        turnOffControlNet = { controlNetTurnedOff = true },
        clearMask = { maskCleared = true },
    )

    @Test
    fun dropsControlNetAndMaskLeftOnADiffusionModel() {
        drop(
            GenerationUiState(
                modelSource = GenerationModelSource.DIFFUSION_MODEL,
                controlNetEnabled = true,
                maskImageFilename = "mask.png",
            ),
        )

        assertTrue(controlNetTurnedOff)
        assertTrue(maskCleared)
    }

    @Test
    fun keepsControlNetAndMaskOnACheckpoint() {
        drop(GenerationUiState(controlNetEnabled = true, maskImageFilename = "mask.png"))

        assertFalse(controlNetTurnedOff)
        assertFalse(maskCleared)
    }

    @Test
    fun leavesADiffusionModelWithoutCheckpointSettingsAlone() {
        drop(GenerationUiState(modelSource = GenerationModelSource.DIFFUSION_MODEL, initImageFilename = "init.png"))

        assertFalse(controlNetTurnedOff)
        assertFalse(maskCleared)
    }
}
