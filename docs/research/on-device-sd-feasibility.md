# On-Device Stable Diffusion Feasibility (Issue #610)

Status: **Research Complete** — see [Recommendation](#recommendation)
Date: 2026-04-09

## Goal

Evaluate the feasibility of integrating on-device Stable Diffusion image generation into
CivitDeck. This covers Apple Core ML, Android inference runtimes, model size / memory
constraints, UX considerations, and a cross-platform KMP architecture sketch.

CivitDeck already has an on-device ML pipeline for SigLIP-2 image embeddings (see
[siglip2-feasibility.md](./siglip2-feasibility.md)), using the `expect/actual`
`ImageEmbeddingModel` pattern with Core ML on iOS and ONNX Runtime on Android. This
research asks whether the same approach can scale to the much heavier diffusion workload.

## 1. Apple ml-stable-diffusion & Core ML

### Framework overview

Apple's [ml-stable-diffusion](https://github.com/apple/ml-stable-diffusion) repository
provides a Python conversion pipeline (PyTorch → Core ML `.mlpackage`) and a Swift
inference library. It targets CPU, GPU, and the Apple Neural Engine (ANE) via Core ML
compute units.

Key facts:
- Requires **iOS 16.2+ / macOS 13.1+** (CivitDeck targets iOS 16.0 — compatible)
- Supports SD 1.4, SD 1.5, SD 2.0, SD 2.1, and **SDXL** via Apple's official conversion
- **SD 3 / FLUX** are *not* supported by Apple's repo — community projects like
  [DiffusionKit](https://github.com/argmaxinc/DiffusionKit) (Argmax) cover SD 3 / 3.5
  and FLUX via MLX on Mac, but these are Mac-only (not iPhone)
- 6-bit palettization (iOS 17+ / macOS 14+) reduces the UNet to **~1 GB** with ≤30%
  Neural Engine speedup
  ([HF blog](https://huggingface.co/blog/fast-diffusers-coreml),
  [apple/coreml-stable-diffusion-v1-5-palettized](https://huggingface.co/apple/coreml-stable-diffusion-v1-5-palettized))
- SDXL UNet at fp16 is **4.8 GB**; 6-bit palettized drops to ~1.2 GB; mixed-bit (avg
  4.5-bit) reaches ~1.46 GB
  ([coremltools guide](https://apple.github.io/coremltools/docs-guides/source/opt-stable-diffusion.html))

### Compute units: ANE vs GPU

| Compute unit | Pros | Cons |
|---|---|---|
| ANE (`.cpuAndNeuralEngine`) | Lowest power draw, ~150 MB peak RAM, best for iPhone | Some ops fall back to CPU; throughput ceiling on older ANE |
| GPU (`.cpuAndGPU`) | Higher peak throughput on M-series | Higher memory footprint (~2 GB+), thermal throttling on iPhone |
| CPU only | Universal fallback | 5–10x slower than ANE/GPU |

Apple recommends `.cpuAndNeuralEngine` on iPhone/iPad and `.cpuAndGPU` on Mac for best
results ([Apple ML Research](https://machinelearning.apple.com/research/stable-diffusion-coreml-apple-silicon)).

### iPhone RAM constraints

| Device | RAM | SD 1.5 feasible? | SDXL feasible? |
|---|---|---|---|
| iPhone 12 / 13 (4 GB) | 4 GB | Marginal — ANE mode only, risk of OOM | No |
| iPhone 13 Pro / 14 (6 GB) | 6 GB | Yes (ANE) | Marginal with heavy palettization |
| iPhone 15 Pro / 16 Pro (8 GB) | 8 GB | Yes | Possible with 6-bit UNet |
| iPhone 16 Pro Max (12 GB) | 12 GB | Yes | Yes |

Apple's own [issue #291](https://github.com/apple/ml-stable-diffusion/issues/291) confirms
that 4 GB devices struggle with the full pipeline. SD 1.5 on ANE needs ~2 GB peak; SDXL
needs ~3–4 GB even palettized.

## 2. Performance benchmarks

### Apple devices — SD 1.5 (512x512, 20 steps)

Sources: Apple ml-stable-diffusion README, Hugging Face benchmarks, Draw Things reports.

| Device | Engine | SD 1.5 (20 steps) | Notes |
|---|---|---|---|
| iPhone 12 (A14) | ANE | ~35–45 s | 4 GB RAM, borderline |
| iPhone 13 Pro (A15) | ANE | ~23–30 s | |
| iPhone 14 Pro (A16) | ANE | ~18–25 s | |
| iPhone 15 Pro (A17 Pro) | ANE | ~10–15 s | INT8 activations on A17 ANE |
| iPhone 16 Pro (A18 Pro) | ANE | ~7–12 s (est.) | Based on A18 ANE uplift |
| M1 MacBook | GPU | ~8 s | |
| M2 MacBook | GPU | ~5 s | |
| M3 MacBook Pro | GPU | ~3.5 s | |
| M4 MacBook Pro | GPU | ~2.5 s (est.) | |

### Apple devices — SDXL (1024x1024, 20 steps)

| Device | Engine | SDXL (20 steps) | Notes |
|---|---|---|---|
| iPhone 15 Pro (A17 Pro) | ANE | ~40–60 s | 6-bit palettized UNet |
| M1 MacBook | GPU | ~45 s | |
| M2 MacBook Pro | GPU | ~30 s | |
| M3 MacBook Pro | GPU | ~20 s | |

### Draw Things optimized performance

[Draw Things](https://drawthings.ai/) achieves significantly faster inference through
custom Metal kernels:

- **Metal FlashAttention** reduces attention memory from O(n^2) to O(n) and speeds up
  the attention block by 43–120%
  ([engineering blog](https://engineering.drawthings.ai/p/integrating-metal-flashattention-accelerating-the-heart-of-image-generation-in-the-apple-ecosystem-16a86142eb18))
- **Metal FlashAttention 2.0** added backward pass for fine-tuning on-device
  ([v2.0 blog](https://engineering.drawthings.ai/p/metal-flashattention-2-0-pushing-forward-on-device-inference-training-on-apple-silicon-fe8aac1ab23c))
- **Metal FlashAttention v2.5** with Neural Accelerators claims **4.6x** gains on M5/A19
  ([release notes](https://releases.drawthings.ai/p/metal-flashattention-v25-w-neural))
- On iPhone 15 Pro with LCM LoRA: SD 1.5 at 512x512 in **< 10 s**, SDXL at 1024x1024
  in **< 40 s**
  ([Draw Things tweet](https://x.com/drawthingsapp/status/1724992139271270480))
- Supports SD 1.5, SDXL, SD 3, and FLUX models
- Devices as old as iPhone 12 are supported

Draw Things is the gold standard for on-device diffusion on Apple platforms. Their
optimizations are open source (Metal FlashAttention), but integrating them requires
significant custom Metal shader work outside Core ML's standard pipeline.

### Qualcomm Android — SD 1.5 (512x512, 20 steps)

Sources: Qualcomm AI Hub, Local Dream benchmarks, Hugging Face model cards.

| Device | Engine | SD 1.5 (20 steps) | Notes |
|---|---|---|---|
| Snapdragon 8 Gen 1 | QNN NPU | ~15–25 s | First-gen NPU for diffusion |
| Snapdragon 8 Gen 2 | QNN NPU | ~8–15 s | |
| Snapdragon 8 Gen 3 | QNN NPU | ~5–10 s | Qualcomm claims < 1 s per step |
| Snapdragon 8 Elite | QNN NPU | ~3–7 s (est.) | |
| Tensor G3 (Pixel 8) | ONNX CPU | ~40–60 s | No NPU path for diffusion |
| Tensor G4 (Pixel 9) | ONNX CPU | ~30–50 s | Limited NPU support |

Per Qualcomm's [published model card](https://huggingface.co/qualcomm/Stable-Diffusion),
individual component times on Galaxy S24 (8 Gen 3): TextEncoder 8 ms, UNet 189 ms/step,
VAEDecoder 295 ms. At 20 steps: ~0.008 + 20×0.189 + 0.295 = **~4.1 s** for the full
pipeline — aligning with the < 1 s/step claim.

### MobileDiffusion (Google)

[MobileDiffusion](https://research.google/blog/mobilediffusion-rapid-text-to-image-generation-on-device/)
is a purpose-built 520M parameter model:
- **0.2 s** on iPhone 15 Pro for a 512x512 image (single-step DiffusionGAN distillation)
- Comparable to SD 1.5 quality despite much smaller model
- Uses separable convolutions and architectural optimizations
- Available via [MediaPipe Image Generation API](https://ai.google.dev/edge/mediapipe/solutions/vision/image_generator/android)
  for Android (~15 s on "higher-end devices" without NPU acceleration)
- MediaPipe supports SD 1.5 architecture models, ControlNet-like plugins (6M param
  lightweight), and LoRA weights

## 3. Model size and storage

### SD 1.5 Core ML package sizes

| Variant | Total size | Notes |
|---|---|---|
| fp16 (full pipeline) | ~2.5 GB | Text encoder + UNet + VAE + safety checker |
| 6-bit palettized | ~1.0–1.2 GB | Recommended for iPhone; iOS 17+ required |
| INT8 activations + 6-bit weights | ~0.9 GB | A17 Pro+ ANE optimization |

### SDXL Core ML package sizes

| Variant | Total size | Notes |
|---|---|---|
| fp16 | ~6.5 GB | Impractical for mobile |
| 6-bit palettized | ~2.5 GB | Tight for iPhone storage |
| Mixed-bit (avg 4.5-bit) | ~1.5 GB | Best balance, iOS 17+ |

### Android (ONNX / QNN)

| Format | SD 1.5 size | Notes |
|---|---|---|
| ONNX fp16 | ~2 GB | Full pipeline |
| QNN W8A16 | ~1.0–1.2 GB | Qualcomm NPU optimized |
| ONNX INT8 | ~1.0 GB | CPU execution |

### Storage strategy for a mobile app

These models are **far too large to bundle** with the app binary:
- App Store limit per binary: 4 GB (iOS), but users won't tolerate a 1+ GB initial download
- Play Store AAB limit: 200 MB (base APK) + 2 GB (Play Asset Delivery)

**On-Demand Download is mandatory.** Options:
- **iOS**: On-Demand Resources (ODR) for up to 20 GB of tagged assets, or custom
  download from a CDN
- **Android**: Play Asset Delivery (asset packs) or direct CDN download
- **Both**: Show download size, progress, and allow deletion from Settings

## 4. Android framework comparison

| Framework | Maturity | NPU support | SD models | Notes |
|---|---|---|---|---|
| **Qualcomm QNN SDK** | Production | Snapdragon NPU (Hexagon) | SD 1.5, SDXL | Best performance on Qualcomm; device-locked |
| **ONNX Runtime + QNN EP** | Production | Via QNN EP | SD 1.5 | Bridges ONNX models to QNN; broader device coverage |
| **MediaPipe Image Generator** | Experimental | Limited | SD 1.5 architecture | Google's official API; ~15 s on high-end; ControlNet plugins |
| **TFLite / LiteRT** | Experimental | Qualcomm (via delegate) | Limited | No first-party SD export; conversion brittle |
| **Samsung on-device** | Proprietary | Exynos NPU | Galaxy AI features | Not available as public SDK |

**Assessment:** Qualcomm QNN (via ONNX Runtime QNN EP or direct) is the only
production-ready path for fast Android inference. MediaPipe is the easiest to integrate
but performance is mediocre without NPU acceleration. Non-Qualcomm devices (Tensor, Dimensity,
Exynos) lack a viable NPU path for diffusion — they fall back to CPU/GPU, which is 3–10x
slower.

### Local Dream reference implementation

[Local Dream](https://github.com/xororz/local-dream) is the most complete open-source
Android SD app:
- Uses QNN SDK with W8A16 quantization
- Supports Snapdragon 8 Gen 1/2/3, 8 Elite
- SD 1.5 on 8 Gen 3: ~5–10 s per image
- Falls back to CPU/GPU on unsupported devices
- Good reference for QNN integration patterns

## 5. Cutting-edge research

### Diffusion Models on the Edge (arXiv:2504.15298)

This April 2025 [survey](https://arxiv.org/abs/2504.15298) comprehensively covers:
- **Model compression**: pruning, quantization (PTQ and QAT), knowledge distillation
- **Sampling efficiency**: few-step diffusion (LCM, consistency models), GAN-distilled
  single-step (DiffusionGAN, SDXL Turbo)
- **Hardware-software co-design**: ANE-aware graph optimization, NPU kernel fusion
- **Key finding**: combining architectural efficiency (MobileDiffusion-class models) with
  aggressive quantization and single/few-step sampling can achieve sub-second generation
  on 2024-era flagships

### 1.58-bit FLUX (arXiv:2412.18653)

[1.58-bit FLUX](https://arxiv.org/abs/2412.18653) demonstrates extreme quantization of
the FLUX.1-dev model to ternary weights ({-1, 0, +1}):
- **7.7x reduction** in model storage (FLUX.1-dev: ~24 GB → ~3 GB)
- **5.1x reduction** in inference memory
- Improved inference latency with custom ternary kernels
- Quality maintained on GenEval and T2I-Compbench benchmarks
- **Relevance to CivitDeck**: this opens a path to running large modern models (FLUX-class)
  on devices with 8+ GB RAM in the future, but requires custom kernel development and is
  not yet production-ready on mobile

### SnapFusion & LCM (Latent Consistency Models)

- **SnapFusion** (2023): architecture-aware distillation achieving SD 1.5 in < 2 s on
  iPhone 14 Pro at 512x512
- **LCM** (2023-2024): reduces required steps from 20–50 to **4–8 steps** while
  maintaining quality, via consistency distillation from a pre-trained diffusion model
- **LCM + LoRA**: lightweight fine-tuning compatible, widely adopted (Draw Things uses
  LCM LoRA for its fastest modes)

## 6. UX design considerations

### Model download flow

```
┌─────────────────────────────────┐
│  On-Device Generation           │
│  ─────────────────────          │
│  [i] Models are downloaded      │
│  on-demand and stored locally.  │
│                                 │
│  ┌───────────────────────┐      │
│  │ SD 1.5 (Palettized)   │      │
│  │ 1.0 GB · Best for     │      │
│  │ older devices          │      │
│  │          [Download]    │      │
│  └───────────────────────┘      │
│                                 │
│  ┌───────────────────────┐      │
│  │ SDXL (Compressed)     │      │
│  │ 1.5 GB · Higher       │      │
│  │ quality, needs 8 GB+  │      │
│  │          [Download]    │      │
│  └───────────────────────┘      │
│                                 │
│  Storage used: 1.0 GB           │
│  [Manage Storage]               │
└─────────────────────────────────┘
```

### Key UX requirements

1. **Download management**: Show size before download, progress bar, pause/resume,
   Wi-Fi-only option
2. **Storage management**: Show per-model disk usage, one-tap deletion, total usage in
   Settings
3. **Device compatibility check**: Detect RAM and chip, hide unsupported models, warn on
   marginal devices (e.g., 4 GB RAM + SDXL)
4. **Generation UI**: Prompt input, negative prompt (collapsible), step count slider,
   guidance scale, seed, aspect ratio presets
5. **Progress indication**: Step-by-step progress bar ("Step 8/20"), estimated time
   remaining, preview at intervals (if supported)
6. **Background generation**: Allow user to browse while generating; notification on
   completion
7. **Result handling**: Save to gallery, share, use as reference for CivitAI model search
   ("find models that produce similar images")

### Thermal management

Long inference causes thermal throttling on iPhone. UX should:
- Show thermal warning if device is already warm
- Throttle generation queue (max 1 concurrent)
- Suggest waiting between consecutive generations

## 7. KMP architecture

### Existing pattern

CivitDeck already uses `expect/actual` for on-device ML:

```kotlin
// commonMain
expect class ImageEmbeddingModel() {
    val isAvailable: Boolean
    suspend fun embed(imageBytes: ByteArray): FloatArray
}

// iOS uses SigLIP2Bridge (callback-based, wraps Core ML)
// Android uses ONNX Runtime directly
// Desktop is a no-op stub
```

### Proposed abstraction for diffusion

```kotlin
// commonMain — core-domain module
expect class OnDeviceDiffusionModel() {
    val isAvailable: Boolean
    val supportedModels: List<DiffusionModelInfo>
    suspend fun isModelDownloaded(modelId: String): Boolean
    suspend fun downloadModel(modelId: String, onProgress: (Float) -> Unit)
    suspend fun deleteModel(modelId: String)
    suspend fun generate(request: GenerationRequest): GenerationResult
}

data class DiffusionModelInfo(
    val id: String,
    val name: String,
    val sizeBytes: Long,
    val minRamGB: Int,
    val supportedResolutions: List<ImageSize>,
)

data class GenerationRequest(
    val modelId: String,
    val prompt: String,
    val negativePrompt: String = "",
    val steps: Int = 20,
    val guidanceScale: Float = 7.5f,
    val width: Int = 512,
    val height: Int = 512,
    val seed: Long? = null,
)

sealed class GenerationResult {
    data class Success(val imageBytes: ByteArray) : GenerationResult()
    data class Progress(val step: Int, val totalSteps: Int) : GenerationResult()
    data class Error(val message: String) : GenerationResult()
}
```

### iOS actual

```swift
// Swift bridge (similar to SigLIP2Bridge pattern)
protocol DiffusionBridge {
    var isAvailable: Bool { get }
    func supportedModels() -> [DiffusionModelInfo]
    func generate(request: GenerationRequest,
                  onProgress: @escaping (Int, Int) -> Void,
                  onResult: @escaping (Data?) -> Void)
}
```

Implementation would wrap Apple's `StableDiffusionPipeline` from ml-stable-diffusion,
or potentially Draw Things' open-source Metal FlashAttention kernels for better
performance.

### Android actual

Would use either:
- **ONNX Runtime + QNN EP** for broad device support
- **Qualcomm QNN SDK directly** for maximum Snapdragon performance
- **MediaPipe Image Generator** for simplest integration (but slowest)

### Desktop actual

No-op stub (same as `ImageEmbeddingModel`). Desktop users can use external tools
(ComfyUI, Automatic1111) which CivitDeck already integrates via the ComfyUI feature
module.

## 8. Risk assessment

| Risk | Severity | Mitigation |
|---|---|---|
| 4 GB RAM devices OOM | High | Gate feature behind RAM check; SD 1.5 only on 6 GB+ |
| Thermal throttling degrades UX | Medium | Limit queue, show warnings, cool-down timer |
| Model download size deters users | Medium | Clear size disclosure, Wi-Fi-only default, easy deletion |
| Qualcomm-only NPU on Android | High | CPU/GPU fallback exists but is 3–10x slower |
| Android fragmentation (Tensor, Dimensity, Exynos) | High | MediaPipe as universal fallback, QNN as premium path |
| Maintenance burden of ML pipeline | High | Two completely separate native implementations |
| App Store review (generated content) | Medium | NSFW safety checker, content policy compliance |
| Core ML conversion failures for newer models | Low | Apple provides pre-converted checkpoints on HF |

## Recommendation

### Should CivitDeck pursue this?

**Yes, but as a Phase 2 feature, not immediate priority.**

CivitDeck's core value is *browsing and discovering* AI-generated art on CivitAI. On-device
generation is a compelling companion feature ("try this model locally") but is not core to
the browsing experience. The engineering cost is high (two full native ML pipelines), the
UX complexity is significant (downloads, storage, thermals), and the device compatibility
matrix is fragmented.

### Phased approach

#### Phase 0: Research (this document) ✅

#### Phase 1: iOS-only SD 1.5 prototype (recommended next step)
- **Why iOS first**: Apple's ml-stable-diffusion is mature, well-documented, and has
  pre-converted models on Hugging Face. The `SigLIP2Bridge` pattern already exists.
- **Model**: SD 1.5, 6-bit palettized (~1 GB download)
- **Target devices**: iPhone 14 Pro+ (6 GB RAM), all M-series Macs
- **Steps**: 8 (LCM LoRA) for fast generation, 20 for quality
- **Scope**: Basic txt2img with prompt, negative prompt, seed
- **Estimated effort**: 2–3 weeks for a functional prototype

#### Phase 2: Android Qualcomm NPU path
- **Framework**: ONNX Runtime + QNN Execution Provider
- **Model**: SD 1.5, W8A16 quantized (~1 GB)
- **Target devices**: Snapdragon 8 Gen 2+ (Galaxy S23+, OnePlus 12+)
- **Fallback**: CPU inference for non-Qualcomm (warn about slow speed)
- **Estimated effort**: 3–4 weeks (QNN integration is less documented)

#### Phase 3: SDXL and advanced features
- SDXL support for 8 GB+ devices
- ControlNet / img2img
- Integration with CivitAI model pages ("Generate with this model")
- Model management UI in Settings
- Possible MediaPipe integration for broader Android support

#### Phase 4: Next-gen models (future)
- SD 3 / FLUX when mobile-viable conversion pipelines mature
- 1.58-bit quantization when custom kernels are production-ready
- MobileDiffusion if Google releases it publicly

### Why NOT right now

1. **SigLIP-2 embedding pipeline (#602) is still in progress** — completing the similarity
   search feature delivers more immediate user value
2. **Model download UX** requires significant new infrastructure (download manager,
   storage management, background downloads)
3. **App size perception** — even with on-demand download, the feature adds complexity to
   an app whose strength is lightweight browsing
4. **Android fragmentation** — non-Qualcomm devices (30–40% of the market) would have a
   degraded experience

### If we proceed, which framework?

| Platform | Recommended framework | Rationale |
|---|---|---|
| iOS | Apple ml-stable-diffusion (Core ML) | First-party, pre-converted models, ANE optimized |
| Android (Qualcomm) | ONNX Runtime + QNN EP | Best perf/compatibility balance |
| Android (other) | MediaPipe Image Generator | Only viable option; accept slower speed |
| Desktop | No-op (use external ComfyUI) | Not worth the effort; Desktop has full tools |

## References

- [Apple ml-stable-diffusion](https://github.com/apple/ml-stable-diffusion)
- [Apple ML Research — Stable Diffusion with Core ML](https://machinelearning.apple.com/research/stable-diffusion-coreml-apple-silicon)
- [Faster Stable Diffusion with Core ML (HF blog)](https://huggingface.co/blog/fast-diffusers-coreml)
- [SDXL Core ML quantization (HF blog)](https://huggingface.co/blog/stable-diffusion-xl-coreml)
- [apple/coreml-stable-diffusion-v1-5-palettized](https://huggingface.co/apple/coreml-stable-diffusion-v1-5-palettized)
- [coremltools — Optimizing Stable Diffusion](https://apple.github.io/coremltools/docs-guides/source/opt-stable-diffusion.html)
- [DiffusionKit (Argmax)](https://github.com/argmaxinc/DiffusionKit)
- [Draw Things — Metal FlashAttention](https://engineering.drawthings.ai/p/integrating-metal-flashattention-accelerating-the-heart-of-image-generation-in-the-apple-ecosystem-16a86142eb18)
- [Draw Things — Metal FlashAttention 2.0](https://engineering.drawthings.ai/p/metal-flashattention-2-0-pushing-forward-on-device-inference-training-on-apple-silicon-fe8aac1ab23c)
- [Draw Things — FlashAttention v2.5 Neural Accelerators](https://releases.drawthings.ai/p/metal-flashattention-v25-w-neural)
- [Qualcomm Stable Diffusion (HF)](https://huggingface.co/qualcomm/Stable-Diffusion)
- [Local Dream — Android SD with Snapdragon NPU](https://github.com/xororz/local-dream)
- [ONNX Runtime QNN Execution Provider](https://onnxruntime.ai/docs/execution-providers/QNN-ExecutionProvider.html)
- [MediaPipe Image Generator for Android](https://ai.google.dev/edge/mediapipe/solutions/vision/image_generator/android)
- [Google MobileDiffusion](https://research.google/blog/mobilediffusion-rapid-text-to-image-generation-on-device/)
- [MobileDiffusion paper (arXiv:2311.16567)](https://arxiv.org/abs/2311.16567)
- [Diffusion Models on the Edge (arXiv:2504.15298)](https://arxiv.org/abs/2504.15298)
- [1.58-bit FLUX (arXiv:2412.18653)](https://arxiv.org/abs/2412.18653)
- [ml-stable-diffusion Issue #291 — 4 GB memory](https://github.com/apple/ml-stable-diffusion/issues/291)
