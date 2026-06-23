package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.ImageTag
import com.riox432.civitdeck.domain.repository.ImageTagRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Covers [BatchEditTagsUseCase]: it forwards add/remove only when the respective
 * tag list is non-empty, so empty lists never trigger a repository write.
 */
class BatchEditTagsUseCaseTest {

    @Test
    fun applies_both_add_and_remove_when_both_lists_are_non_empty() = runTest {
        val repo = RecordingImageTagRepo()
        val useCase = BatchEditTagsUseCase(repo)

        useCase(imageIds = listOf(1L, 2L), addTags = listOf("a"), removeTags = listOf("b"))

        assertEquals(listOf(1L, 2L) to listOf("a"), repo.added)
        assertEquals(listOf(1L, 2L) to listOf("b"), repo.removed)
    }

    @Test
    fun skips_add_when_add_list_is_empty() = runTest {
        val repo = RecordingImageTagRepo()
        val useCase = BatchEditTagsUseCase(repo)

        useCase(imageIds = listOf(1L), addTags = emptyList(), removeTags = listOf("b"))

        assertNull(repo.added)
        assertEquals(listOf(1L) to listOf("b"), repo.removed)
    }

    @Test
    fun skips_remove_when_remove_list_is_empty() = runTest {
        val repo = RecordingImageTagRepo()
        val useCase = BatchEditTagsUseCase(repo)

        useCase(imageIds = listOf(1L), addTags = listOf("a"), removeTags = emptyList())

        assertEquals(listOf(1L) to listOf("a"), repo.added)
        assertNull(repo.removed)
    }

    @Test
    fun writes_nothing_when_both_lists_are_empty() = runTest {
        val repo = RecordingImageTagRepo()
        val useCase = BatchEditTagsUseCase(repo)

        useCase(imageIds = listOf(1L), addTags = emptyList(), removeTags = emptyList())

        assertNull(repo.added)
        assertNull(repo.removed)
    }

    private class RecordingImageTagRepo : ImageTagRepository {
        var added: Pair<List<Long>, List<String>>? = null
        var removed: Pair<List<Long>, List<String>>? = null
        override suspend fun addTagsToImages(imageIds: List<Long>, tags: List<String>) {
            added = imageIds to tags
        }
        override suspend fun removeTagsFromImages(imageIds: List<Long>, tags: List<String>) {
            removed = imageIds to tags
        }
        override suspend fun getTagsForImage(datasetImageId: Long): List<ImageTag> = emptyList()
        override suspend fun getTagSuggestions(datasetId: Long, prefix: String): List<String> =
            emptyList()
    }
}
