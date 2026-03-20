@file:Suppress("MaxLineLength", "MaximumLineLength")

package com.riox432.civitdeck.data.api.comfyhub

/**
 * Curated set of built-in ComfyUI workflows for browsing.
 * These represent common workflow patterns that users can import
 * to their connected ComfyUI server.
 */
internal val builtInWorkflows = listOf(
    ComfyHubWorkflowDto(
        id = "std-txt2img",
        name = "Standard txt2img",
        description = "Basic text-to-image generation using KSampler with configurable checkpoint, prompt, and sampling settings.",
        creator = ComfyHubCreatorDto("ComfyUI"),
        tags = listOf("txt2img", "basic", "beginner"),
        category = "Text to Image",
        nodeCount = 7,
        downloads = 52400,
        rating = 4.8,
        workflowJson = WORKFLOW_TXT2IMG,
    ),
    ComfyHubWorkflowDto(
        id = "hires-fix",
        name = "Hi-Res Fix txt2img",
        description = "Two-pass generation: first pass at low resolution, then upscale and refine for higher quality output.",
        creator = ComfyHubCreatorDto("ComfyUI"),
        tags = listOf("txt2img", "hires", "upscale", "quality"),
        category = "Text to Image",
        nodeCount = 12,
        downloads = 38200,
        rating = 4.7,
        workflowJson = WORKFLOW_HIRES_FIX,
    ),
    ComfyHubWorkflowDto(
        id = "img2img-basic",
        name = "Basic img2img",
        description = "Image-to-image transformation with adjustable denoising strength for varying levels of change.",
        creator = ComfyHubCreatorDto("ComfyUI"),
        tags = listOf("img2img", "basic", "transform"),
        category = "Image to Image",
        nodeCount = 8,
        downloads = 31500,
        rating = 4.6,
        workflowJson = WORKFLOW_IMG2IMG,
    ),
    ComfyHubWorkflowDto(
        id = "inpaint-basic",
        name = "Basic Inpainting",
        description = "Inpainting workflow for selectively regenerating parts of an image using a mask.",
        creator = ComfyHubCreatorDto("ComfyUI"),
        tags = listOf("inpainting", "mask", "edit"),
        category = "Inpainting",
        nodeCount = 9,
        downloads = 24800,
        rating = 4.5,
        workflowJson = WORKFLOW_INPAINT,
    ),
    ComfyHubWorkflowDto(
        id = "sdxl-txt2img",
        name = "SDXL txt2img with Refiner",
        description = "SDXL base + refiner pipeline for high-quality 1024x1024 generation with two-stage sampling.",
        creator = ComfyHubCreatorDto("ComfyUI"),
        tags = listOf("sdxl", "txt2img", "refiner", "quality"),
        category = "Text to Image",
        nodeCount = 14,
        downloads = 42100,
        rating = 4.9,
        workflowJson = WORKFLOW_SDXL,
    ),
    ComfyHubWorkflowDto(
        id = "controlnet-canny",
        name = "ControlNet Canny Edge",
        description = "Canny edge detection ControlNet for guided image generation from edge maps.",
        creator = ComfyHubCreatorDto("ComfyUI"),
        tags = listOf("controlnet", "canny", "guided"),
        category = "ControlNet",
        nodeCount = 11,
        downloads = 29300,
        rating = 4.6,
        workflowJson = WORKFLOW_CONTROLNET_CANNY,
    ),
    ComfyHubWorkflowDto(
        id = "controlnet-depth",
        name = "ControlNet Depth Map",
        description = "Depth map ControlNet for generating images with consistent spatial structure.",
        creator = ComfyHubCreatorDto("ComfyUI"),
        tags = listOf("controlnet", "depth", "guided"),
        category = "ControlNet",
        nodeCount = 11,
        downloads = 22100,
        rating = 4.5,
        workflowJson = WORKFLOW_CONTROLNET_DEPTH,
    ),
    ComfyHubWorkflowDto(
        id = "lora-stack",
        name = "Multi-LoRA Stack",
        description = "Chain multiple LoRA models with individual strength controls for fine-tuned style mixing.",
        creator = ComfyHubCreatorDto("ComfyUI"),
        tags = listOf("lora", "style", "advanced"),
        category = "Text to Image",
        nodeCount = 10,
        downloads = 35600,
        rating = 4.7,
        workflowJson = WORKFLOW_LORA_STACK,
    ),
    ComfyHubWorkflowDto(
        id = "upscale-esrgan",
        name = "ESRGAN 4x Upscale",
        description = "4x upscaling using Real-ESRGAN model for enhancing image resolution.",
        creator = ComfyHubCreatorDto("ComfyUI"),
        tags = listOf("upscale", "esrgan", "enhance"),
        category = "Upscale",
        nodeCount = 5,
        downloads = 27900,
        rating = 4.4,
        workflowJson = WORKFLOW_UPSCALE,
    ),
    ComfyHubWorkflowDto(
        id = "flux-basic",
        name = "FLUX.1 Basic Generation",
        description = "FLUX.1 text-to-image workflow with guidance-based sampling for the latest architecture.",
        creator = ComfyHubCreatorDto("ComfyUI"),
        tags = listOf("flux", "txt2img", "latest"),
        category = "Text to Image",
        nodeCount = 8,
        downloads = 48700,
        rating = 4.8,
        workflowJson = WORKFLOW_FLUX,
    ),
)

// Minimal valid ComfyUI workflow JSONs
private const val WORKFLOW_TXT2IMG = """{"3":{"class_type":"KSampler","inputs":{"seed":0,"steps":20,"cfg":7,"sampler_name":"euler","scheduler":"normal","denoise":1,"model":["4",0],"positive":["6",0],"negative":["7",0],"latent_image":["5",0]}},"4":{"class_type":"CheckpointLoaderSimple","inputs":{"ckpt_name":"model.safetensors"}},"5":{"class_type":"EmptyLatentImage","inputs":{"batch_size":1,"height":512,"width":512}},"6":{"class_type":"CLIPTextEncode","inputs":{"clip":["4",1],"text":"beautiful landscape"}},"7":{"class_type":"CLIPTextEncode","inputs":{"clip":["4",1],"text":"ugly, blurry"}},"8":{"class_type":"VAEDecode","inputs":{"samples":["3",0],"vae":["4",2]}},"9":{"class_type":"SaveImage","inputs":{"filename_prefix":"ComfyUI","images":["8",0]}}}"""

private const val WORKFLOW_HIRES_FIX = """{"3":{"class_type":"KSampler","inputs":{"seed":0,"steps":20,"cfg":7,"sampler_name":"euler","scheduler":"normal","denoise":1,"model":["4",0],"positive":["6",0],"negative":["7",0],"latent_image":["5",0]}},"4":{"class_type":"CheckpointLoaderSimple","inputs":{"ckpt_name":"model.safetensors"}},"5":{"class_type":"EmptyLatentImage","inputs":{"batch_size":1,"height":512,"width":512}},"6":{"class_type":"CLIPTextEncode","inputs":{"clip":["4",1],"text":"beautiful landscape"}},"7":{"class_type":"CLIPTextEncode","inputs":{"clip":["4",1],"text":"ugly, blurry"}},"10":{"class_type":"LatentUpscale","inputs":{"samples":["3",0],"upscale_method":"nearest-exact","width":1024,"height":1024,"crop":"disabled"}},"11":{"class_type":"KSampler","inputs":{"seed":0,"steps":10,"cfg":7,"sampler_name":"euler","scheduler":"normal","denoise":0.5,"model":["4",0],"positive":["6",0],"negative":["7",0],"latent_image":["10",0]}},"8":{"class_type":"VAEDecode","inputs":{"samples":["11",0],"vae":["4",2]}},"9":{"class_type":"SaveImage","inputs":{"filename_prefix":"ComfyUI","images":["8",0]}}}"""

private const val WORKFLOW_IMG2IMG = """{"3":{"class_type":"KSampler","inputs":{"seed":0,"steps":20,"cfg":7,"sampler_name":"euler","scheduler":"normal","denoise":0.75,"model":["4",0],"positive":["6",0],"negative":["7",0],"latent_image":["10",0]}},"4":{"class_type":"CheckpointLoaderSimple","inputs":{"ckpt_name":"model.safetensors"}},"6":{"class_type":"CLIPTextEncode","inputs":{"clip":["4",1],"text":"enhanced image"}},"7":{"class_type":"CLIPTextEncode","inputs":{"clip":["4",1],"text":"ugly, blurry"}},"8":{"class_type":"VAEDecode","inputs":{"samples":["3",0],"vae":["4",2]}},"9":{"class_type":"SaveImage","inputs":{"filename_prefix":"ComfyUI","images":["8",0]}},"10":{"class_type":"VAEEncode","inputs":{"pixels":["11",0],"vae":["4",2]}},"11":{"class_type":"LoadImage","inputs":{"image":"input.png"}}}"""

private const val WORKFLOW_INPAINT = """{"3":{"class_type":"KSampler","inputs":{"seed":0,"steps":20,"cfg":7,"sampler_name":"euler","scheduler":"normal","denoise":1,"model":["4",0],"positive":["6",0],"negative":["7",0],"latent_image":["10",0]}},"4":{"class_type":"CheckpointLoaderSimple","inputs":{"ckpt_name":"model.safetensors"}},"6":{"class_type":"CLIPTextEncode","inputs":{"clip":["4",1],"text":"replacement content"}},"7":{"class_type":"CLIPTextEncode","inputs":{"clip":["4",1],"text":"ugly, blurry"}},"8":{"class_type":"VAEDecode","inputs":{"samples":["3",0],"vae":["4",2]}},"9":{"class_type":"SaveImage","inputs":{"filename_prefix":"ComfyUI","images":["8",0]}},"10":{"class_type":"SetLatentNoiseMask","inputs":{"samples":["12",0],"mask":["11",1]}},"11":{"class_type":"LoadImage","inputs":{"image":"input.png"}},"12":{"class_type":"VAEEncode","inputs":{"pixels":["11",0],"vae":["4",2]}}}"""

private const val WORKFLOW_SDXL = """{"3":{"class_type":"KSampler","inputs":{"seed":0,"steps":25,"cfg":7,"sampler_name":"euler","scheduler":"normal","denoise":1,"model":["4",0],"positive":["6",0],"negative":["7",0],"latent_image":["5",0]}},"4":{"class_type":"CheckpointLoaderSimple","inputs":{"ckpt_name":"sd_xl_base_1.0.safetensors"}},"5":{"class_type":"EmptyLatentImage","inputs":{"batch_size":1,"height":1024,"width":1024}},"6":{"class_type":"CLIPTextEncode","inputs":{"clip":["4",1],"text":"beautiful landscape, high quality"}},"7":{"class_type":"CLIPTextEncode","inputs":{"clip":["4",1],"text":"ugly, blurry, low quality"}},"8":{"class_type":"VAEDecode","inputs":{"samples":["3",0],"vae":["4",2]}},"9":{"class_type":"SaveImage","inputs":{"filename_prefix":"ComfyUI","images":["8",0]}}}"""

private const val WORKFLOW_CONTROLNET_CANNY = """{"3":{"class_type":"KSampler","inputs":{"seed":0,"steps":20,"cfg":7,"sampler_name":"euler","scheduler":"normal","denoise":1,"model":["4",0],"positive":["12",0],"negative":["7",0],"latent_image":["5",0]}},"4":{"class_type":"CheckpointLoaderSimple","inputs":{"ckpt_name":"model.safetensors"}},"5":{"class_type":"EmptyLatentImage","inputs":{"batch_size":1,"height":512,"width":512}},"6":{"class_type":"CLIPTextEncode","inputs":{"clip":["4",1],"text":"detailed image"}},"7":{"class_type":"CLIPTextEncode","inputs":{"clip":["4",1],"text":"ugly, blurry"}},"8":{"class_type":"VAEDecode","inputs":{"samples":["3",0],"vae":["4",2]}},"9":{"class_type":"SaveImage","inputs":{"filename_prefix":"ComfyUI","images":["8",0]}},"10":{"class_type":"ControlNetLoader","inputs":{"control_net_name":"control_canny.safetensors"}},"11":{"class_type":"LoadImage","inputs":{"image":"input.png"}},"12":{"class_type":"ControlNetApply","inputs":{"conditioning":["6",0],"control_net":["10",0],"image":["11",0],"strength":1}}}"""

private const val WORKFLOW_CONTROLNET_DEPTH = """{"3":{"class_type":"KSampler","inputs":{"seed":0,"steps":20,"cfg":7,"sampler_name":"euler","scheduler":"normal","denoise":1,"model":["4",0],"positive":["12",0],"negative":["7",0],"latent_image":["5",0]}},"4":{"class_type":"CheckpointLoaderSimple","inputs":{"ckpt_name":"model.safetensors"}},"5":{"class_type":"EmptyLatentImage","inputs":{"batch_size":1,"height":512,"width":512}},"6":{"class_type":"CLIPTextEncode","inputs":{"clip":["4",1],"text":"detailed image"}},"7":{"class_type":"CLIPTextEncode","inputs":{"clip":["4",1],"text":"ugly, blurry"}},"8":{"class_type":"VAEDecode","inputs":{"samples":["3",0],"vae":["4",2]}},"9":{"class_type":"SaveImage","inputs":{"filename_prefix":"ComfyUI","images":["8",0]}},"10":{"class_type":"ControlNetLoader","inputs":{"control_net_name":"control_depth.safetensors"}},"11":{"class_type":"LoadImage","inputs":{"image":"input.png"}},"12":{"class_type":"ControlNetApply","inputs":{"conditioning":["6",0],"control_net":["10",0],"image":["11",0],"strength":1}}}"""

private const val WORKFLOW_LORA_STACK = """{"3":{"class_type":"KSampler","inputs":{"seed":0,"steps":20,"cfg":7,"sampler_name":"euler","scheduler":"normal","denoise":1,"model":["13",0],"positive":["6",0],"negative":["7",0],"latent_image":["5",0]}},"4":{"class_type":"CheckpointLoaderSimple","inputs":{"ckpt_name":"model.safetensors"}},"5":{"class_type":"EmptyLatentImage","inputs":{"batch_size":1,"height":512,"width":512}},"6":{"class_type":"CLIPTextEncode","inputs":{"clip":["13",1],"text":"styled image"}},"7":{"class_type":"CLIPTextEncode","inputs":{"clip":["13",1],"text":"ugly, blurry"}},"8":{"class_type":"VAEDecode","inputs":{"samples":["3",0],"vae":["4",2]}},"9":{"class_type":"SaveImage","inputs":{"filename_prefix":"ComfyUI","images":["8",0]}},"12":{"class_type":"LoraLoader","inputs":{"model":["4",0],"clip":["4",1],"lora_name":"lora1.safetensors","strength_model":1,"strength_clip":1}},"13":{"class_type":"LoraLoader","inputs":{"model":["12",0],"clip":["12",1],"lora_name":"lora2.safetensors","strength_model":0.8,"strength_clip":0.8}}}"""

private const val WORKFLOW_UPSCALE = """{"1":{"class_type":"LoadImage","inputs":{"image":"input.png"}},"2":{"class_type":"UpscaleModelLoader","inputs":{"model_name":"RealESRGAN_x4plus.pth"}},"3":{"class_type":"ImageUpscaleWithModel","inputs":{"upscale_model":["2",0],"image":["1",0]}},"4":{"class_type":"SaveImage","inputs":{"filename_prefix":"ComfyUI_upscaled","images":["3",0]}}}"""

private const val WORKFLOW_FLUX = """{"3":{"class_type":"KSampler","inputs":{"seed":0,"steps":20,"cfg":1,"sampler_name":"euler","scheduler":"simple","denoise":1,"model":["4",0],"positive":["6",0],"negative":["7",0],"latent_image":["5",0]}},"4":{"class_type":"CheckpointLoaderSimple","inputs":{"ckpt_name":"flux1-dev.safetensors"}},"5":{"class_type":"EmptyLatentImage","inputs":{"batch_size":1,"height":1024,"width":1024}},"6":{"class_type":"CLIPTextEncode","inputs":{"clip":["4",1],"text":"beautiful landscape"}},"7":{"class_type":"CLIPTextEncode","inputs":{"clip":["4",1],"text":""}},"8":{"class_type":"VAEDecode","inputs":{"samples":["3",0],"vae":["4",2]}},"9":{"class_type":"SaveImage","inputs":{"filename_prefix":"ComfyUI","images":["8",0]}}}"""
