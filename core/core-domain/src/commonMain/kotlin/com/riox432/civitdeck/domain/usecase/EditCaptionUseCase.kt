package com.riox432.civitdeck.domain.usecase

import com.riox432.civitdeck.domain.repository.CaptionRepository

class EditCaptionUseCase(private val repository: CaptionRepository) {
    suspend operator fun invoke(datasetImageId: Long, text: String) = repository.setCaption(datasetImageId, text)
}
