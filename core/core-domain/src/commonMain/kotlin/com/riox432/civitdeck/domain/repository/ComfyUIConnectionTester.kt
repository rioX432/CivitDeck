package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.ComfyUIConnection
import com.riox432.civitdeck.domain.model.ConnectionTestResult

/**
 * Tests a [ComfyUIConnection] against a live server, selecting the correct HTTP client
 * based on [ComfyUIConnection.acceptSelfSigned] and returning a typed result with an
 * actionable failure cause.
 *
 * Unlike [ComfyUIConnectionRepository.testConnection] (which returns a plain Boolean and
 * always uses the shared client), this performs the test on a transient client so the
 * self-signed setting is honored and concurrent tests/probes do not share mutable state.
 */
interface ComfyUIConnectionTester {
    suspend fun test(connection: ComfyUIConnection): ConnectionTestResult
}
