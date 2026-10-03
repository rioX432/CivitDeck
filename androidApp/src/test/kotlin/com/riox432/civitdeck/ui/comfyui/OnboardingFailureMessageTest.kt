package com.riox432.civitdeck.ui.comfyui

import com.riox432.civitdeck.R
import com.riox432.civitdeck.domain.model.ConnectionFailureCause
import kotlin.test.Test
import kotlin.test.assertEquals

class OnboardingFailureMessageTest {

    @Test
    fun eachDiagnosableCauseShowsItsOwnFixInstruction() {
        assertEquals(R.string.comfyui_onboarding_fail_refused, failureMessageRes(ConnectionFailureCause.Refused))
        assertEquals(R.string.comfyui_onboarding_fail_auth, failureMessageRes(ConnectionFailureCause.AuthRequired))
        assertEquals(
            R.string.comfyui_onboarding_fail_not_comfyui,
            failureMessageRes(ConnectionFailureCause.NotComfyUI),
        )
        assertEquals(
            R.string.comfyui_onboarding_fail_loopback,
            failureMessageRes(ConnectionFailureCause.LoopbackHost),
        )
        assertEquals(R.string.comfyui_onboarding_fail_timeout, failureMessageRes(ConnectionFailureCause.Timeout))
    }

    @Test
    fun localNetworkDeniedKeepsTheGenericUnreachableMessage() {
        assertEquals(
            R.string.comfyui_onboarding_fail_unreachable,
            failureMessageRes(ConnectionFailureCause.LocalNetworkDenied),
        )
    }

    @Test
    fun everyCauseMapsToADistinctMessageExceptTheIosOnlyCause() {
        val messages = ConnectionFailureCause.entries
            .filter { it != ConnectionFailureCause.LocalNetworkDenied }
            .map(::failureMessageRes)

        assertEquals(messages.size, messages.toSet().size)
    }
}
