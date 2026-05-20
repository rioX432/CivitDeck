package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.DatasetCollection
import com.riox432.civitdeck.domain.model.DatasetImage
import com.riox432.civitdeck.domain.model.ImageSource
import kotlinx.coroutines.flow.Flow

interface DatasetCollectionRepository {
    fun observeCollections(): Flow<List<DatasetCollection>>
    suspend fun createCollection(name: String, description: String = ""): Long
    suspend fun renameCollection(id: Long, name: String)
    suspend fun deleteCollection(id: Long)
    fun observeImages(datasetId: Long): Flow<List<DatasetImage>>
    suspend fun addImage(
        datasetId: Long,
        imageUrl: String,
        sourceType: ImageSource,
        trainable: Boolean = true,
        tags: List<String> = emptyList(),
    ): Long
    suspend fun removeImage(imageId: Long)
    suspend fun removeImages(imageIds: List<Long>)
    suspend fun updateTrainable(imageId: Long, trainable: Boolean)
    suspend fun updateLicenseNote(imageId: Long, licenseNote: String?)
    suspend fun getNonTrainableImages(datasetId: Long): List<DatasetImage>
    suspend fun updatePHash(imageId: Long, pHash: String?)
    suspend fun markExcluded(imageId: Long, excluded: Boolean)
    suspend fun updateDimensions(imageId: Long, width: Int, height: Int)
}
