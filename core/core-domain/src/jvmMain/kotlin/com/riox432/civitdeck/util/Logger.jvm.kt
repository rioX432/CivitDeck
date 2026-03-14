package com.riox432.civitdeck.util

import java.util.logging.Level
import java.util.logging.Logger as JulLogger

actual fun platformLog(level: LogLevel, tag: String, message: String) {
    val logger = JulLogger.getLogger(tag)
    when (level) {
        LogLevel.WARN -> logger.log(Level.WARNING, message)
        LogLevel.ERROR -> logger.log(Level.SEVERE, message)
    }
}
