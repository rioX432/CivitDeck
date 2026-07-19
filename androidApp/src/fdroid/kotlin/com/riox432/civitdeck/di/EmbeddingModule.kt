package com.riox432.civitdeck.di

import com.riox432.civitdeck.domain.ml.ImageEmbeddingModel
import com.riox432.civitdeck.domain.ml.TextEmbeddingModel
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * fdroid ships no on-device ML — it does not depend on `:core:core-ml`, so ONNX Runtime
 * and its native libraries are absent from the APK. These unavailable stubs keep the
 * embedding use-case graph resolvable: `EmbedImageUseCase` is injected into an
 * always-created detail ViewModel and short-circuits on `isAvailable == false`.
 */
val embeddingModule: Module = module {
    single<ImageEmbeddingModel> { UnavailableImageEmbeddingModel }
    single<TextEmbeddingModel> { UnavailableTextEmbeddingModel }
}

private object UnavailableImageEmbeddingModel : ImageEmbeddingModel {
    override val isAvailable: Boolean = false
    override suspend fun embed(imageBytes: ByteArray): FloatArray =
        throw NotImplementedError("On-device embedding is unavailable in the F-Droid build")
}

private object UnavailableTextEmbeddingModel : TextEmbeddingModel {
    override val isAvailable: Boolean = false
    override val embeddingModelId: String = "unavailable"
    override suspend fun embed(text: String): FloatArray =
        throw NotImplementedError("On-device text embedding is unavailable in the F-Droid build")
}
