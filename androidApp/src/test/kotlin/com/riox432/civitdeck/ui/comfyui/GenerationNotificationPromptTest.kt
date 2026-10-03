package com.riox432.civitdeck.ui.comfyui

import android.os.Build
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GenerationNotificationPromptTest {

    private val android13 = Build.VERSION_CODES.TIRAMISU
    private val android12L = Build.VERSION_CODES.S_V2

    @Test
    fun promptsOnFirstGenerateWhenAlertsOnAndPermissionMissing() {
        assertTrue(shouldPrompt())
    }

    @Test
    fun doesNotPromptWhenGenerationAlertsAreOff() {
        assertFalse(shouldPrompt(enabled = false))
    }

    @Test
    fun doesNotPromptTwiceOnTheSameScreen() {
        assertFalse(shouldPrompt(alreadyPrompted = true))
    }

    @Test
    fun doesNotPromptBeforeAndroid13() {
        assertFalse(shouldPrompt(sdkInt = android12L))
    }

    @Test
    fun doesNotPromptWhenPermissionAlreadyGranted() {
        assertFalse(shouldPrompt(granted = true))
    }

    private fun shouldPrompt(
        enabled: Boolean = true,
        alreadyPrompted: Boolean = false,
        sdkInt: Int = android13,
        granted: Boolean = false,
    ) = shouldPromptForGenerationNotifications(
        generationNotificationsEnabled = enabled,
        alreadyPrompted = alreadyPrompted,
        sdkInt = sdkInt,
        permissionGranted = granted,
    )
}
