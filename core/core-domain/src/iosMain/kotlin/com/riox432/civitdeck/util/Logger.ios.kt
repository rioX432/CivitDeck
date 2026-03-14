package com.riox432.civitdeck.util

import platform.Foundation.NSLog

actual fun platformLog(level: LogLevel, tag: String, message: String) {
    val prefix = when (level) {
        LogLevel.WARN -> "W"
        LogLevel.ERROR -> "E"
    }
    NSLog("[$prefix/$tag] %@", message)
}
