package com.riox432.civitdeck.data.repository

import com.riox432.civitdeck.data.local.dao.FavoriteModelDao
import com.riox432.civitdeck.data.local.dao.TypeCount
import com.riox432.civitdeck.data.local.entity.FavoriteModelEntity
import com.riox432.civitdeck.testModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FavoriteRepositoryImplTest {

    private class FakeDao : FavoriteModelDao {
        val entities = mutableListOf<FavoriteModelEntity>()
        private val flow = MutableStateFlow(0) // change counter

        override fun getAllAsFlow(): Flow<List<FavoriteModelEntity>> =
            flow.map { entities.sortedByDescending { it.favoritedAt } }

        override suspend fun getById(id: Long): FavoriteModelEntity? =
            entities.find { it.id == id }

        override fun isFavorite(id: Long): Flow<Boolean> =
            flow.map { entities.any { it.id == id } }

        override suspend fun insert(entity: FavoriteModelEntity) {
            entities.removeAll { it.id == entity.id }
            entities.add(entity)
            flow.value++
        }

        override suspend fun deleteById(id: Long) {
            entities.removeAll { it.id == id }
            flow.value++
        }

        override suspend fun count(): Int = entities.size

        override suspend fun getAllIds(): List<Long> = entities.map { it.id }

        override suspend fun getTypeCounts(): List<TypeCount> =
            entities.groupBy { it.type }.map { (type, list) -> TypeCount(type, list.size) }
    }

    @Test
    fun toggleFavorite_adds_when_not_exists() = runTest {
        val dao = FakeDao()
        val repo = FavoriteRepositoryImpl(dao)
        val model = testModel(id = 1L)
        repo.toggleFavorite(model)
        assertEquals(1, dao.entities.size)
        assertEquals(1L, dao.entities[0].id)
    }

    @Test
    fun toggleFavorite_removes_when_exists() = runTest {
        val dao = FakeDao()
        dao.entities.add(
            FavoriteModelEntity(
                id = 1L, name = "Test", type = "Checkpoint", nsfw = false,
                thumbnailUrl = null, creatorName = null, downloadCount = 0,
                favoriteCount = 0, rating = 0.0, favoritedAt = 1000L,
            ),
        )
        val repo = FavoriteRepositoryImpl(dao)
        repo.toggleFavorite(testModel(id = 1L))
        assertTrue(dao.entities.isEmpty())
    }

    @Test
    fun observeFavorites_maps_entities_to_domain() = runTest {
        val dao = FakeDao()
        dao.entities.add(
            FavoriteModelEntity(
                id = 1L, name = "Model A", type = "LORA", nsfw = false,
                thumbnailUrl = null, creatorName = "artist", downloadCount = 50,
                favoriteCount = 10, rating = 4.0, favoritedAt = 1000L,
            ),
        )
        val repo = FavoriteRepositoryImpl(dao)
        val result = repo.observeFavorites().first()
        assertEquals(1, result.size)
        assertEquals("Model A", result[0].name)
        assertEquals("artist", result[0].creatorName)
    }

    @Test
    fun observeIsFavorite_returns_true_when_exists() = runTest {
        val dao = FakeDao()
        dao.entities.add(
            FavoriteModelEntity(
                id = 5L, name = "Test", type = "Checkpoint", nsfw = false,
                thumbnailUrl = null, creatorName = null, downloadCount = 0,
                favoriteCount = 0, rating = 0.0, favoritedAt = 1000L,
            ),
        )
        val repo = FavoriteRepositoryImpl(dao)
        assertTrue(repo.observeIsFavorite(5L).first())
        assertFalse(repo.observeIsFavorite(99L).first())
    }

    @Test
    fun getAllFavoriteIds_returns_ids() = runTest {
        val dao = FakeDao()
        dao.entities.addAll(
            listOf(
                FavoriteModelEntity(
                    id = 1L, name = "A", type = "Checkpoint", nsfw = false,
                    thumbnailUrl = null, creatorName = null, downloadCount = 0,
                    favoriteCount = 0, rating = 0.0, favoritedAt = 1000L,
                ),
                FavoriteModelEntity(
                    id = 2L, name = "B", type = "LORA", nsfw = false,
                    thumbnailUrl = null, creatorName = null, downloadCount = 0,
                    favoriteCount = 0, rating = 0.0, favoritedAt = 2000L,
                ),
            ),
        )
        val repo = FavoriteRepositoryImpl(dao)
        assertEquals(setOf(1L, 2L), repo.getAllFavoriteIds())
    }

    @Test
    fun getFavoriteTypeCounts_aggregates() = runTest {
        val dao = FakeDao()
        dao.entities.addAll(
            listOf(
                FavoriteModelEntity(
                    id = 1L, name = "A", type = "Checkpoint", nsfw = false,
                    thumbnailUrl = null, creatorName = null, downloadCount = 0,
                    favoriteCount = 0, rating = 0.0, favoritedAt = 1000L,
                ),
                FavoriteModelEntity(
                    id = 2L, name = "B", type = "Checkpoint", nsfw = false,
                    thumbnailUrl = null, creatorName = null, downloadCount = 0,
                    favoriteCount = 0, rating = 0.0, favoritedAt = 2000L,
                ),
                FavoriteModelEntity(
                    id = 3L, name = "C", type = "LORA", nsfw = false,
                    thumbnailUrl = null, creatorName = null, downloadCount = 0,
                    favoriteCount = 0, rating = 0.0, favoritedAt = 3000L,
                ),
            ),
        )
        val repo = FavoriteRepositoryImpl(dao)
        val counts = repo.getFavoriteTypeCounts()
        assertEquals(2, counts["Checkpoint"])
        assertEquals(1, counts["LORA"])
    }
}
