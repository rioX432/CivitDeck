package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.data.local.entity.HiddenModelEntity
import com.riox432.civitdeck.domain.repository.HiddenModelRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HiddenModelUseCasesTest {

    private class FakeHiddenModelRepository : HiddenModelRepository {
        val hiddenIds = mutableSetOf(1L, 2L)
        val hiddenEntities = mutableListOf(
            HiddenModelEntity(modelId = 1L, modelName = "Model A", hiddenAt = 1000L),
            HiddenModelEntity(modelId = 2L, modelName = "Model B", hiddenAt = 2000L),
        )
        var hideCalled = false
        var unhideCalled = false
        var lastHideId: Long? = null
        var lastUnhideId: Long? = null

        override suspend fun getHiddenModelIds(): Set<Long> = hiddenIds.toSet()
        override suspend fun getHiddenModels(): List<HiddenModelEntity> = hiddenEntities.toList()
        override suspend fun hideModel(modelId: Long, modelName: String) {
            hideCalled = true
            lastHideId = modelId
        }
        override suspend fun unhideModel(modelId: Long) {
            unhideCalled = true
            lastUnhideId = modelId
        }
    }

    private val repo = FakeHiddenModelRepository()

    @Test
    fun getHiddenModelIds_returns_set() = runTest {
        val useCase = GetHiddenModelIdsUseCase(repo)
        assertEquals(setOf(1L, 2L), useCase())
    }

    @Test
    fun getHiddenModels_returns_entities() = runTest {
        val useCase = GetHiddenModelsUseCase(repo)
        val result = useCase()
        assertEquals(2, result.size)
        assertEquals("Model A", result[0].modelName)
    }

    @Test
    fun hideModel_delegates() = runTest {
        val useCase = HideModelUseCase(repo)
        useCase(42L, "Hidden Model")
        assertTrue(repo.hideCalled)
        assertEquals(42L, repo.lastHideId)
    }

    @Test
    fun unhideModel_delegates() = runTest {
        val useCase = UnhideModelUseCase(repo)
        useCase(1L)
        assertTrue(repo.unhideCalled)
        assertEquals(1L, repo.lastUnhideId)
    }
}
