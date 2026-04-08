package com.riox432.civitdeck.domain.ml

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * iOS implementation backed by a Swift Core ML bridge.
 *
 * The actual ML work happens in Swift (`iosApp/iosApp/ML/SigLIP2Embedder.swift`) because
 * Core ML is not directly callable from Kotlin/Native. At app startup, Swift registers
 * a [SigLIP2Bridge] via [registerSigLIP2Bridge]; this class delegates to it.
 *
 * Until the SigLIP-2 `.mlpackage` is shipped (tracked separately as a follow-up to #700),
 * the Swift bridge reports [SigLIP2Bridge.isAvailable] = false and [embed] throws.
 * The repository / use cases short-circuit on `isAvailable`, so the rest of the system
 * keeps working.
 */
actual class ImageEmbeddingModel actual constructor() {

    actual val isAvailable: Boolean
        get() = sigLIP2Bridge?.isAvailable == true

    @Suppress("ThrowsCount")
    actual suspend fun embed(imageBytes: ByteArray): FloatArray {
        val bridge = sigLIP2Bridge
            ?: throw NotImplementedError(
                "SigLIP2Bridge not registered — call registerSigLIP2Bridge from Swift",
            )
        if (!bridge.isAvailable) {
            throw NotImplementedError("SigLIP2 Core ML model not bundled yet (see #700 follow-up)")
        }
        val result = withContext(Dispatchers.Default) {
            suspendCancellableCoroutine { cont ->
                bridge.embed(imageBytes) { vector ->
                    cont.resume(vector)
                }
            }
        }
        return result
            ?: throw NotImplementedError("SigLIP2 embedding failed (bridge returned null)")
    }
}
