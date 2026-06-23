package com.riox432.civitdeck.feature.comfyui.domain.usecase

import com.riox432.civitdeck.feature.comfyui.domain.model.ExtractedParameter
import com.riox432.civitdeck.feature.comfyui.domain.model.ParameterType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Covers [InjectWorkflowParametersUseCase]: parameter values are written back into
 * the workflow JSON with the correct primitive type per [ParameterType], untouched
 * nodes/inputs are preserved, and malformed JSON is returned unchanged.
 */
class InjectWorkflowParametersUseCaseTest {

    private val useCase = InjectWorkflowParametersUseCase()

    private fun param(
        nodeId: String,
        paramName: String,
        type: ParameterType,
        value: String,
    ) = ExtractedParameter(
        nodeId = nodeId,
        nodeTitle = "",
        nodeClassType = "",
        paramName = paramName,
        paramType = type,
        currentValue = value,
    )

    @Test
    fun injects_seed_as_long_and_number_as_long_when_integral() {
        val workflow = """
            {"3":{"class_type":"KSampler","inputs":{"seed":1,"steps":10}}}
        """.trimIndent()

        val result = useCase(
            workflow,
            listOf(
                param("3", "seed", ParameterType.SEED, "999"),
                param("3", "steps", ParameterType.NUMBER, "30"),
            ),
        )

        val inputs = Json.parseToJsonElement(result).jsonObject["3"]!!
            .jsonObject["inputs"]!!.jsonObject
        // SEED and integral NUMBER both serialize as JSON integers (no decimal point).
        assertEquals("999", inputs["seed"]!!.jsonPrimitive.content)
        assertEquals("30", inputs["steps"]!!.jsonPrimitive.content)
    }

    @Test
    fun injects_non_integral_number_as_double() {
        val workflow = """{"4":{"inputs":{"cfg":7}}}"""

        val result = useCase(workflow, listOf(param("4", "cfg", ParameterType.NUMBER, "7.5")))

        val cfg = Json.parseToJsonElement(result).jsonObject["4"]!!
            .jsonObject["inputs"]!!.jsonObject["cfg"]!!.jsonPrimitive.content
        assertEquals("7.5", cfg)
    }

    @Test
    fun injects_boolean_from_true_string_and_numeric_one() {
        val workflow = """{"5":{"inputs":{"a":false,"b":false}}}"""

        val result = useCase(
            workflow,
            listOf(
                param("5", "a", ParameterType.BOOLEAN, "true"),
                param("5", "b", ParameterType.BOOLEAN, "1"),
            ),
        )

        val inputs = Json.parseToJsonElement(result).jsonObject["5"]!!
            .jsonObject["inputs"]!!.jsonObject
        assertTrue((inputs["a"] as JsonPrimitive).boolean)
        assertTrue((inputs["b"] as JsonPrimitive).boolean)
    }

    @Test
    fun boolean_is_false_for_any_other_string() {
        val workflow = """{"5":{"inputs":{"a":true}}}"""

        val result = useCase(workflow, listOf(param("5", "a", ParameterType.BOOLEAN, "no")))

        val a = Json.parseToJsonElement(result).jsonObject["5"]!!
            .jsonObject["inputs"]!!.jsonObject["a"] as JsonPrimitive
        assertFalse(a.boolean)
    }

    @Test
    fun leaves_unmatched_inputs_and_nodes_untouched() {
        val workflow = """
            {"3":{"inputs":{"seed":1,"keep":"x"}},"9":{"inputs":{"foo":"bar"}}}
        """.trimIndent()

        val result = useCase(workflow, listOf(param("3", "seed", ParameterType.SEED, "5")))

        val parsed = Json.parseToJsonElement(result).jsonObject
        val node3Inputs = parsed["3"]!!.jsonObject["inputs"]!!.jsonObject
        // Edited input changed, sibling input preserved verbatim.
        assertEquals("5", node3Inputs["seed"]!!.jsonPrimitive.content)
        assertEquals("x", node3Inputs["keep"]!!.jsonPrimitive.content)
        // Unrelated node untouched.
        val node9 = parsed["9"]!!.jsonObject["inputs"]!!.jsonObject
        assertEquals("bar", node9["foo"]!!.jsonPrimitive.content)
    }

    @Test
    fun returns_input_unchanged_when_workflow_json_is_malformed() {
        val malformed = "not json {"

        val result = useCase(malformed, listOf(param("3", "seed", ParameterType.SEED, "5")))

        assertEquals(malformed, result)
    }

    @Test
    fun injects_text_value_as_string() {
        val workflow = """{"2":{"inputs":{"text":"old"}}}"""

        val result = useCase(workflow, listOf(param("2", "text", ParameterType.TEXT, "new prompt")))

        val text = Json.parseToJsonElement(result).jsonObject["2"]!!
            .jsonObject["inputs"]!!.jsonObject["text"]!!.jsonPrimitive.content
        assertEquals("new prompt", text)
    }

    @Test
    fun preserves_node_without_inputs_object() {
        // A node whose "inputs" is missing should pass through even if a param targets it.
        val workflow = """{"7":{"class_type":"Note"}}"""

        val result = useCase(workflow, listOf(param("7", "anything", ParameterType.TEXT, "v")))

        val node = Json.parseToJsonElement(result).jsonObject["7"]!!.jsonObject
        assertEquals("Note", node["class_type"]!!.jsonPrimitive.content)
        // No inputs were fabricated.
        assertEquals(null, node["inputs"])
    }
}
