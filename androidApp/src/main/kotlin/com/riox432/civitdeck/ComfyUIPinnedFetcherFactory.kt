package com.riox432.civitdeck

import coil3.ImageLoader
import coil3.Uri
import coil3.fetch.Fetcher
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.Options
import coil3.toAndroidUri
import com.riox432.civitdeck.data.api.comfyui.ComfyUIServerTrust
import com.riox432.civitdeck.data.api.comfyui.trustOnlyPinnedLeaf
import okhttp3.OkHttpClient
import java.util.concurrent.ConcurrentHashMap

/**
 * Fetches `https` images from a ComfyUI host:port with a confirmed certificate pin over a client
 * that trusts only that certificate. Returns null for every other URI so the default fetcher,
 * with system trust, handles CivitAI images and unpinned servers.
 */
class ComfyUIPinnedFetcherFactory(
    private val pinnedSha256For: (host: String, port: Int) -> String?,
) : Fetcher.Factory<Uri> {

    private val factoriesByPin = ConcurrentHashMap<String, Fetcher.Factory<Uri>>()

    override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
        if (data.scheme != HTTPS) return null
        val uri = data.toAndroidUri()
        val host = uri.host ?: return null
        val port = uri.port.takeIf { it != -1 } ?: HTTPS_PORT
        val pin = pinnedSha256For(host, port) ?: return null
        val factory = factoriesByPin.getOrPut(pin) {
            OkHttpNetworkFetcherFactory(callFactory = { pinnedClient(pin) })
        }
        // The disk cache keys by URL only. Adding the pin means an entry written under one pin is
        // never read under another pin or under system trust, so a pin change needs no disk
        // eviction.
        val diskCacheKey = "${options.diskCacheKey ?: data}#sha256=$pin"
        return factory.create(data, options.copy(diskCacheKey = diskCacheKey), imageLoader)
    }

    private fun pinnedClient(pin: String): OkHttpClient =
        OkHttpClient.Builder()
            .trustOnlyPinnedLeaf(ComfyUIServerTrust.PinnedLeaf(pin))
            .build()

    private companion object {
        const val HTTPS = "https"
        const val HTTPS_PORT = 443
    }
}
