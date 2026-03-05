package com.riox432.civitdeck.util

/**
 * Minimal logging utility for the shared module.
 * Wraps [println] with tag-based formatting so log output
 * can be filtered and disabled globally.
 */
object Logger {
    /** Set to `false` to silence all log output (e.g. in release builds). */
    var enabled: Boolean = true

    /** Warning-level log. */
    fun w(tag: String, message: String) {
        if (enabled) println("[W/$tag] $message")
    }

    /** Error-level log. */
    fun e(tag: String, message: String) {
        if (enabled) println("[E/$tag] $message")
    }
}
