package com.riox432.civitdeck.feature.comfyui.domain.usecase

import com.riox432.civitdeck.domain.model.AppModeView
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ParseAppModeMetadataUseCaseTest {

    private val useCase = ParseAppModeMetadataUseCase()

    @Test
    fun parsesAppModeInputsFromLinearData() {
        val json = buildAppModeWorkflow(
            inputs = """[["3", "seed"], ["6", "text"]]""",
            outputs = """["9"]""",
        )

        val result = useCase(json)

        assertNotNull(result)
        assertEquals(2, result.inputs.size)
        assertEquals("3", result.inputs[0].nodeId)
        assertEquals("seed", result.inputs[0].paramName)
        assertEquals(0, result.inputs[0].order)
        assertEquals("6", result.inputs[1].nodeId)
        assertEquals("text", result.inputs[1].paramName)
        assertEquals(1, result.inputs[1].order)
    }

    @Test
    fun parsesOutputNodeIds() {
        val json = buildAppModeWorkflow(
            inputs = """[["3", "seed"]]""",
            outputs = """["9", "12"]""",
        )

        val result = useCase(json)

        assertNotNull(result)
        assertEquals(2, result.outputs.size)
        assertEquals("9", result.outputs[0].nodeId)
        assertEquals("12", result.outputs[1].nodeId)
    }

    @Test
    fun parsesWidgetHeightFromConfig() {
        val json = buildAppModeWorkflow(
            inputs = """[["6", "text", {"height": 200}]]""",
            outputs = """["9"]""",
        )

        val result = useCase(json)

        assertNotNull(result)
        assertEquals(200, result.inputs[0].widgetHeight)
    }

    @Test
    fun returnsNullForWorkflowWithoutExtra() {
        val json = """
        {
            "3": {
                "class_type": "KSampler",
                "inputs": {"seed": 12345}
            }
        }
        """.trimIndent()

        assertNull(useCase(json))
    }

    @Test
    fun returnsNullForWorkflowWithoutLinearData() {
        val json = """
        {
            "3": {
                "class_type": "KSampler",
                "inputs": {"seed": 12345}
            },
            "extra": {"ds": {"scale": 1.0}}
        }
        """.trimIndent()

        assertNull(useCase(json))
    }

    @Test
    fun returnsNullWhenBothInputsAndOutputsEmpty() {
        val json = buildAppModeWorkflow(
            inputs = """[]""",
            outputs = """[]""",
        )

        assertNull(useCase(json))
    }

    @Test
    fun parsesDefaultViewApp() {
        val json = buildAppModeWorkflow(
            inputs = """[["3", "seed"]]""",
            outputs = """["9"]""",
            linearMode = true,
        )

        val result = useCase(json)

        assertNotNull(result)
        assertEquals(AppModeView.APP, result.defaultView)
    }

    @Test
    fun parsesDefaultViewGraph() {
        val json = buildAppModeWorkflow(
            inputs = """[["3", "seed"]]""",
            outputs = """["9"]""",
            linearMode = false,
        )

        val result = useCase(json)

        assertNotNull(result)
        assertEquals(AppModeView.GRAPH, result.defaultView)
    }

    @Test
    fun defaultViewIsGraphWhenLinearModeAbsent() {
        val json = """
        {
            "3": {
                "class_type": "KSampler",
                "inputs": {"seed": 12345},
                "_meta": {"title": "Sampler"}
            },
            "extra": {
                "linearData": {
                    "inputs": [["3", "seed"]],
                    "outputs": []
                }
            }
        }
        """.trimIndent()

        val result = useCase(json)

        assertNotNull(result)
        assertEquals(AppModeView.GRAPH, result.defaultView)
    }

    @Test
    fun resolvesNodeTitleFromMeta() {
        val json = """
        {
            "3": {
                "class_type": "KSampler",
                "inputs": {"seed": 12345},
                "_meta": {"title": "My Sampler"}
            },
            "extra": {
                "linearData": {
                    "inputs": [["3", "seed"]],
                    "outputs": []
                },
                "linearMode": true
            }
        }
        """.trimIndent()

        val result = useCase(json)

        assertNotNull(result)
        assertEquals("My Sampler", result.inputs[0].label)
    }

    @Test
    fun returnsNullForInvalidJson() {
        assertNull(useCase("not valid json"))
    }

    @Test
    fun handlesPartialInputTuples() {
        // Input with only 1 element should be skipped
        val json = buildAppModeWorkflow(
            inputs = """[["3"], ["6", "text"]]""",
            outputs = """["9"]""",
        )

        val result = useCase(json)

        assertNotNull(result)
        assertEquals(1, result.inputs.size)
        assertEquals("6", result.inputs[0].nodeId)
    }

    @Test
    fun handlesNodeInNodesArray() {
        // UI workflow format stores nodes in a "nodes" array instead of top-level keys
        val json = """
        {
            "nodes": [
                {
                    "id": "3",
                    "type": "KSampler",
                    "title": "My Sampler Node"
                }
            ],
            "extra": {
                "linearData": {
                    "inputs": [["3", "seed"]],
                    "outputs": ["3"]
                },
                "linearMode": true
            }
        }
        """.trimIndent()

        val result = useCase(json)

        assertNotNull(result)
        assertEquals("My Sampler Node", result.inputs[0].label)
        assertEquals("My Sampler Node", result.outputs[0].label)
    }

    private fun buildAppModeWorkflow(
        inputs: String,
        outputs: String,
        linearMode: Boolean = true,
    ): String {
        return """
        {
            "3": {
                "class_type": "KSampler",
                "inputs": {"seed": 12345, "steps": 20, "cfg": 7.0},
                "_meta": {"title": "KSampler"}
            },
            "6": {
                "class_type": "CLIPTextEncode",
                "inputs": {"text": "a photo", "clip": ["4", 0]},
                "_meta": {"title": "Positive Prompt"}
            },
            "9": {
                "class_type": "SaveImage",
                "inputs": {"images": ["8", 0]},
                "_meta": {"title": "Save Image"}
            },
            "12": {
                "class_type": "PreviewImage",
                "inputs": {"images": ["8", 0]},
                "_meta": {"title": "Preview"}
            },
            "extra": {
                "linearData": {
                    "inputs": $inputs,
                    "outputs": $outputs
                },
                "linearMode": $linearMode
            }
        }
        """.trimIndent()
    }
}
