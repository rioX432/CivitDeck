package com.riox432.civitdeck.domain.export

import kotlin.test.Test
import kotlin.test.assertEquals

class PromptTemplateEngineTest {

    @Test
    fun extractVariables_single() {
        val vars = PromptTemplateEngine.extractVariables("a {subject} photo")
        assertEquals(listOf("subject"), vars)
    }

    @Test
    fun extractVariables_multiple() {
        val vars = PromptTemplateEngine.extractVariables("{subject} in {style} style")
        assertEquals(listOf("subject", "style"), vars)
    }

    @Test
    fun extractVariables_deduplicates() {
        val vars = PromptTemplateEngine.extractVariables("{a} and {a} and {b}")
        assertEquals(listOf("a", "b"), vars)
    }

    @Test
    fun extractVariables_ignoresEscaped() {
        val vars = PromptTemplateEngine.extractVariables("{{literal}} and {real}")
        assertEquals(listOf("real"), vars)
    }

    @Test
    fun extractVariables_empty() {
        val vars = PromptTemplateEngine.extractVariables("no variables here")
        assertEquals(emptyList(), vars)
    }

    @Test
    fun substitute_replaces() {
        val result = PromptTemplateEngine.substitute(
            "a {subject} in {style}",
            mapOf("subject" to "cat", "style" to "watercolor"),
        )
        assertEquals("a cat in watercolor", result)
    }

    @Test
    fun substitute_preservesUnknown() {
        val result = PromptTemplateEngine.substitute(
            "a {subject} in {style}",
            mapOf("subject" to "cat"),
        )
        assertEquals("a cat in {style}", result)
    }

    @Test
    fun substitute_escapedBraces() {
        val result = PromptTemplateEngine.substitute(
            "{{literal}} and {real}",
            mapOf("real" to "value"),
        )
        assertEquals("{literal} and value", result)
    }

    @Test
    fun substitute_noVariables() {
        val result = PromptTemplateEngine.substitute("plain text", emptyMap())
        assertEquals("plain text", result)
    }
}
