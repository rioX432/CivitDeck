package com.riox432.civitdeck.domain.repository

import com.riox432.civitdeck.domain.model.Caption

interface CaptionRepository {
    suspend fun getCaption(datasetImageId: Long): Caption?
    suspend fun setCaption(datasetImageId: Long, text: String)
}
