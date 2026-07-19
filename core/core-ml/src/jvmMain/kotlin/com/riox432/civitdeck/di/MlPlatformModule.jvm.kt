package com.riox432.civitdeck.di

import com.riox432.civitdeck.domain.ml.ImageEmbeddingModel
import com.riox432.civitdeck.domain.ml.ImageEmbeddingModelImpl
import com.riox432.civitdeck.domain.ml.TextEmbeddingModel
import com.riox432.civitdeck.domain.ml.TextEmbeddingModelImpl
import org.koin.core.module.Module
import org.koin.dsl.module

actual val mlPlatformModule: Module = module {
    single<ImageEmbeddingModel> { ImageEmbeddingModelImpl() }
    single<TextEmbeddingModel> { TextEmbeddingModelImpl() }
}
