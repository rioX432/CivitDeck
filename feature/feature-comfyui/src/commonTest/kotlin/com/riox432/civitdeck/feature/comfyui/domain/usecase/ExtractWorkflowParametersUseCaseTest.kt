package com.riox432.civitdeck.feature.comfyui.domain.usecase

import com.riox432.civitdeck.feature.comfyui.domain.model.ParameterType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExtractWorkflowParametersUseCaseTest {

    private val useCase = ExtractWorkflowParametersUseCase(ParseAppModeMetadataUseCase())

    // region APP mode extraction

    @Test
    fun extractsOnlyAppModeDesignatedInputs() {
        val workflow = buildAppModeWorkflow()

        val params = useCase(workflow)

        // APP mode designates only seed and text — should NOT extract steps, cfg, etc.
        assertEquals(2, params.size)
        assertEquals("seed", params[0].paramName)
        assertEquals("text", params[1].paramName)
    }

    @Test
    fun appModePreservesOriginalOrder() {
        val workflow = buildAppModeWorkflow()

        val params = useCase(workflow)

        assertEquals(0, params[0].order)
        assertEquals(1, params[1].order)
    }

    @Test
    fun appModeResolvesNodeTitleFromMeta() {
        val workflow = buildAppModeWorkflow()

        val params = useCase(workflow)

        assertEquals("KSampler", params[0].nodeTitle)
        assertEquals("Positive Prompt", params[1].nodeTitle)
    }

    @Test
    fun appModeEnrichesWithObjectInfo() {
        val workflow = buildAppModeWorkflow()
        val objectInfo = """
        {
            "KSampler": {
                "input": {
                    "required": {
                        "seed": ["INT", {"min": 0, "max": 999999}]
                    }
                }
            }
        }
        """.trimIndent()

        val params = useCase(workflow, objectInfo)

        val seedParam = params.first { it.paramName == "seed" }
        assertEquals(0.0, seedParam.min)
        assertEquals(999999.0, seedParam.max)
        assertEquals(ParameterType.SEED, seedParam.paramType)
    }

    @Test
    fun appModeSkipsLinkedInputs() {
        // "clip" input is a link reference ["4", 0] — should be skipped even if designated
        val workflow = """
        {
            "6": {
                "class_type": "CLIPTextEncode",
                "inputs": {"text": "a photo", "clip": ["4", 0]},
                "_meta": {"title": "Prompt"}
            },
            "extra": {
                "linearData": {
                    "inputs": [["6", "text"], ["6", "clip"]],
                    "outputs": []
                },
                "linearMode": true
            }
        }
        """.trimIndent()

        val params = useCase(workflow)

        assertEquals(1, params.size)
        assertEquals("text", params[0].paramName)
    }

    @Test
    fun appModeHandlesUnknownNodeTypes() {
        // A custom node type not in PRIORITY_NODES should still be extracted via APP mode
        val workflow = """
        {
            "10": {
                "class_type": "CustomStyleTransfer",
                "inputs": {"style_strength": 0.8, "style_name": "anime"},
                "_meta": {"title": "Style Transfer"}
            },
            "extra": {
                "linearData": {
                    "inputs": [["10", "style_strength"], ["10", "style_name"]],
                    "outputs": []
                },
                "linearMode": true
            }
        }
        """.trimIndent()

        val params = useCase(workflow)

        assertEquals(2, params.size)
        assertEquals("CustomStyleTransfer", params[0].nodeClassType)
        assertEquals("style_strength", params[0].paramName)
        assertEquals("style_name", params[1].paramName)
    }

    // endregion

    // region Legacy fallback

    @Test
    fun fallsBackToLegacyWhenNoAppMode() {
        val workflow = """
        {
            "3": {
                "class_type": "KSampler",
                "inputs": {
                    "seed": 42,
                    "steps": 20,
                    "cfg": 7.0,
                    "sampler_name": "euler",
                    "scheduler": "normal",
                    "denoise": 1.0,
                    "model": ["1", 0],
                    "positive": ["6", 0],
                    "negative": ["7", 0],
                    "latent_image": ["5", 0]
                },
                "_meta": {"title": "KSampler"}
            }
        }
        """.trimIndent()

        val params = useCase(workflow)

        // Legacy mode extracts all PRIORITY_NODES params from KSampler
        assertTrue(params.any { it.paramName == "seed" })
        assertTrue(params.any { it.paramName == "steps" })
        assertTrue(params.any { it.paramName == "cfg" })
    }

    @Test
    fun legacyIgnoresNonPriorityNodes() {
        val workflow = """
        {
            "10": {
                "class_type": "CustomNode",
                "inputs": {"my_param": "value"},
                "_meta": {"title": "Custom"}
            }
        }
        """.trimIndent()

        val params = useCase(workflow)

        assertTrue(params.isEmpty())
    }

    @Test
    fun legacyGroupAndOrderAreDefaults() {
        val workflow = """
        {
            "3": {
                "class_type": "KSampler",
                "inputs": {"seed": 42},
                "_meta": {"title": "KSampler"}
            }
        }
        """.trimIndent()

        val params = useCase(workflow)

        assertEquals(1, params.size)
        assertNull(params[0].group)
        assertEquals(0, params[0].order)
    }

    // endregion

    // region Mixed workflow

    @Test
    fun appModeTakesPriorityOverLegacy() {
        // Workflow has both KSampler (would be extracted by legacy)
        // and APP mode metadata designating only "text"
        val workflow = """
        {
            "3": {
                "class_type": "KSampler",
                "inputs": {"seed": 42, "steps": 20, "cfg": 7.0},
                "_meta": {"title": "KSampler"}
            },
            "6": {
                "class_type": "CLIPTextEncode",
                "inputs": {"text": "a cat"},
                "_meta": {"title": "Prompt"}
            },
            "extra": {
                "linearData": {
                    "inputs": [["6", "text"]],
                    "outputs": []
                },
                "linearMode": true
            }
        }
        """.trimIndent()

        val params = useCase(workflow)

        // Only APP mode designated input, not legacy KSampler params
        assertEquals(1, params.size)
        assertEquals("text", params[0].paramName)
    }

    // endregion

    // region Error handling

    @Test
    fun returnsEmptyForInvalidJson() {
        val params = useCase("not json")
        assertTrue(params.isEmpty())
    }

    @Test
    fun returnsEmptyForEmptyObject() {
        val params = useCase("{}")
        assertTrue(params.isEmpty())
    }

    // endregion

    private fun buildAppModeWorkflow(): String = """
        {
            "3": {
                "class_type": "KSampler",
                "inputs": {
                    "seed": 42,
                    "steps": 20,
                    "cfg": 7.0,
                    "sampler_name": "euler",
                    "scheduler": "normal",
                    "denoise": 1.0
                },
                "_meta": {"title": "KSampler"}
            },
            "6": {
                "class_type": "CLIPTextEncode",
                "inputs": {"text": "a photo of a cat"},
                "_meta": {"title": "Positive Prompt"}
            },
            "9": {
                "class_type": "SaveImage",
                "inputs": {"images": ["8", 0]},
                "_meta": {"title": "Save Image"}
            },
            "extra": {
                "linearData": {
                    "inputs": [["3", "seed"], ["6", "text"]],
                    "outputs": ["9"]
                },
                "linearMode": true
            }
        }
    """.trimIndent()
}
