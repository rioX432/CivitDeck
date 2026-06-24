package com.riox432.civitdeck.feature.comfyui.domain.usecase

import com.riox432.civitdeck.domain.model.SDWebUIConnection
import com.riox432.civitdeck.domain.repository.SDWebUIConnectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifies [TestSDWebUIConnectionUseCase] returns the connectivity result and persists it only
 * for saved connections (id != 0), not for unsaved drafts (id == 0).
 */
class TestSDWebUIConnectionUseCaseTest {

    @Test
    fun persistsResultForSavedConnection() = runTest {
        val repo = RecordingSDWebUIRepo(testResult = true)
        val useCase = TestSDWebUIConnectionUseCase(repo)

        val success = useCase(SDWebUIConnection(id = 5L, name = "Home", hostname = "10.0.0.1"))

        assertTrue(success)
        assertEquals(5L to true, repo.lastUpdate)
    }

    @Test
    fun doesNotPersistResultForUnsavedDraft() = runTest {
        val repo = RecordingSDWebUIRepo(testResult = false)
        val useCase = TestSDWebUIConnectionUseCase(repo)

        val success = useCase(SDWebUIConnection(id = 0L, name = "Draft", hostname = "10.0.0.2"))

        assertFalse(success)
        // id == 0 => updateTestResult must not be called.
        assertEquals(null, repo.lastUpdate)
    }

    private class RecordingSDWebUIRepo(private val testResult: Boolean) : SDWebUIConnectionRepository {
        var lastUpdate: Pair<Long, Boolean>? = null
        override suspend fun testConnection(connection: SDWebUIConnection): Boolean = testResult
        override suspend fun updateTestResult(id: Long, success: Boolean) {
            lastUpdate = id to success
        }
        override fun observeConnections(): Flow<List<SDWebUIConnection>> = flowOf(emptyList())
        override fun observeActiveConnection(): Flow<SDWebUIConnection?> = flowOf(null)
        override suspend fun getActiveConnection(): SDWebUIConnection? = null
        override suspend fun saveConnection(connection: SDWebUIConnection): Long = 1L
        override suspend fun deleteConnection(id: Long) = Unit
        override suspend fun activateConnection(id: Long) = Unit
    }
}
