package com.riox432.civitdeck.domain.ml

/**
 * iOS-side bridge implemented in Swift to run SigLIP-2 image embedding on Core ML.
 *
 * Lives in `commonMain` (rather than `iosMain`) only so that the symbols are visible
 * to the Swift side through the shared umbrella framework — Kotlin/Native does not
 * export iosMain types from dependency modules. Android and Desktop never reference
 * this interface; their `ImageEmbeddingModel` actuals ignore [sigLIP2Bridge] entirely.
 *
 * The Kotlin side cannot host the model directly (Core ML is Swift / Objective-C),
 * so the Swift implementation is registered at app startup via [registerSigLIP2Bridge]
 * and consumed by the iOS [ImageEmbeddingModel] actual through the [sigLIP2Bridge] holder.
 *
 * The interface uses a callback rather than `suspend` because Swift cannot reliably
 * implement Kotlin suspend functions across the SKIE/K-N boundary. The Kotlin actual
 * wraps the callback in `suspendCancellableCoroutine` to expose a normal coroutine API
 * to the rest of the codebase.
 */
interface SigLIP2Bridge {
    /** True when the Swift side has loaded the Core ML model and can produce embeddings. */
    val isAvailable: Boolean

    /** Identifier of the underlying model — used as the cache key in the embeddings DB. */
    val embeddingModelId: String

    /** Length of the returned vector. */
    val dimension: Int

    /**
     * Embeds the given encoded image bytes (JPEG/PNG) into a fixed-length vector.
     *
     * Calls [onResult] with the embedding when inference succeeds, or null on failure
     * (model unavailable, decode error, inference error). Implementations MUST return
     * an L2-normalized vector of length [dimension].
     *
     * The callback may be invoked on any thread; the Kotlin caller is responsible for
     * dispatching back to its own coroutine context.
     */
    fun embed(imageBytes: ByteArray, onResult: (FloatArray?) -> Unit)
}

/**
 * Holder for the registered Swift bridge. Mutable on purpose: the Swift side calls
 * [registerSigLIP2Bridge] exactly once during app startup. Reads happen later from
 * [ImageEmbeddingModel], by which point the bridge is set.
 *
 * Kept package-private to discourage anyone else from poking it.
 */
internal var sigLIP2Bridge: SigLIP2Bridge? = null
    private set

/**
 * Called from Swift `iOSApp.swift` after `doInitKoin` so the platform actual of
 * [ImageEmbeddingModel] can find the Core ML implementation.
 *
 * Safe to call multiple times — the most recent registration wins, which simplifies
 * hot-reload during development.
 */
fun registerSigLIP2Bridge(bridge: SigLIP2Bridge) {
    sigLIP2Bridge = bridge
}
