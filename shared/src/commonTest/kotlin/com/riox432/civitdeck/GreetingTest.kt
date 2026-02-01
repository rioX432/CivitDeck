package com.riox432.civitdeck

import kotlin.test.Test
import kotlin.test.assertTrue

class GreetingTest {
    @Test
    fun greetingContainsCivitDeck() {
        val greeting = Greeting().greet()
        assertTrue(greeting.contains("CivitDeck"), "Greeting should contain 'CivitDeck'")
    }
}
