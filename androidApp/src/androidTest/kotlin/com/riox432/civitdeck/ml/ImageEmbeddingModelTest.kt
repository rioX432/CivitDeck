package com.riox432.civitdeck.ml

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Environment
import androidx.test.platform.app.InstrumentationRegistry
import com.riox432.civitdeck.domain.ml.ImageEmbeddingModel
import com.riox432.civitdeck.domain.ml.ImageEmbeddingModelImpl
import kotlinx.coroutines.test.runTest
import org.junit.Assume.assumeTrue
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.sqrt
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

    private fun createModel(): ImageEmbeddingModel =
        ImageEmbeddingModelImpl(InstrumentationRegistry.getInstrumentation().targetContext)

    @Test
    fun modelIsAvailable() {
        val model = createModel()
        assertTrue(model.isAvailable, "ONNX model should load from assets")
    }

    @Test
    fun embedReturns768dVector() = runTest {
        val model = createModel()
        val imageBytes = createTestImage()
        val embedding = model.embed(imageBytes)
        assertTrue(
            embedding.size == EMBEDDING_DIM,
            "Expected $EMBEDDING_DIM-d embedding, got ${embedding.size}",
        )
    }

    @Test
    fun embeddingIsL2Normalized() = runTest {
        val model = createModel()
        val embedding = model.embed(createTestImage())
        val norm = sqrt(embedding.fold(0.0f) { acc, v -> acc + v * v })
        assertTrue(
            abs(norm - 1.0f) < NORM_TOLERANCE,
            "L2 norm should be ~1.0, got $norm",
        )
    }

    @Test
    fun determinism_sameInputProducesSameEmbedding() = runTest {
        val model = createModel()
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

    // TODO(#707): Current reference vector was generated from the Android ONNX
    // model (siglip2_vision_q4f16.onnx). Cross-validation against the HuggingFace
    // Python pipeline is still pending — update reference_embedding.bin once the
    // Python-generated vector is available.

    /**
     * Compares the model output against a known-good reference embedding.
     *
     * The reference was generated from the same Android ONNX model and checked
     * into `androidTest/assets/ml/reference_embedding.bin`. If the file is
     * missing, the test is skipped — run [generateReferenceEmbedding] first
     * to produce it.
     */
    @Test
    fun embeddingMatchesReferenceVector() = runTest {
        val referenceBytes = loadReferenceEmbedding()
        assumeTrue(
            "Reference embedding not found — run generateReferenceEmbedding " +
                "on a device and copy the output to " +
                "androidApp/src/androidTest/assets/ml/reference_embedding.bin",
            referenceBytes != null,
        )
        val reference = bytesToFloatArray(referenceBytes!!)
        assertTrue(
            reference.size == EMBEDDING_DIM,
            "Reference has ${reference.size} dims, expected $EMBEDDING_DIM",
        )

        val model = createModel()
        val embedding = model.embed(createTestImage())
        val similarity = cosineSimilarity(embedding, reference)
        assertTrue(
            similarity >= REFERENCE_SIMILARITY_THRESHOLD,
            "Cosine similarity vs reference should be >= $REFERENCE_SIMILARITY_THRESHOLD, " +
                "got $similarity",
        )
    }

    /**
     * One-shot helper: generates a reference embedding from the synthetic test
     * image and writes it to the device's Downloads folder.
     *
     * Run manually on a device, then extract with:
     * ```
     * adb pull /sdcard/Download/reference_embedding.bin \
     *     androidApp/src/androidTest/assets/ml/reference_embedding.bin
     * ```
     */
    @Test
    fun generateReferenceEmbedding() = runTest {
        val model = createModel()
        val embedding = model.embed(createTestImage())
        val bytes = floatArrayToBytes(embedding)
        val outDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS,
        )
        val outFile = File(outDir, REFERENCE_FILE_NAME)
        outFile.writeBytes(bytes)
        assertTrue(
            outFile.exists() && outFile.length() == EXPECTED_REFERENCE_SIZE,
            "Reference written to ${outFile.absolutePath} " +
                "(${outFile.length()} bytes)",
        )
    }

    /**
     * Creates a synthetic 224x224 JPEG test image (gradient pattern).
     * Using a deterministic pattern ensures reproducible embeddings.
     */
    @Suppress("MagicNumber") // RGB gradient calculation (255, 2) — standard 8-bit color range
    private fun createTestImage(): ByteArray {
        val size = SIGLIP_INPUT_SIZE
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
        bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)
        bitmap.recycle()
        return stream.toByteArray()
    }

    /**
     * Loads the reference embedding from androidTest assets.
     * Returns null if the file is not found.
     */
    @Suppress("SwallowedException")
    private fun loadReferenceEmbedding(): ByteArray? {
        val ctx = InstrumentationRegistry.getInstrumentation().context
        return try {
            ctx.assets.open(REFERENCE_ASSET_PATH).use { it.readBytes() }
        } catch (_: java.io.IOException) {
            null
        }
    }

    private companion object {
        private const val EMBEDDING_DIM = 768
        private const val SIGLIP_INPUT_SIZE = 224
        private const val JPEG_QUALITY = 95
        private const val NORM_TOLERANCE = 0.01f
        private const val DETERMINISM_TOLERANCE = 1e-6f
        private const val REFERENCE_SIMILARITY_THRESHOLD = 0.999f
        private const val REFERENCE_FILE_NAME = "reference_embedding.bin"
        private const val REFERENCE_ASSET_PATH = "ml/$REFERENCE_FILE_NAME"

        /** 768 floats * 4 bytes each = 3072 bytes */
        private const val EXPECTED_REFERENCE_SIZE = EMBEDDING_DIM * Float.SIZE_BYTES.toLong()

        /** Converts a [FloatArray] to raw little-endian bytes. */
        private fun floatArrayToBytes(array: FloatArray): ByteArray {
            val buffer = ByteBuffer
                .allocate(array.size * Float.SIZE_BYTES)
                .order(ByteOrder.LITTLE_ENDIAN)
            for (v in array) buffer.putFloat(v)
            return buffer.array()
        }

        /** Converts raw little-endian bytes back to a [FloatArray]. */
        private fun bytesToFloatArray(bytes: ByteArray): FloatArray {
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val result = FloatArray(bytes.size / Float.SIZE_BYTES)
            buffer.asFloatBuffer().get(result)
            return result
        }

        /** Cosine similarity between two vectors. */
        private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
            var dot = 0.0f
            var normA = 0.0f
            var normB = 0.0f
            for (i in a.indices) {
                dot += a[i] * b[i]
                normA += a[i] * a[i]
                normB += b[i] * b[i]
            }
            return dot / (sqrt(normA) * sqrt(normB))
        }
    }
}
