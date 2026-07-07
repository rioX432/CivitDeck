package com.riox432.civitdeck.ui.components

/**
 * Whether `Modifier.blur` actually renders on this platform. On Android it is
 * backed by RenderEffect and silently does nothing below API 31, so callers
 * that rely on blur to obscure NSFW content need an opaque fallback there.
 */
expect fun isBlurSupported(): Boolean
