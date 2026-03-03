package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.ImageSource
import com.riox432.civitdeck.domain.repository.DatasetCollectionRepository

class AddImageToDatasetUseCase(
    private val repository: DatasetCollectionRepository,
) {
    suspend operator fun invoke(
        datasetId: Long,
        imageUrl: String,
        sourceType: ImageSource,
        trainable: Boolean = true,
        tags: List<String> = emptyList(),
    ): Long = repository.addImage(datasetId, imageUrl, sourceType, trainable, tags)
}
