package com.riox432.civitdeck.domain.ml

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.mp.KoinPlatform
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

/**
 * Android implementation backed by ONNX Runtime Mobile.
 *
 * Loads `ml/siglip2_vision_q4f16.onnx` from the app's `assets/` directory on
 * first access and creates an [OrtSession]. Inference runs on
 * `Dispatchers.Default`, following the latency budget from
 * `docs/research/siglip2-feasibility.md`.
 *
 * Preprocessing (per the SigLIP image processor defaults):
 * - Resize 224x224 (bilinear via Bitmap.createScaledBitmap — Android has no
 *   explicit BICUBIC, bilinear is the platform default and visually equivalent
 *   at this resolution)
 * - Rescale 1/255, then normalize mean/std = [0.5, 0.5, 0.5]
 *   => effectively: (pixel / 255.0 - 0.5) / 0.5 = pixel / 127.5 - 1.0
 * - NCHW float32 layout
 * - Output L2-normalized 768-d vector
 */
actual class ImageEmbeddingModel actual constructor() {

    private val context: Context? by lazy {
        runCatching { KoinPlatform.getKoin().get<Context>() }.getOrNull()
    }

    /**
     * ONNX Runtime environment, shared across sessions. Calling
     * `getEnvironment()` multiple times returns the same singleton.
     */
    private val ortEnv: OrtEnvironment by lazy { OrtEnvironment.getEnvironment() }

    /**
     * ONNX Runtime session, lazily loaded from the app bundle. Returns `null`
     * when the asset file is missing or unreadable.
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private val session: OrtSession? by lazy {
        val ctx = context ?: return@lazy null
        try {
            val bytes = ctx.assets.open(ASSET_PATH).use { it.readBytes() }
            ortEnv.createSession(bytes)
        } catch (_: java.io.IOException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    actual val isAvailable: Boolean
        get() = session != null

    actual suspend fun embed(imageBytes: ByteArray): FloatArray {
        val ortSession = session
            ?: throw NotImplementedError(
                "SigLIP-2 ONNX model not available — asset missing or load failed",
            )
        return withContext(Dispatchers.Default) {
            runInference(ortSession, imageBytes)
        }
    }

    /**
     * Decodes [imageBytes] into a 224x224 bitmap, packs into an NCHW float
     * tensor, runs the ONNX vision model, and L2-normalizes the output.
     */
    private fun runInference(ortSession: OrtSession, imageBytes: ByteArray): FloatArray {
        val inputTensor = preprocessImage(imageBytes)
        try {
            return executeAndNormalize(ortSession, inputTensor)
        } finally {
            inputTensor.close()
        }
    }

    private companion object {
        private const val ASSET_PATH = "ml/siglip2_vision_q4f16.onnx"
        private const val IMAGE_SIZE = 224
        private const val CHANNELS = 3
        private const val INPUT_NAME = "pixel_values"
        private const val OUTPUT_NAME = "pooler_output"
        private const val EMBEDDING_DIM = 768

        /** Normalization: pixel / 127.5 - 1.0 (equivalent to (p/255 - 0.5) / 0.5) */
        private const val SCALE = 127.5f
    }

    /**
     * Decodes JPEG/PNG bytes into a 224x224 bitmap, then packs pixels into an
     * NCHW float32 [OnnxTensor] with SigLIP normalization.
     */
    @Suppress("MagicNumber")
    private fun preprocessImage(imageBytes: ByteArray): OnnxTensor {
        val bitmap = decodeBitmap(imageBytes)
        val floatBuf = bitmapToNchwBuffer(bitmap)
        bitmap.recycle()

        val shape = longArrayOf(1, CHANNELS.toLong(), IMAGE_SIZE.toLong(), IMAGE_SIZE.toLong())
        return OnnxTensor.createTensor(ortEnv, floatBuf, shape)
    }

    /** Decodes raw bytes into a 224x224 ARGB_8888 bitmap. */
    private fun decodeBitmap(imageBytes: ByteArray): Bitmap {
        val original = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            ?: throw IllegalArgumentException("Failed to decode image bytes")
        return if (original.width == IMAGE_SIZE && original.height == IMAGE_SIZE) {
            original
        } else {
            val scaled = Bitmap.createScaledBitmap(original, IMAGE_SIZE, IMAGE_SIZE, true)
            if (scaled !== original) original.recycle()
            scaled
        }
    }

    /**
     * Packs a 224x224 bitmap into a direct [java.nio.FloatBuffer] in NCHW
     * layout with SigLIP normalization (mean=0.5, std=0.5 per channel).
     */
    @Suppress("MagicNumber")
    private fun bitmapToNchwBuffer(bitmap: Bitmap): java.nio.FloatBuffer {
        val pixels = IntArray(IMAGE_SIZE * IMAGE_SIZE)
        bitmap.getPixels(pixels, 0, IMAGE_SIZE, 0, 0, IMAGE_SIZE, IMAGE_SIZE)

        val floatCount = 1 * CHANNELS * IMAGE_SIZE * IMAGE_SIZE
        val buffer = ByteBuffer
            .allocateDirect(floatCount * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()

        // NCHW layout: all R values, then all G values, then all B values
        for (c in 0 until CHANNELS) {
            for (pixel in pixels) {
                val channel = when (c) {
                    0 -> (pixel shr 16) and 0xFF // R
                    1 -> (pixel shr 8) and 0xFF // G
                    else -> pixel and 0xFF // B
                }
                buffer.put(channel.toFloat() / SCALE - 1.0f)
            }
        }
        buffer.rewind()
        return buffer
    }

    /**
     * Runs the ONNX session and L2-normalizes the pooler_output embedding.
     */
    @Suppress("UNCHECKED_CAST")
    private fun executeAndNormalize(ortSession: OrtSession, inputTensor: OnnxTensor): FloatArray {
        val results = ortSession.run(mapOf(INPUT_NAME to inputTensor))
        results.use { output ->
            val poolerTensor = output.get(OUTPUT_NAME)
                .orElseThrow { IllegalStateException("Model missing '$OUTPUT_NAME' output") }
            val raw = (poolerTensor as OnnxTensor).floatBuffer
            val embedding = FloatArray(EMBEDDING_DIM)
            raw.rewind()
            raw.get(embedding)
            return l2Normalize(embedding)
        }
    }

    /** L2-normalizes [vec] in place and returns it. */
    private fun l2Normalize(vec: FloatArray): FloatArray {
        var sumSq = 0.0f
        for (v in vec) sumSq += v * v
        val norm = sqrt(sumSq)
        if (norm > 0f) {
            for (i in vec.indices) vec[i] /= norm
        }
        return vec
    }
}
