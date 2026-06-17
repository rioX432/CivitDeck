package com.riox432.civitdeck.di

import com.riox432.civitdeck.domain.usecase.EmbedImageUseCase
import com.riox432.civitdeck.domain.usecase.EmbedOnBrowseUseCase
import com.riox432.civitdeck.domain.usecase.FindSimilarModelsByEmbeddingUseCase
import com.riox432.civitdeck.domain.usecase.TextSearchUseCase
import org.koin.dsl.module

// ImageEmbeddingModel and TextEmbeddingModel are registered in domainPlatformModule
// (interface in commonMain + platform impl class).
val embeddingDomainModule = module {
    factory { EmbedImageUseCase(get()) }
    factory { EmbedOnBrowseUseCase(get(), get(), get()) }
    factory { FindSimilarModelsByEmbeddingUseCase(get()) }
    factory { TextSearchUseCase(get(), get()) }
}
