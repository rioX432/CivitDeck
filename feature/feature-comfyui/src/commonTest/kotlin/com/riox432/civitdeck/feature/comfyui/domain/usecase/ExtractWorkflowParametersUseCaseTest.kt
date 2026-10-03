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

    // region Schema / parameter-type resolution edge cases

    @Test
    fun legacyResolvesSelectTypeFromSchemaOptions() {
        val workflow = """
        {
            "4": {
                "class_type": "CheckpointLoaderSimple",
                "inputs": {"ckpt_name": "sd_xl.safetensors"},
                "_meta": {"title": "Load Checkpoint"}
            }
        }
        """.trimIndent()
        // object_info exposes ckpt_name as a list of options -> SELECT.
        val objectInfo = """
        {
            "CheckpointLoaderSimple": {
                "input": {
                    "required": {
                        "ckpt_name": [["sd_xl.safetensors", "sd_15.safetensors"]]
                    }
                }
            }
        }
        """.trimIndent()

        val params = useCase(workflow, objectInfo)

        val ckpt = params.single { it.paramName == "ckpt_name" }
        assertEquals(ParameterType.SELECT, ckpt.paramType)
        assertEquals(listOf("sd_xl.safetensors", "sd_15.safetensors"), ckpt.options)
    }

    @Test
    fun appModeResolvesBooleanTypeFromSchema() {
        // BOOLEAN type comes from the schema type descriptor, not the param name.
        val workflow = """
        {
            "12": {
                "class_type": "KSamplerAdvanced",
                "inputs": {"my_flag": true},
                "_meta": {"title": "Sampler"}
            },
            "extra": {
                "linearData": {"inputs": [["12", "my_flag"]], "outputs": []},
                "linearMode": true
            }
        }
        """.trimIndent()
        val objectInfo = """
        {
            "KSamplerAdvanced": {
                "input": {
                    "required": {
                        "my_flag": ["BOOLEAN", {"default": true}]
                    }
                }
            }
        }
        """.trimIndent()

        val params = useCase(workflow, objectInfo)

        assertEquals(ParameterType.BOOLEAN, params.single().paramType)
    }

    @Test
    fun resolvesSchemaFromOptionalBlockWhenNotInRequired() {
        // The schema constraint for "denoise" lives under "optional", not "required".
        val workflow = """
        {
            "3": {
                "class_type": "KSampler",
                "inputs": {"denoise": 0.5},
                "_meta": {"title": "KSampler"}
            }
        }
        """.trimIndent()
        val objectInfo = """
        {
            "KSampler": {
                "input": {
                    "required": {},
                    "optional": {
                        "denoise": ["FLOAT", {"min": 0.0, "max": 1.0, "step": 0.01}]
                    }
                }
            }
        }
        """.trimIndent()

        val params = useCase(workflow, objectInfo)

        val denoise = params.single { it.paramName == "denoise" }
        assertEquals(0.0, denoise.min)
        assertEquals(1.0, denoise.max)
        assertEquals(0.01, denoise.step)
    }

    @Test
    fun appModeDetectsImageParamByName() {
        // "image" is a known image param even without schema.
        val workflow = """
        {
            "20": {
                "class_type": "LoadImage",
                "inputs": {"image": "photo.png"},
                "_meta": {"title": "Load Image"}
            },
            "extra": {
                "linearData": {"inputs": [["20", "image"]], "outputs": []},
                "linearMode": true
            }
        }
        """.trimIndent()

        val params = useCase(workflow)

        assertEquals(ParameterType.IMAGE, params.single().paramType)
    }

    @Test
    fun legacySortsByNodePriorityThenNodeId() {
        // CLIPTextEncode (priority 1) must sort before CheckpointLoaderSimple (priority 2),
        // even though the checkpoint node id is numerically smaller.
        val workflow = """
        {
            "1": {
                "class_type": "CheckpointLoaderSimple",
                "inputs": {"ckpt_name": "model.safetensors"},
                "_meta": {"title": "Checkpoint"}
            },
            "6": {
                "class_type": "CLIPTextEncode",
                "inputs": {"text": "a cat"},
                "_meta": {"title": "Prompt"}
            }
        }
        """.trimIndent()

        val params = useCase(workflow)

        assertEquals("CLIPTextEncode", params.first().nodeClassType)
        assertEquals("CheckpointLoaderSimple", params.last().nodeClassType)
    }

    @Test
    fun appModeSkipsInputWhenNodeMissing() {
        // A designated input pointing at a non-existent node yields no parameter.
        val workflow = """
        {
            "6": {
                "class_type": "CLIPTextEncode",
                "inputs": {"text": "a cat"},
                "_meta": {"title": "Prompt"}
            },
            "extra": {
                "linearData": {"inputs": [["6", "text"], ["999", "seed"]], "outputs": []},
                "linearMode": true
            }
        }
        """.trimIndent()

        val params = useCase(workflow)

        assertEquals(1, params.size)
        assertEquals("text", params.single().paramName)
    }

    // endregion

    // region DiT-era nodes

    @Test
    fun legacyExtractsKrea2SplitLoaderParameters() {
        val params = useCase(krea2ApiWorkflow(), krea2ObjectInfoV1())

        fun param(name: String) = params.single { it.paramName == name }
        assertEquals("krea2_turbo_fp8_scaled.safetensors", param("unet_name").currentValue)
        assertEquals("qwen3vl_4b_fp8_scaled.safetensors", param("clip_name").currentValue)
        assertEquals("krea2", param("type").currentValue)
        assertEquals("qwen_image_vae.safetensors", param("vae_name").currentValue)
        assertEquals("krea2_darkbrush.safetensors", param("lora_name").currentValue)
        for (name in listOf("unet_name", "clip_name", "type", "vae_name", "lora_name")) {
            assertEquals(ParameterType.SELECT, param(name).paramType, name)
        }
        assertEquals(listOf("stable_diffusion", "krea2"), param("type").options)
        assertEquals(ParameterType.NUMBER, param("strength_model").paramType)
        assertEquals("1024", params.single { it.paramName == "width" }.currentValue)
        assertEquals("1024", params.single { it.paramName == "height" }.currentValue)
    }

    @Test
    fun legacyExtractsCustomSamplerSd3LatentAndDualClipParameters() {
        val workflow = """
        {
            "1": {"class_type": "DualCLIPLoader",
                  "inputs": {"clip_name1": "clip_l.safetensors", "clip_name2": "t5xxl.safetensors", "type": "flux"}},
            "2": {"class_type": "EmptySD3LatentImage", "inputs": {"width": 1024, "height": 768, "batch_size": 1}},
            "3": {"class_type": "RandomNoise", "inputs": {"noise_seed": 42}},
            "4": {"class_type": "BasicScheduler",
                  "inputs": {"scheduler": "simple", "steps": 8, "denoise": 1.0, "model": ["9", 0]}},
            "5": {"class_type": "KSamplerSelect", "inputs": {"sampler_name": "euler"}},
            "6": {"class_type": "CFGGuider",
                  "inputs": {"cfg": 1.0, "model": ["9", 0], "positive": ["7", 0], "negative": ["8", 0]}}
        }
        """.trimIndent()

        val params = useCase(workflow)

        val extracted = params.map { it.nodeClassType to it.paramName }.toSet()
        val expected = setOf(
            "DualCLIPLoader" to "clip_name1",
            "DualCLIPLoader" to "clip_name2",
            "DualCLIPLoader" to "type",
            "EmptySD3LatentImage" to "width",
            "EmptySD3LatentImage" to "height",
            "EmptySD3LatentImage" to "batch_size",
            "RandomNoise" to "noise_seed",
            "BasicScheduler" to "steps",
            "BasicScheduler" to "scheduler",
            "KSamplerSelect" to "sampler_name",
            "CFGGuider" to "cfg",
        )
        assertEquals(expected, extracted)
        assertEquals(ParameterType.SEED, params.single { it.paramName == "noise_seed" }.paramType)
        assertEquals(ParameterType.NUMBER, params.single { it.paramName == "cfg" }.paramType)
    }

    @Test
    fun legacySortsDiTSamplerNodesBeforeLoadersAndLatent() {
        val workflow = """
        {
            "1": {"class_type": "UNETLoader", "inputs": {"unet_name": "model.safetensors"}},
            "2": {"class_type": "EmptySD3LatentImage", "inputs": {"width": 1024}},
            "3": {"class_type": "LoraLoaderModelOnly", "inputs": {"lora_name": "style.safetensors"}},
            "4": {"class_type": "KSamplerSelect", "inputs": {"sampler_name": "euler"}}
        }
        """.trimIndent()

        val params = useCase(workflow)

        assertEquals(
            listOf("KSamplerSelect", "UNETLoader", "LoraLoaderModelOnly", "EmptySD3LatentImage"),
            params.map { it.nodeClassType },
        )
    }

    @Test
    fun selectOptionsComeFromV3ComboShape() {
        val workflow = """
        {
            "1": {"class_type": "UNETLoader", "inputs": {"unet_name": "a.safetensors"}},
            "2": {"class_type": "KSamplerSelect", "inputs": {"sampler_name": "euler"}}
        }
        """.trimIndent()
        // Synthetic V3 shape: ["COMBO", {"options": [...]}] (comfy_api/latest/_io.py add_to_dict_v1).
        val objectInfo = """
        {
            "UNETLoader": {"input": {"required": {
                "unet_name": ["COMBO", {"options": ["a.safetensors", "b.safetensors"], "tooltip": "t"}]
            }}},
            "KSamplerSelect": {"input": {"required": {
                "sampler_name": ["COMBO", {"multiselect": false, "options": ["euler", "res_multistep"]}]
            }}}
        }
        """.trimIndent()

        val params = useCase(workflow, objectInfo)

        val unet = params.single { it.paramName == "unet_name" }
        assertEquals(ParameterType.SELECT, unet.paramType)
        assertEquals(listOf("a.safetensors", "b.safetensors"), unet.options)
        val sampler = params.single { it.paramName == "sampler_name" }
        assertEquals(ParameterType.SELECT, sampler.paramType)
        assertEquals(listOf("euler", "res_multistep"), sampler.options)
    }

    @Test
    fun v3ComboWithoutOptionsYieldsNoOptions() {
        val workflow = """{"1": {"class_type": "VAELoader", "inputs": {"vae_name": "ae.safetensors"}}}"""
        val objectInfo = """{"VAELoader": {"input": {"required": {"vae_name": ["COMBO", {}]}}}}"""

        val params = useCase(workflow, objectInfo)

        val vae = params.single()
        assertTrue(vae.options.isEmpty())
        assertEquals(ParameterType.TEXT, vae.paramType)
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

    // Node ids and widget values follow the subgraph of Comfy-Org/workflow_templates
    // image_krea2_turbo_t2i.json (25ff90a5), rewritten as API-format inputs.
    private fun krea2ApiWorkflow(): String = """
        {
            "10": {"class_type": "UNETLoader",
                   "inputs": {"unet_name": "krea2_turbo_fp8_scaled.safetensors", "weight_dtype": "default"}},
            "11": {"class_type": "CLIPLoader",
                   "inputs": {"clip_name": "qwen3vl_4b_fp8_scaled.safetensors", "type": "krea2", "device": "default"}},
            "12": {"class_type": "VAELoader", "inputs": {"vae_name": "qwen_image_vae.safetensors"}},
            "15": {"class_type": "LoraLoaderModelOnly",
                   "inputs": {"lora_name": "krea2_darkbrush.safetensors", "strength_model": 0.8, "model": ["10", 0]}},
            "6": {"class_type": "CLIPTextEncode", "inputs": {"text": "a martini glass", "clip": ["11", 0]}},
            "13": {"class_type": "ConditioningZeroOut", "inputs": {"conditioning": ["6", 0]}},
            "5": {"class_type": "EmptyLatentImage", "inputs": {"width": 1024, "height": 1024, "batch_size": 1}},
            "3": {"class_type": "KSampler",
                  "inputs": {"seed": 735915477938686, "steps": 8, "cfg": 1, "sampler_name": "euler",
                             "scheduler": "simple", "denoise": 1, "model": ["15", 0], "positive": ["6", 0],
                             "negative": ["13", 0], "latent_image": ["5", 0]}},
            "8": {"class_type": "VAEDecode", "inputs": {"samples": ["3", 0], "vae": ["12", 0]}},
            "29": {"class_type": "SaveImage", "inputs": {"filename_prefix": "Krea2_turbo", "images": ["8", 0]}}
        }
    """.trimIndent()

    // V1 loader shape [[names...], {...}] as returned by nodes.py INPUT_TYPES.
    private fun krea2ObjectInfoV1(): String = """
        {
            "UNETLoader": {"input": {"required": {
                "unet_name": [["krea2_turbo_fp8_scaled.safetensors"]],
                "weight_dtype": [["default", "fp8_e4m3fn"], {"advanced": true}]
            }}},
            "CLIPLoader": {"input": {"required": {
                "clip_name": [["qwen3vl_4b_fp8_scaled.safetensors"]],
                "type": [["stable_diffusion", "krea2"]]
            }}},
            "VAELoader": {"input": {"required": {"vae_name": [["qwen_image_vae.safetensors", "pixel_space"]]}}},
            "LoraLoaderModelOnly": {"input": {"required": {
                "lora_name": [["krea2_darkbrush.safetensors"]],
                "strength_model": ["FLOAT", {"default": 1.0, "min": -100.0, "max": 100.0, "step": 0.01}]
            }}}
        }
    """.trimIndent()

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
