package com.riox432.civitdeck.data.local.repository

import com.riox432.civitdeck.data.local.dao.ShareHashtagDao
import com.riox432.civitdeck.data.local.entity.ShareHashtagEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [ShareHashtagRepositoryImpl] covering tag normalization (# prefix),
 * blank rejection, enable toggling, removal, and entity-to-domain mapping.
 */
class ShareHashtagRepositoryImplTest {

    private class FakeDao : ShareHashtagDao {
        val entities = mutableListOf<ShareHashtagEntity>()
        private val flow = MutableStateFlow<List<ShareHashtagEntity>>(emptyList())

        private fun emit() {
            flow.value = entities.sortedBy { it.addedAt }.toList()
        }

        override fun observeAll(): Flow<List<ShareHashtagEntity>> = flow

        override suspend fun insert(entity: ShareHashtagEntity) {
            if (entities.none { it.tag == entity.tag }) entities.add(entity)
            emit()
        }

        override suspend fun setEnabled(tag: String, isEnabled: Boolean) {
            val idx = entities.indexOfFirst { it.tag == tag }
            if (idx >= 0) entities[idx] = entities[idx].copy(isEnabled = isEnabled)
            emit()
        }

        override suspend fun delete(tag: String): Int {
            val before = entities.size
            entities.removeAll { it.tag == tag }
            emit()
            return before - entities.size
        }
    }

    @Test
    fun addCustom_prepends_hash_when_missing() = runTest {
        val dao = FakeDao()
        val repo = ShareHashtagRepositoryImpl(dao)
        repo.addCustom("civitai")
        assertEquals("#civitai", dao.entities[0].tag)
        assertTrue(dao.entities[0].isCustom)
        assertTrue(dao.entities[0].isEnabled)
    }

    @Test
    fun addCustom_keeps_existing_hash() = runTest {
        val dao = FakeDao()
        val repo = ShareHashtagRepositoryImpl(dao)
        repo.addCustom("  #lora  ")
        assertEquals("#lora", dao.entities[0].tag)
    }

    @Test
    fun addCustom_normalizes_whitespace_only_tag_to_hash() = runTest {
        // normalizeTag trims then prefixes "#", so a blank input yields "#" (never ignored).
        val dao = FakeDao()
        val repo = ShareHashtagRepositoryImpl(dao)
        repo.addCustom("   ")
        assertEquals("#", dao.entities.single().tag)
    }

    @Test
    fun observeAll_maps_to_domain() = runTest {
        val dao = FakeDao()
        val repo = ShareHashtagRepositoryImpl(dao)
        repo.addCustom("anime")
        val result = repo.observeAll().first()
        assertEquals(1, result.size)
        assertEquals("#anime", result[0].tag)
        assertTrue(result[0].isCustom)
    }

    @Test
    fun setEnabled_toggles_flag() = runTest {
        val dao = FakeDao()
        val repo = ShareHashtagRepositoryImpl(dao)
        repo.addCustom("anime")
        repo.setEnabled("#anime", false)
        assertEquals(false, repo.observeAll().first()[0].isEnabled)
    }

    @Test
    fun remove_deletes_tag() = runTest {
        val dao = FakeDao()
        val repo = ShareHashtagRepositoryImpl(dao)
        repo.addCustom("anime")
        repo.remove("#anime")
        assertTrue(dao.entities.isEmpty())
    }
}
