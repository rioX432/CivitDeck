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

    @Suppress("ReturnCount")
    operator fun invoke(raw: String): ComfyUIConnection? {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return null

        val (useHttps, rest) = when {
            trimmed.startsWith("https://", ignoreCase = true) -> true to trimmed.substring(HTTPS_PREFIX_LENGTH)
            trimmed.startsWith("http://", ignoreCase = true) -> false to trimmed.substring(HTTP_PREFIX_LENGTH)
            else -> false to trimmed
        }

        // Strip any path/query, keep authority only.
        val authority = rest.substringBefore('/').substringBefore('?')
        if (authority.isBlank()) return null

        val host = authority.substringBeforeLast(':', authority).ifBlank { return null }
        val portPart = if (authority.contains(':')) authority.substringAfterLast(':') else ""
        val port = when {
            portPart.isBlank() -> if (useHttps) DEFAULT_HTTPS_PORT else ComfyUIConnection.DEFAULT_COMFYUI_PORT
            else -> portPart.toIntOrNull()?.takeIf { it in 1..MAX_PORT } ?: return null
        }

        return ComfyUIConnection(
            name = host,
            hostname = host,
            port = port,
            useHttps = useHttps,
        )
    }

    private companion object {
        const val HTTP_PREFIX_LENGTH = 7
        const val HTTPS_PREFIX_LENGTH = 8
        const val DEFAULT_HTTPS_PORT = 443
        const val MAX_PORT = 65_535
    }
}
