package com.riox432.civitdeck.di

import com.riox432.civitdeck.domain.ml.ImageEmbeddingModel
import com.riox432.civitdeck.domain.ml.TextEmbeddingModel
import com.riox432.civitdeck.domain.usecase.EmbedImageUseCase
import com.riox432.civitdeck.domain.usecase.EmbedOnBrowseUseCase
import com.riox432.civitdeck.domain.usecase.FindSimilarModelsByEmbeddingUseCase
import com.riox432.civitdeck.domain.usecase.TextSearchUseCase
import org.koin.dsl.module

val embeddingDomainModule = module {
    single { ImageEmbeddingModel() }
    single { TextEmbeddingModel() }
    factory { EmbedImageUseCase(get()) }
    factory { EmbedOnBrowseUseCase(get(), get(), get()) }
    factory { FindSimilarModelsByEmbeddingUseCase(get()) }
    factory { TextSearchUseCase(get(), get()) }
}
