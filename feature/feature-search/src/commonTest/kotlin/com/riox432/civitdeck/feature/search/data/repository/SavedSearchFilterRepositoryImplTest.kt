package com.riox432.civitdeck.feature.search.data.repository

import com.riox432.civitdeck.data.local.dao.SavedSearchFilterDao
import com.riox432.civitdeck.data.local.entity.SavedSearchFilterEntity
import com.riox432.civitdeck.domain.model.BaseModel
import com.riox432.civitdeck.domain.model.ModelSource
import com.riox432.civitdeck.domain.model.ModelType
import com.riox432.civitdeck.domain.model.NsfwFilterLevel
import com.riox432.civitdeck.domain.model.SavedSearchFilter
import com.riox432.civitdeck.domain.model.SortOrder
import com.riox432.civitdeck.domain.model.TimePeriod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers [SavedSearchFilterRepositoryImpl]'s entity <-> domain serialization:
 * enum round-trips, CSV/newline (de)serialization, empty-string guards, and the
 * lenient fallbacks for unknown enum names.
 */
class SavedSearchFilterRepositoryImplTest {

    private class FakeDao : SavedSearchFilterDao {
        val entities = mutableListOf<SavedSearchFilterEntity>()
        private var idCounter = 1L
        private val updates = MutableStateFlow(0)

        override fun observeAll(): Flow<List<SavedSearchFilterEntity>> =
            updates.map { entities.sortedByDescending { it.savedAt } }

        override suspend fun getAll(): List<SavedSearchFilterEntity> =
            entities.sortedByDescending { it.savedAt }

        override suspend fun insert(entity: SavedSearchFilterEntity): Long {
            // Mirror OnConflictStrategy.REPLACE: explicit non-zero id replaces in place.
            val id = if (entity.id != 0L) entity.id else idCounter++
            entities.removeAll { it.id == id }
            entities.add(entity.copy(id = id))
            updates.value++
            return id
        }

        override suspend fun insertAll(entities: List<SavedSearchFilterEntity>) {
            entities.forEach { insert(it) }
        }

        override suspend fun deleteAll(): Int {
            val removed = entities.size
            entities.clear()
            updates.value++
            return removed
        }

        override suspend fun deleteById(id: Long): Int {
            val removed = entities.count { it.id == id }
            entities.removeAll { it.id == id }
            updates.value++
            return removed
        }
    }

    @Test
    fun observeAll_maps_all_fields_from_entity_to_domain() = runTest {
        val dao = FakeDao()
        dao.entities.add(
            SavedSearchFilterEntity(
                id = 1,
                name = "Anime LoRAs",
                query = "anime",
                selectedType = ModelType.LORA.name,
                selectedSort = SortOrder.Newest.name,
                selectedPeriod = TimePeriod.Week.name,
                selectedBaseModels = "Pony,SDXL 1.0",
                nsfwFilterLevel = NsfwFilterLevel.Soft.name,
                isFreshFindEnabled = 1,
                excludedTags = "gore\nviolence",
                includedTags = "cute",
                selectedSources = "CIVITAI,HUGGING_FACE",
                savedAt = 500L,
            ),
        )
        val repo = SavedSearchFilterRepositoryImpl(dao)

        val result = repo.observeAll().first().first()

        assertEquals("Anime LoRAs", result.name)
        assertEquals("anime", result.query)
        assertEquals(ModelType.LORA, result.selectedType)
        assertEquals(SortOrder.Newest, result.selectedSort)
        assertEquals(TimePeriod.Week, result.selectedPeriod)
        assertEquals(setOf(BaseModel.Pony, BaseModel.SDXL10), result.selectedBaseModels)
        assertEquals(NsfwFilterLevel.Soft, result.nsfwFilterLevel)
        assertTrue(result.isFreshFindEnabled)
        assertEquals(listOf("gore", "violence"), result.excludedTags)
        assertEquals(listOf("cute"), result.includedTags)
        assertEquals(setOf(ModelSource.CIVITAI, ModelSource.HUGGING_FACE), result.selectedSources)
    }

    @Test
    fun observeAll_applies_defaults_for_blank_and_unknown_values() = runTest {
        val dao = FakeDao()
        dao.entities.add(
            SavedSearchFilterEntity(
                id = 1,
                name = "Defaults",
                selectedType = null,
                selectedSort = "BogusSort",
                selectedPeriod = "BogusPeriod",
                selectedBaseModels = "",
                nsfwFilterLevel = "BogusLevel",
                isFreshFindEnabled = 0,
                excludedTags = "",
                includedTags = "",
                selectedSources = "",
                savedAt = 1L,
            ),
        )
        val repo = SavedSearchFilterRepositoryImpl(dao)

        val result = repo.observeAll().first().first()

        assertNull(result.selectedType)
        assertEquals(SortOrder.MostDownloaded, result.selectedSort) // fallback
        assertEquals(TimePeriod.AllTime, result.selectedPeriod) // fallback
        assertTrue(result.selectedBaseModels.isEmpty())
        assertEquals(NsfwFilterLevel.Off, result.nsfwFilterLevel) // fallback
        assertTrue(!result.isFreshFindEnabled)
        assertTrue(result.excludedTags.isEmpty())
        assertTrue(result.includedTags.isEmpty())
        assertEquals(setOf(ModelSource.CIVITAI), result.selectedSources) // default source
    }

    @Test
    fun save_persists_domain_filter_round_trip() = runTest {
        val dao = FakeDao()
        val repo = SavedSearchFilterRepositoryImpl(dao)
        val filter = SavedSearchFilter(
            id = 0,
            name = "My Filter",
            query = "checkpoint",
            selectedType = ModelType.Checkpoint,
            selectedSort = SortOrder.MostDownloaded,
            selectedPeriod = TimePeriod.AllTime,
            selectedBaseModels = setOf(BaseModel.SD15),
            nsfwFilterLevel = NsfwFilterLevel.Off,
            isFreshFindEnabled = false,
            excludedTags = listOf("gore"),
            includedTags = emptyList(),
            selectedSources = setOf(ModelSource.CIVITAI),
            savedAt = 1000L,
        )

        val newId = repo.save(filter)
        val stored = dao.entities.single()

        assertEquals(newId, stored.id)
        assertEquals("My Filter", stored.name)
        assertEquals(ModelType.Checkpoint.name, stored.selectedType)
        assertEquals("SD 1.5", stored.selectedBaseModels) // apiValue, not enum name
        assertEquals("gore", stored.excludedTags)
        assertEquals("CIVITAI", stored.selectedSources)
    }

    @Test
    fun delete_removes_filter_by_id() = runTest {
        val dao = FakeDao()
        dao.entities.add(SavedSearchFilterEntity(id = 1, name = "a", savedAt = 1L))
        dao.entities.add(SavedSearchFilterEntity(id = 2, name = "b", savedAt = 2L))
        val repo = SavedSearchFilterRepositoryImpl(dao)

        repo.delete(1L)

        assertEquals(listOf(2L), dao.entities.map { it.id })
    }
}
