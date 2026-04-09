package com.riox432.civitdeck.ml

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.platform.app.InstrumentationRegistry
import com.riox432.civitdeck.domain.ml.ImageEmbeddingModel
import kotlinx.coroutines.test.runTest
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import java.io.ByteArrayOutputStream
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Instrumented tests for [ImageEmbeddingModel] (Android ONNX Runtime).
 *
 * These tests must run on a device or emulator because:
 * - ONNX Runtime loads native `.so` libs from the AAR
 * - The model file lives in the app's `assets/ml/` directory
 *
 * Run with: `./gradlew :androidApp:connectedAndroidTest`
 */
class ImageEmbeddingModelTest {

    @BeforeTest
    fun setUp() {
        if (GlobalContext.getOrNull() == null) {
            val ctx = InstrumentationRegistry.getInstrumentation().targetContext
            startKoin { androidContext(ctx) }
        }
    }

    @AfterTest
    fun tearDown() {
        runCatching { stopKoin() }
    }

    @Test
    fun modelIsAvailable() {
        val model = ImageEmbeddingModel()
        assertTrue(model.isAvailable, "ONNX model should load from assets")
    }

    @Test
    fun embedReturns768dVector() = runTest {
        val model = ImageEmbeddingModel()
        val imageBytes = createTestImage()
        val embedding = model.embed(imageBytes)
        assertTrue(
            embedding.size == EMBEDDING_DIM,
            "Expected $EMBEDDING_DIM-d embedding, got ${embedding.size}",
        )
    }

    @Test
    fun embeddingIsL2Normalized() = runTest {
        val model = ImageEmbeddingModel()
        val embedding = model.embed(createTestImage())
        val norm = sqrt(embedding.fold(0.0f) { acc, v -> acc + v * v })
        assertTrue(
            abs(norm - 1.0f) < NORM_TOLERANCE,
            "L2 norm should be ~1.0, got $norm",
        )
    }

    @Test
    fun determinism_sameInputProducesSameEmbedding() = runTest {
        val model = ImageEmbeddingModel()
        val imageBytes = createTestImage()
        val first = model.embed(imageBytes)
        val second = model.embed(imageBytes)
        val maxDelta = first.zip(second.toList())
            .maxOf { (a, b) -> abs(a - b) }
        assertTrue(
            maxDelta < DETERMINISM_TOLERANCE,
            "Same input should produce identical output (max delta=$maxDelta)",
        )
    }

    // TODO(#707): Add reference vector comparison once a known-good embedding
    // is generated from the HuggingFace Python pipeline and checked into
    // androidApp/src/androidTest/assets/ml/reference_embedding.bin

    /**
     * Creates a synthetic 224x224 JPEG test image (gradient pattern).
     * Using a deterministic pattern ensures reproducible embeddings.
     */
    @Suppress("MagicNumber")
    private fun createTestImage(): ByteArray {
        val size = 224
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (y in 0 until size) {
            for (x in 0 until size) {
                val r = (x * 255) / size
                val g = (y * 255) / size
                val b = ((x + y) * 255) / (2 * size)
                bitmap.setPixel(x, y, Color.rgb(r, g, b))
            }
        }
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
        bitmap.recycle()
        return stream.toByteArray()
    }

    private companion object {
        private const val EMBEDDING_DIM = 768
        private const val NORM_TOLERANCE = 0.01f
        private const val DETERMINISM_TOLERANCE = 1e-6f
    }
}
