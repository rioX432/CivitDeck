package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.DatasetCollection
import com.riox432.civitdeck.domain.model.DatasetImage
import com.riox432.civitdeck.domain.model.ImageSource
import com.riox432.civitdeck.domain.repository.DatasetCollectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers the two dataset use cases that transform the observed image stream:
 * [DetectDuplicatesUseCase] (pHash Hamming grouping) and
 * [FilterByResolutionUseCase] (resolution predicate).
 */
class DatasetImageUseCasesTest {

    @Test
    fun detect_duplicates_groups_images_within_hamming_threshold() = runTest {
        // 1 & 2 differ by 1 bit (distance 1), 3 is far. With threshold 1, only {1,2} group.
        val repo = FakeDatasetRepo(
            listOf(
                image(id = 1L, pHash = "0000"),
                image(id = 2L, pHash = "0001"),
                image(id = 3L, pHash = "1111"),
            ),
        )
        val useCase = DetectDuplicatesUseCase(repo)

        val groups = useCase(datasetId = 1L, threshold = 1).first()

        assertEquals(1, groups.size)
        assertEquals(listOf(1L, 2L), groups[0].images.map { it.id })
    }

    @Test
    fun detect_duplicates_ignores_images_without_phash() = runTest {
        val repo = FakeDatasetRepo(
            listOf(
                image(id = 1L, pHash = "0000"),
                image(id = 2L, pHash = null),
                image(id = 3L, pHash = "0000"),
            ),
        )
        val useCase = DetectDuplicatesUseCase(repo)

        val groups = useCase(datasetId = 1L, threshold = 0).first()

        // Only the two hashable identical images group; the null-pHash one is excluded.
        assertEquals(1, groups.size)
        assertEquals(listOf(1L, 3L), groups[0].images.map { it.id })
    }

    @Test
    fun detect_duplicates_returns_empty_when_no_pair_is_within_threshold() = runTest {
        val repo = FakeDatasetRepo(
            listOf(image(id = 1L, pHash = "0000"), image(id = 2L, pHash = "1111")),
        )
        val useCase = DetectDuplicatesUseCase(repo)

        val groups = useCase(datasetId = 1L, threshold = 1).first()

        assertTrue(groups.isEmpty())
    }

    @Test
    fun filter_by_resolution_returns_only_images_below_minimum() = runTest {
        val repo = FakeDatasetRepo(
            listOf(
                image(id = 1L, width = 512, height = 512), // below 1024x1024 -> kept
                image(id = 2L, width = 2048, height = 2048), // above -> dropped
                image(id = 3L, width = 2048, height = 256), // height below -> kept
                image(id = 4L, width = null, height = null), // unknown dims -> dropped
            ),
        )
        val useCase = FilterByResolutionUseCase(repo)

        val result = useCase(datasetId = 1L, minWidth = 1024, minHeight = 1024).first()

        assertEquals(listOf(1L, 3L), result.map { it.id })
    }

    private fun image(
        id: Long,
        pHash: String? = null,
        width: Int? = null,
        height: Int? = null,
    ) = DatasetImage(
        id = id,
        datasetId = 1L,
        imageUrl = "url$id",
        sourceType = ImageSource.CIVITAI,
        addedAt = 0L,
        pHash = pHash,
        width = width,
        height = height,
    )

    private class FakeDatasetRepo(private val images: List<DatasetImage>) :
        DatasetCollectionRepository {
        override fun observeImages(datasetId: Long): Flow<List<DatasetImage>> = flowOf(images)

        override fun observeCollections(): Flow<List<DatasetCollection>> = throw NotImplementedError()
        override suspend fun createCollection(name: String, description: String): Long =
            throw NotImplementedError()
        override suspend fun renameCollection(id: Long, name: String) = throw NotImplementedError()
        override suspend fun deleteCollection(id: Long) = throw NotImplementedError()
        override suspend fun addImage(
            datasetId: Long,
            imageUrl: String,
            sourceType: ImageSource,
            trainable: Boolean,
            tags: List<String>,
        ): Long = throw NotImplementedError()
        override suspend fun removeImage(imageId: Long) = throw NotImplementedError()
        override suspend fun removeImages(imageIds: List<Long>) = throw NotImplementedError()
        override suspend fun updateTrainable(imageId: Long, trainable: Boolean) =
            throw NotImplementedError()
        override suspend fun updateLicenseNote(imageId: Long, licenseNote: String?) =
            throw NotImplementedError()
        override suspend fun getNonTrainableImages(datasetId: Long): List<DatasetImage> =
            throw NotImplementedError()
        override suspend fun updatePHash(imageId: Long, pHash: String?) = throw NotImplementedError()
        override suspend fun markExcluded(imageId: Long, excluded: Boolean) =
            throw NotImplementedError()
        override suspend fun updateDimensions(imageId: Long, width: Int, height: Int) =
            throw NotImplementedError()
    }
}
