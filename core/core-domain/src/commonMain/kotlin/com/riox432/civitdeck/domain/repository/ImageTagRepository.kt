package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.ImageTag

interface ImageTagRepository {
    suspend fun getTagsForImage(datasetImageId: Long): List<ImageTag>
    suspend fun addTagsToImages(imageIds: List<Long>, tags: List<String>)
    suspend fun removeTagsFromImages(imageIds: List<Long>, tags: List<String>)
    suspend fun getTagSuggestions(datasetId: Long, prefix: String): List<String>
}
