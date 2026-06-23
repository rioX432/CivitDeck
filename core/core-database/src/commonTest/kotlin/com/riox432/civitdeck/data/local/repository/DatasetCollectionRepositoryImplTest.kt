package com.riox432.civitdeck.data.local.repository

import com.riox432.civitdeck.data.local.dao.DatasetCollectionDao
import com.riox432.civitdeck.data.local.dao.DatasetCollectionWithCount
import com.riox432.civitdeck.data.local.dao.DatasetImageMetaDao
import com.riox432.civitdeck.data.local.entity.CaptionEntity
import com.riox432.civitdeck.data.local.entity.DatasetCollectionEntity
import com.riox432.civitdeck.data.local.entity.DatasetImageEntity
import com.riox432.civitdeck.data.local.entity.ImageTagEntity
import com.riox432.civitdeck.domain.model.ImageSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [DatasetCollectionRepositoryImpl] covering collection CRUD, image add (with
 * tag insertion), trainable filtering, and image-to-domain mapping that joins tags + caption.
 */
class DatasetCollectionRepositoryImplTest {

    private class FakeCollectionDao : DatasetCollectionDao {
        val collections = mutableListOf<DatasetCollectionEntity>()
        val images = mutableListOf<DatasetImageEntity>()
        private var collectionId = 1L
        private var imageId = 1L
        private val collectionsFlow = MutableStateFlow<List<DatasetCollectionWithCount>>(emptyList())

        private fun emitCollections() {
            collectionsFlow.value = collections.map { c ->
                DatasetCollectionWithCount(
                    id = c.id,
                    name = c.name,
                    description = c.description,
                    createdAt = c.createdAt,
                    updatedAt = c.updatedAt,
                    imageCount = images.count { it.datasetId == c.id },
                )
            }
        }

        override fun observeAllWithCount(): Flow<List<DatasetCollectionWithCount>> = collectionsFlow

        override suspend fun insertCollection(entity: DatasetCollectionEntity): Long {
            val id = collectionId++
            collections.add(entity.copy(id = id))
            emitCollections()
            return id
        }

        override suspend fun renameCollection(id: Long, name: String, updatedAt: Long): Int {
            val idx = collections.indexOfFirst { it.id == id }
            if (idx < 0) return 0
            collections[idx] = collections[idx].copy(name = name, updatedAt = updatedAt)
            emitCollections()
            return 1
        }

        override suspend fun deleteCollection(id: Long): Int {
            val before = collections.size
            collections.removeAll { it.id == id }
            images.removeAll { it.datasetId == id }
            emitCollections()
            return before - collections.size
        }

        override fun observeImages(datasetId: Long): Flow<List<DatasetImageEntity>> =
            MutableStateFlow(images.filter { it.datasetId == datasetId }.sortedByDescending { it.addedAt })

        override suspend fun insertImage(entity: DatasetImageEntity): Long {
            val id = imageId++
            images.add(entity.copy(id = id))
            emitCollections()
            return id
        }

        override suspend fun deleteImage(imageId: Long): Int {
            val before = images.size
            images.removeAll { it.id == imageId }
            return before - images.size
        }

        override suspend fun deleteImages(imageIds: List<Long>): Int {
            val before = images.size
            images.removeAll { it.id in imageIds }
            return before - images.size
        }

        override suspend fun updateTrainable(imageId: Long, trainable: Boolean): Int =
            mutate(imageId) { it.copy(trainable = trainable) }

        override suspend fun updateLicenseNote(imageId: Long, licenseNote: String?): Int =
            mutate(imageId) { it.copy(licenseNote = licenseNote) }

        override suspend fun getNonTrainableImages(datasetId: Long): List<DatasetImageEntity> =
            images.filter { it.datasetId == datasetId && !it.trainable }

        override suspend fun updatePHash(imageId: Long, pHash: String?): Int =
            mutate(imageId) { it.copy(pHash = pHash) }

        override suspend fun updateExcluded(imageId: Long, excluded: Boolean): Int =
            mutate(imageId) { it.copy(excluded = excluded) }

        override suspend fun updateDimensions(imageId: Long, width: Int, height: Int): Int =
            mutate(imageId) { it.copy(width = width, height = height) }

        private fun mutate(id: Long, block: (DatasetImageEntity) -> DatasetImageEntity): Int {
            val idx = images.indexOfFirst { it.id == id }
            if (idx < 0) return 0
            images[idx] = block(images[idx])
            return 1
        }
    }

    private class FakeMetaDao : DatasetImageMetaDao {
        val tags = mutableListOf<ImageTagEntity>()
        val captions = mutableListOf<CaptionEntity>()
        private var tagId = 1L

        override suspend fun getTagsForImage(datasetImageId: Long): List<ImageTagEntity> =
            tags.filter { it.datasetImageId == datasetImageId }

        override suspend fun insertTags(entities: List<ImageTagEntity>) {
            entities.forEach { tags.add(it.copy(id = tagId++)) }
        }

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
    fun createCollection_persists_and_returns_id() = runTest {
        val dao = FakeCollectionDao()
        val repo = DatasetCollectionRepositoryImpl(dao, FakeMetaDao())
        val id = repo.createCollection("Faces", "desc")
        assertEquals(1L, id)
        assertEquals("Faces", dao.collections[0].name)
    }

    @Test
    fun observeCollections_maps_with_image_count() = runTest {
        val dao = FakeCollectionDao()
        val repo = DatasetCollectionRepositoryImpl(dao, FakeMetaDao())
        val id = repo.createCollection("Faces", "")
        repo.addImage(id, "u1", ImageSource.CIVITAI, true, emptyList())
        val result = repo.observeCollections().first()
        assertEquals(1, result.size)
        assertEquals(1, result[0].imageCount)
    }

    @Test
    fun renameCollection_updates_name() = runTest {
        val dao = FakeCollectionDao()
        val repo = DatasetCollectionRepositoryImpl(dao, FakeMetaDao())
        val id = repo.createCollection("Old", "")
        repo.renameCollection(id, "New")
        assertEquals("New", dao.collections[0].name)
    }

    @Test
    fun addImage_inserts_image_and_tags() = runTest {
        val dao = FakeCollectionDao()
        val meta = FakeMetaDao()
        val repo = DatasetCollectionRepositoryImpl(dao, meta)
        val cid = repo.createCollection("C", "")
        val imageId = repo.addImage(cid, "url", ImageSource.GENERATED, true, listOf("anime", "girl"))
        assertEquals(1, dao.images.size)
        assertEquals(2, meta.tags.count { it.datasetImageId == imageId })
    }

    @Test
    fun observeImages_maps_tags_and_caption_into_domain() = runTest {
        val dao = FakeCollectionDao()
        val meta = FakeMetaDao()
        val repo = DatasetCollectionRepositoryImpl(dao, meta)
        val cid = repo.createCollection("C", "")
        val imageId = repo.addImage(cid, "url", ImageSource.CIVITAI, true, listOf("anime"))
        meta.upsertCaption(CaptionEntity(datasetImageId = imageId, text = "a cat"))
        val image = repo.observeImages(cid).first().single()
        assertEquals(ImageSource.CIVITAI, image.sourceType)
        assertEquals(listOf("anime"), image.tags.map { it.tag })
        assertEquals("a cat", image.caption?.text)
    }

    @Test
    fun getNonTrainableImages_filters_by_trainable_flag() = runTest {
        val dao = FakeCollectionDao()
        val repo = DatasetCollectionRepositoryImpl(dao, FakeMetaDao())
        val cid = repo.createCollection("C", "")
        repo.addImage(cid, "trainable", ImageSource.LOCAL, true, emptyList())
        repo.addImage(cid, "blocked", ImageSource.LOCAL, false, emptyList())
        val result = repo.getNonTrainableImages(cid)
        assertEquals(listOf("blocked"), result.map { it.imageUrl })
    }

    @Test
    fun deleteCollection_removes_collection() = runTest {
        val dao = FakeCollectionDao()
        val repo = DatasetCollectionRepositoryImpl(dao, FakeMetaDao())
        val id = repo.createCollection("C", "")
        repo.deleteCollection(id)
        assertTrue(dao.collections.isEmpty())
    }

    @Test
    fun removeImages_deletes_multiple_by_id() = runTest {
        val dao = FakeCollectionDao()
        val repo = DatasetCollectionRepositoryImpl(dao, FakeMetaDao())
        val cid = repo.createCollection("C", "")
        val a = repo.addImage(cid, "a", ImageSource.LOCAL, true, emptyList())
        val b = repo.addImage(cid, "b", ImageSource.LOCAL, true, emptyList())
        repo.removeImages(listOf(a, b))
        assertTrue(dao.images.isEmpty())
    }
}
