package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.DatasetImage
import com.riox432.civitdeck.domain.repository.DatasetCollectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FilterByResolutionUseCase(private val repository: DatasetCollectionRepository) {
    operator fun invoke(datasetId: Long, minWidth: Int, minHeight: Int): Flow<List<DatasetImage>> =
        repository.observeImages(datasetId).map { images ->
            images.filter { img ->
                img.width != null && img.height != null &&
                    (img.width < minWidth || img.height < minHeight)
            }
        }
}
