package com.riox432.civitdeck.domain.service

/**
 * JVM/Desktop implementation. Desktop is always considered foreground
 * since the window is directly accessible to the user.
 */
class AppLifecycleTrackerImpl : AppLifecycleTracker {
    override val isInForeground: Boolean = true
}
