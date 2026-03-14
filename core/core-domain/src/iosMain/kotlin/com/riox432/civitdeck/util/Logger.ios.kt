package com.riox432.civitdeck.util

import platform.Foundation.NSLog

actual fun platformLog(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
    val prefix = when (level) {
        LogLevel.DEBUG -> "D"
        LogLevel.WARN -> "W"
        LogLevel.ERROR -> "E"
    }
    val suffix = if (throwable != null) "\n${throwable.stackTraceToString()}" else ""
    NSLog("[$prefix/$tag] %@", message + suffix)
}
