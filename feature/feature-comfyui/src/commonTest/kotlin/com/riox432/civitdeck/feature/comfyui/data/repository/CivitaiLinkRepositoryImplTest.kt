package com.riox432.civitdeck.feature.comfyui.data.repository

import com.riox432.civitdeck.data.api.civitailink.CivitaiLinkApi
import com.riox432.civitdeck.domain.model.CivitaiLinkResource
import com.riox432.civitdeck.domain.model.CivitaiLinkStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Covers [CivitaiLinkRepositoryImpl]'s non-WebSocket logic: initial state,
 * [disconnect] resetting status/activities, and the `activeKey == null` guards
 * in [sendResourceToPC]/[cancelActivity] which must short-circuit before the API.
 *
 * Live WebSocket flows (connect/sendCommand) are not exercised — they require a
 * real wss server and are out of scope for unit tests.
 */
class CivitaiLinkRepositoryImplTest {

    // The API would throw if any wss call were attempted; the guarded paths must not reach it.
    private fun throwingApi() = CivitaiLinkApi(
        mockClient { error("network must not be called") },
        testJson,
    )

    private val sampleResource = CivitaiLinkResource(
        versionId = 1L,
        modelId = 2L,
        versionName = "v1",
        downloadUrl = "https://example.com/m.safetensors",
    )

    @Test
    fun initial_status_is_disconnected() = runTest {
        val repo = CivitaiLinkRepositoryImpl(throwingApi())

        assertEquals(CivitaiLinkStatus.Disconnected, repo.observeStatus().first())
        assertFalse(repo.isConnected())
    }

    @Test
    fun observeActivities_starts_empty() = runTest {
        val repo = CivitaiLinkRepositoryImpl(throwingApi())

        assertEquals(emptyList(), repo.observeActivities().first())
    }

    @Test
    fun disconnect_resets_status_and_activities() = runTest {
        val repo = CivitaiLinkRepositoryImpl(throwingApi())

        repo.disconnect()

        assertEquals(CivitaiLinkStatus.Disconnected, repo.observeStatus().first())
        assertEquals(emptyList(), repo.observeActivities().first())
        assertFalse(repo.isConnected())
    }

    @Test
    fun sendResourceToPC_is_noop_when_no_active_key() = runTest {
        // No connect() called -> activeKey is null -> must return before touching the throwing API.
        val repo = CivitaiLinkRepositoryImpl(throwingApi())

        repo.sendResourceToPC(sampleResource) // should not throw
    }

    @Test
    fun cancelActivity_is_noop_when_no_active_key() = runTest {
        val repo = CivitaiLinkRepositoryImpl(throwingApi())

        repo.cancelActivity("activity-1") // should not throw
    }
}
