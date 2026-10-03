package com.riox432.civitdeck.util

import platform.Foundation.NSLog

actual fun platformLog(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
    val prefix = when (level) {
        LogLevel.DEBUG -> "D"
        LogLevel.WARN -> "W"
        LogLevel.ERROR -> "E"
    }
    val suffix = if (throwable != null) "\n${throwable.stackTraceToString()}" else ""
    // A Kotlin String passed as a variadic NSLog argument does not reach `%@` as an NSString
    // and crashes with EXC_BAD_ACCESS, so the whole line goes in the format with `%` escaped.
    NSLog("[$prefix/$tag] $message$suffix".replace("%", "%%"))
}
