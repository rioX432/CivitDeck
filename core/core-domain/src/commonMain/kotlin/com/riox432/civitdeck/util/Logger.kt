package com.riox432.civitdeck.util

/**
 * Minimal logging utility for the shared module.
 * Uses platform-specific logging via expect/actual.
 */
object Logger {
    /** Set to `false` to silence all log output (e.g. in release builds). */
    var enabled: Boolean = true

    /** Debug-level log. */
    fun d(tag: String, message: String) {
        if (enabled) platformLog(LogLevel.DEBUG, tag, message)
    }

    /** Warning-level log. */
    fun w(tag: String, message: String) {
        if (enabled) platformLog(LogLevel.WARN, tag, message)
    }

    /** Error-level log. */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (enabled) platformLog(LogLevel.ERROR, tag, message, throwable)
    }
}

enum class LogLevel { DEBUG, WARN, ERROR }

expect fun platformLog(level: LogLevel, tag: String, message: String, throwable: Throwable? = null)
