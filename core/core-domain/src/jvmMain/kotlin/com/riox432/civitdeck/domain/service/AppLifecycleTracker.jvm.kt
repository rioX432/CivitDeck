package com.riox432.civitdeck.domain.service

/**
 * JVM/Desktop implementation. Desktop is always considered foreground
 * since the window is directly accessible to the user.
 */
actual class AppLifecycleTracker {
    actual val isInForeground: Boolean = true
}
