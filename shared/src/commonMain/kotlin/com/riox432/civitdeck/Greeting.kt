package com.riox432.civitdeck

class Greeting {
    private val platform = getPlatform()

    fun greet(): String = "Hello from CivitDeck on ${platform.name}!"
}
