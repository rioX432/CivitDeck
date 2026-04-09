package com.riox432.civitdeck.domain.ml

import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.mp.KoinPlatform

/**
 * Android implementation backed by ONNX Runtime Mobile.
 *
 * The Kotlin side loads `ml/siglip2_vision_q4f16.onnx` from the app's `assets/`
 * directory on first access and creates an [OrtSession]. Inference runs on
 * `Dispatchers.Default`, following the latency budget from
 * `docs/research/siglip2-feasibility.md`.
 *
 * **Current status (#701 — wiring only)**: the `.onnx` file is not bundled yet, so
 * [session] lazily resolves to `null`, [isAvailable] returns `false`, and [embed]
 * throws [NotImplementedError]. The Kotlin repository / use cases short-circuit on
 * `isAvailable`, so the rest of the embedding pipeline behaves like a no-op until
 * the follow-up issue ships the model file plus the preprocessing + inference body.
 *
 * **Preprocessing for the future implementation** (per the research doc):
 * - Resize 224×224 BICUBIC
 * - mean / std = `[0.5, 0.5, 0.5]` (NOT the 0.485/0.456/0.406 OpenAI/CLIP stats)
 * - NCHW float32 input
 * - Output L2-normalized 768-d vector so cosine similarity reduces to a dot product
 */
actual class ImageEmbeddingModel actual constructor() {

    private val context: Context? by lazy {
        runCatching { KoinPlatform.getKoin().get<Context>() }.getOrNull()
    }

    /**
     * ONNX Runtime session, lazily loaded from the app bundle. Returns `null` (and
     * stays `null`) when the asset file is missing — expected until a follow-up issue
     * adds the binary. All failures are swallowed intentionally: we want the feature
     * to quietly disable itself, not crash the app, when the bundle is incomplete.
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private val session: OrtSession? by lazy {
        val ctx = context ?: return@lazy null
        try {
            val bytes = ctx.assets.open(ASSET_PATH).use { it.readBytes() }
            OrtEnvironment.getEnvironment().createSession(bytes)
        } catch (ioError: java.io.IOException) {
            null
        } catch (ortError: Exception) {
            null
        }
    }

    actual val isAvailable: Boolean
        get() = session != null

    actual suspend fun embed(imageBytes: ByteArray): FloatArray {
        val ortSession = session
            ?: throw NotImplementedError(
                "SigLIP-2 ONNX model not bundled yet (see #701 follow-up)",
            )
        return withContext(Dispatchers.Default) {
            runInference(ortSession, imageBytes)
        }
    }

    /**
     * Inference body intentionally left as a TODO for the follow-up issue. The
     * wiring PR stops at "session loaded" to keep the diff small and avoid shipping
     * a preprocessing pipeline that cannot be tested against a real model.
     */
    @Suppress("UnusedParameter")
    private fun runInference(ortSession: OrtSession, imageBytes: ByteArray): FloatArray {
        throw NotImplementedError(
            "ONNX inference body deferred — see #701 follow-up for preprocessing + run",
        )
    }

    private companion object {
        private const val ASSET_PATH = "ml/siglip2_vision_q4f16.onnx"
    }
}
