package com.riox432.civitdeck.domain.service

/**
 * Platform-specific tracker that reports whether the app is currently in the foreground.
 * Used to suppress local notifications when the user is already looking at the app.
 */
expect class AppLifecycleTracker {
    /** Returns true when the app is in the foreground (visible to the user). */
    val isInForeground: Boolean
}
