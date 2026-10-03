package com.riox432.civitdeck.feature.comfyui.domain.usecase

import com.riox432.civitdeck.domain.model.ComfyUIConnection

/**
 * Parses a raw QR/manual string into a [ComfyUIConnection] draft (id = 0).
 *
 * Accepts:
 * - Full URLs: `http://192.168.1.20:8188`, `https://comfy.example.com`
 * - Host[:port]: `192.168.1.20:8188`, `192.168.1.20`
 *
 * Returns null when no host can be extracted. The default port is
 * [ComfyUIConnection.DEFAULT_COMFYUI_PORT] when omitted.
 */
class ParseConnectionUrlUseCase {

    operator fun invoke(raw: String): ComfyUIConnection? {
        val address = parseAddress(raw) ?: return null
        val useHttps = address.useHttps ?: false
        return draft(address.host, address.port ?: defaultPort(useHttps), useHttps)
    }

    /**
     * Parses the manual-entry host field, where the user may type a bare host or paste a full URL.
     * A scheme or port written in [raw] wins over the form's [fallbackPort] and [fallbackUseHttps].
     * A scheme without a port uses that scheme's default port rather than the port field, since a
     * pasted `https://host` means 443, not whatever the port field still holds.
     */
    fun parseManualEntry(raw: String, fallbackPort: Int, fallbackUseHttps: Boolean): ComfyUIConnection? {
        val address = parseAddress(raw) ?: return null
        val useHttps = address.useHttps ?: fallbackUseHttps
        val port = address.port ?: if (address.useHttps != null) defaultPort(useHttps) else fallbackPort
        return draft(address.host, port, useHttps)
    }

    @Suppress("ReturnCount")
    private fun parseAddress(raw: String): Address? {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return null

        val (useHttps, rest) = when {
            trimmed.startsWith("https://", ignoreCase = true) -> true to trimmed.substring(HTTPS_PREFIX_LENGTH)
            trimmed.startsWith("http://", ignoreCase = true) -> false to trimmed.substring(HTTP_PREFIX_LENGTH)
            else -> null to trimmed
        }

        // Strip any path/query, keep authority only.
        val authority = rest.substringBefore('/').substringBefore('?')
        if (authority.isBlank()) return null

        val (host, portPart) = splitHostAndPort(authority) ?: return null
        if (host.isBlank()) return null
        val port = when {
            portPart.isBlank() -> null
            else -> portPart.toIntOrNull()?.takeIf { it in 1..MAX_PORT } ?: return null
        }
        return Address(host, port, useHttps)
    }

    /**
     * Splits `host[:port]`. A bracketed IPv6 literal such as `[::1]:8188` keeps its brackets so
     * [ComfyUIConnection.baseUrl] stays a valid URL, and its inner colons are not a port separator.
     */
    @Suppress("ReturnCount")
    private fun splitHostAndPort(authority: String): Pair<String, String>? {
        if (!authority.startsWith('[')) {
            val portPart = if (authority.contains(':')) authority.substringAfterLast(':') else ""
            return authority.substringBeforeLast(':', authority) to portPart
        }
        val close = authority.indexOf(']')
        if (close <= 1) return null
        val afterHost = authority.substring(close + 1)
        if (afterHost.isNotEmpty() && !afterHost.startsWith(':')) return null
        return authority.substring(0, close + 1) to afterHost.removePrefix(":")
    }

    private fun defaultPort(useHttps: Boolean): Int =
        if (useHttps) DEFAULT_HTTPS_PORT else ComfyUIConnection.DEFAULT_COMFYUI_PORT

    private fun draft(host: String, port: Int, useHttps: Boolean) = ComfyUIConnection(
        name = host,
        hostname = host,
        port = port,
        useHttps = useHttps,
    )

    /** [port] and [useHttps] are null when the input names no port or no scheme respectively. */
    private class Address(val host: String, val port: Int?, val useHttps: Boolean?)

    private companion object {
        const val HTTP_PREFIX_LENGTH = 7
        const val HTTPS_PREFIX_LENGTH = 8
        const val DEFAULT_HTTPS_PORT = 443
        const val MAX_PORT = 65_535
    }
}
