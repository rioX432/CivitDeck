package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.model.DatasetImage
import com.riox432.civitdeck.domain.model.DuplicateGroup
import com.riox432.civitdeck.domain.repository.DatasetCollectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DetectDuplicatesUseCase(private val repository: DatasetCollectionRepository) {
    operator fun invoke(datasetId: Long, threshold: Int = 10): Flow<List<DuplicateGroup>> =
        repository.observeImages(datasetId).map { images -> detectDuplicates(images, threshold) }

    private fun detectDuplicates(images: List<DatasetImage>, threshold: Int): List<DuplicateGroup> {
        val hashableImages = images.filter { it.pHash != null }
        val groups = mutableListOf<DuplicateGroup>()
        val visited = mutableSetOf<Long>()
        for (i in hashableImages.indices) {
            if (hashableImages[i].id in visited) continue
            val group = mutableListOf(hashableImages[i])
            for (j in i + 1 until hashableImages.size) {
                if (hashableImages[j].id in visited) continue
                if (hammingDistance(hashableImages[i].pHash!!, hashableImages[j].pHash!!) <= threshold) {
                    group.add(hashableImages[j])
                    visited.add(hashableImages[j].id)
                }
            }
            if (group.size > 1) {
                visited.add(hashableImages[i].id)
                groups.add(DuplicateGroup(group))
            }
        }
        return groups
    }

    private fun hammingDistance(a: String, b: String): Int =
        a.zip(b).count { (x, y) -> x != y }
}
