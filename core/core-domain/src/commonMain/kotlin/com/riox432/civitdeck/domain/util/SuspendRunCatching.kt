package com.riox432.civitdeck.domain.util

import com.riox432.civitdeck.util.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * Like [runCatching], but rethrows [CancellationException] so that structured
 * concurrency is not silently broken.
 */
inline fun <T> suspendRunCatching(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}

/**
 * Launch a coroutine that wraps [block] in [suspendRunCatching] and logs
 * failures via [Logger.w]. Useful for fire-and-forget operations where
 * the caller only needs to log errors.
 */
fun CoroutineScope.launchSafe(
    tag: String,
    operationName: String,
    block: suspend () -> Unit,
) {
    launch {
        suspendRunCatching { block() }
            .onFailure { e -> Logger.w(tag, "$operationName failed: ${e.message}") }
    }
}
