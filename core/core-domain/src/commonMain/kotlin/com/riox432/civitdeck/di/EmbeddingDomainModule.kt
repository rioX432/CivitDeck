package com.riox432.civitdeck.di

import com.riox432.civitdeck.domain.ml.ImageEmbeddingModel
import com.riox432.civitdeck.domain.usecase.EmbedImageUseCase
import com.riox432.civitdeck.domain.usecase.FindSimilarModelsByEmbeddingUseCase
import org.koin.dsl.module

val embeddingDomainModule = module {
    single { ImageEmbeddingModel() }
    factory { EmbedImageUseCase(get()) }
    factory { FindSimilarModelsByEmbeddingUseCase(get()) }
}
