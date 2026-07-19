package com.riox432.civitdeck.di

import com.riox432.civitdeck.domain.ml.ImageEmbeddingModel
import com.riox432.civitdeck.domain.ml.TextEmbeddingModel
import org.koin.dsl.koinApplication
import kotlin.test.Test
import kotlin.test.assertFalse

/**
 * Flavor-conditional wiring: the fdroid flavor must bind on-device embedding to
 * unavailable no-ops (no :core:core-ml dependency). Guards against a regression that
 * would drag ONNX Runtime back into the F-Droid build.
 */
class FdroidEmbeddingModuleTest {

    @Test
    fun fdroidBindsUnavailableEmbeddingModels() {
        val koin = koinApplication { modules(embeddingModule) }.koin

        assertFalse(koin.get<ImageEmbeddingModel>().isAvailable)
        assertFalse(koin.get<TextEmbeddingModel>().isAvailable)
    }
}
