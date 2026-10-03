package com.riox432.civitdeck

import android.net.Uri
import coil3.memory.MemoryCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Removes Coil memory-cache entries for a ComfyUI host:port whenever its pinned certificate is
 * confirmed, changed or cleared, so an image fetched under the old trust is not shown again for
 * the same URL. The disk cache needs no eviction because [ComfyUIPinnedFetcherFactory] keys its
 * entries by pin.
 */
class ComfyUIImageCacheEvictor(
    private val pinsByHostPort: Flow<Map<String, String>>,
    private val memoryCache: () -> MemoryCache?,
) {
    fun start(scope: CoroutineScope): Job = scope.launch {
        // Starts empty so the first emission also evicts entries loaded with system trust before
        // the stored pins were known.
        var previous = emptyMap<String, String>()
        pinsByHostPort.collect { current ->
            val changed = (previous.keys + current.keys).filterTo(HashSet()) { previous[it] != current[it] }
            previous = current
            if (changed.isNotEmpty()) evict(changed)
        }
    }

    private fun evict(hostPorts: Set<String>) {
        val cache = memoryCache() ?: return
        cache.keys
            .filter { hostPortOf(it.key) in hostPorts }
            .forEach { cache.remove(it) }
    }

    /** Same `host:port` form as the pin map. Pins apply only to `https`, so other URLs are skipped. */
    private fun hostPortOf(url: String): String? {
        val uri = Uri.parse(url)
        if (uri.scheme != HTTPS) return null
        val host = uri.host ?: return null
        val port = uri.port.takeIf { it != -1 } ?: HTTPS_PORT
        return "${host.lowercase()}:$port"
    }

    private companion object {
        const val HTTPS = "https"
        const val HTTPS_PORT = 443
    }
}
