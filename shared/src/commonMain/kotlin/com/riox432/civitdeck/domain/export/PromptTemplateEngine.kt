package com.riox432.civitdeck.domain.export

/**
 * Simple template engine for prompt variable substitution.
 * Variables use `{name}` syntax. Escape with `{{` and `}}` for literal braces.
 */
object PromptTemplateEngine {

    private val variablePattern = Regex("""\{([^{}]+)\}""")

    fun extractVariables(template: String): List<String> {
        val escaped = template.replace("{{", OPEN_PLACEHOLDER).replace("}}", CLOSE_PLACEHOLDER)
        return variablePattern.findAll(escaped)
            .map { it.groupValues[1].trim() }
            .distinct()
            .toList()
    }

    fun substitute(template: String, values: Map<String, String>): String {
        val escaped = template.replace("{{", OPEN_PLACEHOLDER).replace("}}", CLOSE_PLACEHOLDER)
        val result = variablePattern.replace(escaped) { match ->
            val key = match.groupValues[1].trim()
            values[key] ?: match.value
        }
        return result.replace(OPEN_PLACEHOLDER, "{").replace(CLOSE_PLACEHOLDER, "}")
    }

    private const val OPEN_PLACEHOLDER = "\u0000OPEN\u0000"
    private const val CLOSE_PLACEHOLDER = "\u0000CLOSE\u0000"
}
