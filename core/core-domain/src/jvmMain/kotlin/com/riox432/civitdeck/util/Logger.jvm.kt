package com.riox432.civitdeck.util

import java.util.logging.Level
import java.util.logging.Logger as JulLogger

actual fun platformLog(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
    val logger = JulLogger.getLogger(tag)
    when (level) {
        LogLevel.DEBUG -> logger.log(Level.FINE, message, throwable)
        LogLevel.WARN -> logger.log(Level.WARNING, message, throwable)
        LogLevel.ERROR -> logger.log(Level.SEVERE, message, throwable)
    }
}
