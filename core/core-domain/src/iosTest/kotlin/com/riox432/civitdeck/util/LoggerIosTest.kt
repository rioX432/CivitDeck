package com.riox432.civitdeck.util

import kotlin.test.Test

// A crash in NSLog takes down the test process, so returning normally is the assertion.
class LoggerIosTest {

    @Test
    fun plainMessageIsLoggedWithoutCrashing() {
        platformLog(LogLevel.DEBUG, "Ntfy", "Unsubscribed from ntfy")
    }

    @Test
    fun formatSpecifiersInMessageAndTagAreLoggedLiterally() {
        platformLog(LogLevel.WARN, "Tag%@", "progress 100% %@ %d %s %n")
    }

    @Test
    fun errorWithThrowableIsLoggedWithoutCrashing() {
        platformLog(LogLevel.ERROR, "Ntfy", "failed: 50%", IllegalStateException("boom %@"))
    }
}
