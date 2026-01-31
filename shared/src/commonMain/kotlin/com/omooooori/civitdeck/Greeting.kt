package com.omooooori.civitdeck

class Greeting {
    private val platform = getPlatform()

    fun greet(): String = "Hello from CivitDeck on ${platform.name}!"
}
