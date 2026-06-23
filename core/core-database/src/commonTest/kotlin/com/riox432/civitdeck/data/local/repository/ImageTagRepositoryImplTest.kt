package com.riox432.civitdeck.data.local.repository

import com.riox432.civitdeck.data.local.dao.DatasetImageMetaDao
import com.riox432.civitdeck.data.local.entity.CaptionEntity
import com.riox432.civitdeck.data.local.entity.ImageTagEntity
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [ImageTagRepositoryImpl] covering adding tags across multiple images,
 * removal, per-image tag retrieval/mapping, and prefix-based suggestions.
 */
class ImageTagRepositoryImplTest {

    private class FakeMetaDao : DatasetImageMetaDao {
        val tags = mutableListOf<ImageTagEntity>()
        private var idCounter = 1L

        override suspend fun getTagsForImage(datasetImageId: Long): List<ImageTagEntity> =
            tags.filter { it.datasetImageId == datasetImageId }

        override suspend fun insertTags(entities: List<ImageTagEntity>) {
            for (e in entities) {
                val exists = tags.any { it.datasetImageId == e.datasetImageId && it.tag == e.tag }
                if (!exists) tags.add(e.copy(id = idCounter++))
            }
        }

        override suspend fun deleteTagsForImage(datasetImageId: Long): Int {
            val before = tags.size
            tags.removeAll { it.datasetImageId == datasetImageId }
            return before - tags.size
        }

        override suspend fun deleteTagByName(imageId: Long, tag: String): Int {
            val before = tags.size
            tags.removeAll { it.datasetImageId == imageId && it.tag == tag }
            return before - tags.size
        }

        override suspend fun getTagSuggestions(datasetId: Long, prefix: String): List<String> =
            tags.map { it.tag }
                .filter { prefix.isEmpty() || it.startsWith(prefix) }
                .distinct()
                .sorted()
                .take(20)

        override suspend fun getCaption(datasetImageId: Long): CaptionEntity? = null
        override suspend fun upsertCaption(entity: CaptionEntity) = Unit
    }

    @Test
    fun addTagsToImages_applies_tags_to_every_image() = runTest {
        val dao = FakeMetaDao()
        val repo = ImageTagRepositoryImpl(dao)
        repo.addTagsToImages(listOf(1L, 2L), listOf("anime", "girl"))
        assertEquals(2, repo.getTagsForImage(1L).size)
        assertEquals(2, repo.getTagsForImage(2L).size)
    }

    @Test
    fun getTagsForImage_maps_entity_to_domain() = runTest {
        val dao = FakeMetaDao()
        val repo = ImageTagRepositoryImpl(dao)
        repo.addTagsToImages(listOf(1L), listOf("anime"))
        val result = repo.getTagsForImage(1L)
        assertEquals("anime", result[0].tag)
        assertEquals(1L, result[0].datasetImageId)
    }

    @Test
    fun addTagsToImages_skips_duplicate_tags() = runTest {
        val dao = FakeMetaDao()
        val repo = ImageTagRepositoryImpl(dao)
        repo.addTagsToImages(listOf(1L), listOf("anime"))
        repo.addTagsToImages(listOf(1L), listOf("anime"))
        assertEquals(1, repo.getTagsForImage(1L).size)
    }

    @Test
    fun removeTagsFromImages_removes_only_named_tags() = runTest {
        val dao = FakeMetaDao()
        val repo = ImageTagRepositoryImpl(dao)
        repo.addTagsToImages(listOf(1L), listOf("anime", "girl"))
        repo.removeTagsFromImages(listOf(1L), listOf("anime"))
        assertEquals(listOf("girl"), repo.getTagsForImage(1L).map { it.tag })
    }

    @Test
    fun getTagSuggestions_filters_by_prefix() = runTest {
        val dao = FakeMetaDao()
        val repo = ImageTagRepositoryImpl(dao)
        repo.addTagsToImages(listOf(1L), listOf("anime", "android", "girl"))
        assertEquals(listOf("android", "anime"), repo.getTagSuggestions(1L, "an"))
    }

    @Test
    fun getTagSuggestions_returns_empty_when_no_match() = runTest {
        val dao = FakeMetaDao()
        val repo = ImageTagRepositoryImpl(dao)
        repo.addTagsToImages(listOf(1L), listOf("anime"))
        assertTrue(repo.getTagSuggestions(1L, "zzz").isEmpty())
    }
}
