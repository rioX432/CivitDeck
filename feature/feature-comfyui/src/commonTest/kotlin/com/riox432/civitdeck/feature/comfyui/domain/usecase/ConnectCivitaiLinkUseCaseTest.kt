package com.riox432.civitdeck.feature.comfyui.domain.usecase

import com.riox432.civitdeck.domain.model.CivitaiLinkActivity
import com.riox432.civitdeck.domain.model.CivitaiLinkResource
import com.riox432.civitdeck.domain.model.CivitaiLinkStatus
import com.riox432.civitdeck.domain.repository.AuthPreferencesRepository
import com.riox432.civitdeck.domain.repository.CivitaiLinkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Verifies [ConnectCivitaiLinkUseCase] connects only when a stored link key exists, and
 * reports failure (without calling the repository) when no key is configured.
 */
class ConnectCivitaiLinkUseCaseTest {

    @Test
    fun connectsWithStoredKey() = runTest {
        val repo = RecordingLinkRepo()
        val useCase = ConnectCivitaiLinkUseCase(repo, FakePrefs(linkKey = "secret-key"))

        val result = useCase()

        assertTrue(result)
        assertEquals("secret-key", repo.connectedKey)
    }

    @Test
    fun returnsFalseAndDoesNotConnectWhenKeyMissing() = runTest {
        val repo = RecordingLinkRepo()
        val useCase = ConnectCivitaiLinkUseCase(repo, FakePrefs(linkKey = null))

        val result = useCase()

        assertFalse(result)
        assertNull(repo.connectedKey)
    }

    private class RecordingLinkRepo : CivitaiLinkRepository {
        var connectedKey: String? = null
        override suspend fun connect(key: String) {
            connectedKey = key
        }
        override fun observeStatus(): Flow<CivitaiLinkStatus> = flowOf(CivitaiLinkStatus.Disconnected)
        override fun observeActivities(): Flow<List<CivitaiLinkActivity>> = flowOf(emptyList())
        override fun disconnect() {}
        override suspend fun sendResourceToPC(resource: CivitaiLinkResource) = Unit
        override suspend fun cancelActivity(activityId: String) = Unit
        override fun isConnected(): Boolean = false
    }

    private class FakePrefs(private val linkKey: String?) : AuthPreferencesRepository {
        override suspend fun getCivitaiLinkKey(): String? = linkKey
        override fun observeApiKey(): Flow<String?> = flowOf(null)
        override suspend fun setApiKey(apiKey: String?) = Unit
        override suspend fun getApiKey(): String? = null
        override fun observeCivitaiLinkKey(): Flow<String?> = flowOf(linkKey)
        override suspend fun setCivitaiLinkKey(key: String?) = Unit
    }
}
