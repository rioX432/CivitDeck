package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.FavoriteModelSummary
import com.riox432.civitdeck.domain.model.Model
import com.riox432.civitdeck.domain.repository.FavoriteRepository
import com.riox432.civitdeck.testFavoriteModelSummary
import com.riox432.civitdeck.testModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FavoriteUseCasesTest {

    private val favorites = listOf(
        testFavoriteModelSummary(id = 1L, name = "Fav1"),
        testFavoriteModelSummary(id = 2L, name = "Fav2"),
    )

    private fun fakeRepository(
        isFavorite: Boolean = true,
        toggledModel: Model? = null,
    ) = object : FavoriteRepository {
        var toggleCalled = false
        var toggledWith: Model? = null

        override fun observeFavorites(): Flow<List<FavoriteModelSummary>> =
            flowOf(favorites)

        override fun observeIsFavorite(modelId: Long): Flow<Boolean> =
            flowOf(isFavorite)

        override suspend fun toggleFavorite(model: Model) {
            toggleCalled = true
            toggledWith = model
        }

        override suspend fun addFavorite(model: Model) = error("not used")
        override suspend fun removeFavorite(modelId: Long) = error("not used")
        override suspend fun getAllFavoriteIds(): Set<Long> = error("not used")
        override suspend fun getFavoriteTypeCounts(): Map<String, Int> = error("not used")
    }

    @Test
    fun observeFavorites_emits_list() = runTest {
        val repo = fakeRepository()
        val useCase = ObserveFavoritesUseCase(repo)
        val result = useCase().first()
        assertEquals(2, result.size)
        assertEquals("Fav1", result[0].name)
    }

    @Test
    fun observeIsFavorite_emits_true() = runTest {
        val repo = fakeRepository(isFavorite = true)
        val useCase = ObserveIsFavoriteUseCase(repo)
        val result = useCase(1L).first()
        assertTrue(result)
    }

    @Test
    fun observeIsFavorite_emits_false() = runTest {
        val repo = fakeRepository(isFavorite = false)
        val useCase = ObserveIsFavoriteUseCase(repo)
        val result = useCase(1L).first()
        assertEquals(false, result)
    }

    @Test
    fun toggleFavorite_delegates_to_repository() = runTest {
        val repo = fakeRepository()
        val useCase = ToggleFavoriteUseCase(repo)
        val model = testModel(id = 99L)
        useCase(model)
        assertTrue(repo.toggleCalled)
        assertEquals(99L, repo.toggledWith?.id)
    }
}
