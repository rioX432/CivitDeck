package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.ImageTagRepository

class BatchEditTagsUseCase(private val repository: ImageTagRepository) {
    suspend operator fun invoke(imageIds: List<Long>, addTags: List<String>, removeTags: List<String>) {
        if (addTags.isNotEmpty()) repository.addTagsToImages(imageIds, addTags)
        if (removeTags.isNotEmpty()) repository.removeTagsFromImages(imageIds, removeTags)
    }
}
