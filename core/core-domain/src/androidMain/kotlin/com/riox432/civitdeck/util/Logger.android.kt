package com.riox432.civitdeck.util

import android.util.Log

actual fun platformLog(level: LogLevel, tag: String, message: String) {
    when (level) {
        LogLevel.WARN -> Log.w(tag, message)
        LogLevel.ERROR -> Log.e(tag, message)
    }
}
