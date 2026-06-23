package com.riox432.civitdeck.data.local.repository

import com.riox432.civitdeck.data.local.dao.DatasetImageMetaDao
import com.riox432.civitdeck.data.local.entity.CaptionEntity
import com.riox432.civitdeck.data.local.entity.ImageTagEntity
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit tests for [CaptionRepositoryImpl] covering caption upsert (insert + overwrite),
 * retrieval with entity-to-domain mapping, and the absent case.
 */
class CaptionRepositoryImplTest {

    private class FakeMetaDao : DatasetImageMetaDao {
        val captions = mutableListOf<CaptionEntity>()

        override suspend fun getTagsForImage(datasetImageId: Long): List<ImageTagEntity> = emptyList()
        override suspend fun insertTags(entities: List<ImageTagEntity>) = Unit
        override suspend fun deleteTagsForImage(datasetImageId: Long): Int = 0
        override suspend fun deleteTagByName(imageId: Long, tag: String): Int = 0
        override suspend fun getTagSuggestions(datasetId: Long, prefix: String): List<String> = emptyList()

        override suspend fun getCaption(datasetImageId: Long): CaptionEntity? =
            captions.firstOrNull { it.datasetImageId == datasetImageId }

        override suspend fun upsertCaption(entity: CaptionEntity) {
            captions.removeAll { it.datasetImageId == entity.datasetImageId }
            captions.add(entity)
        }
    }

    @Test
    fun setCaption_inserts_caption() = runTest {
        val repo = CaptionRepositoryImpl(FakeMetaDao())
        repo.setCaption(1L, "a cat")
        assertEquals("a cat", repo.getCaption(1L)?.text)
    }

    @Test
    fun setCaption_overwrites_existing() = runTest {
        val repo = CaptionRepositoryImpl(FakeMetaDao())
        repo.setCaption(1L, "first")
        repo.setCaption(1L, "second")
        assertEquals("second", repo.getCaption(1L)?.text)
    }

    @Test
    fun getCaption_maps_entity_to_domain() = runTest {
        val repo = CaptionRepositoryImpl(FakeMetaDao())
        repo.setCaption(5L, "text")
        val caption = repo.getCaption(5L)
        assertEquals(5L, caption?.datasetImageId)
        assertEquals("text", caption?.text)
    }

    @Test
    fun getCaption_returns_null_when_absent() = runTest {
        val repo = CaptionRepositoryImpl(FakeMetaDao())
        assertNull(repo.getCaption(99L))
    }
}
