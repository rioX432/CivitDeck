# SigLIP-2 On-Device Feasibility (Issue #698)

Status: **Decision** — see [Recommendation](#recommendation)
Parent: #602
Date: 2026-04-08

## Goal

Pick the SigLIP-2 variant and the on-device inference format CivitDeck will ship for image
similarity search. The downstream sub-issues (#700 iOS Core ML, #701 Android ONNX) implement
whatever is decided here.

Constraints from #602:
- Target devices: iPhone 12 / Pixel 6 class (A14 / Tensor G1)
- Per-image embed latency budget: < 500 ms
- Bundle size impact budget: < 150 MB per platform
- License must allow commercial app distribution

## SigLIP-2 model variants

Released by Google, February 2025 ([blog](https://huggingface.co/blog/siglip2),
[paper](https://arxiv.org/abs/2502.14786)). All weights are **Apache-2.0**
([model card](https://huggingface.co/google/siglip2-base-patch16-224)) — commercial use OK with
attribution.

| Family | Params | Patch | Resolutions on HF | Notes |
|---|---|---|---|---|
| Base | 86 M | 16 | 224 / 256 / 384 / 512 / NaFlex | Smallest, target for mobile |
| Large | 303 M | 16 | 256 / 384 / 512 | Too large for client bundle |
| So400m | 400 M | 14 / 16 | 224 / 256 / 384 / 512 / NaFlex | Best quality, server-side only |
| Giant | 1 B | 16 | 256 / 384 | Server-side only |

NaFlex variants accept variable resolution / aspect ratio, but require the
`Siglip2Model` runtime path and complicate ONNX/Core ML export. **FixRes is the only realistic
path for mobile.**

For the `base-patch16-224` config the image encoder is a standard ViT with `hidden_size=768`,
12 layers, 12 heads — embedding dimension after projection is **768** (verified against
`google/siglip2-base-patch16-224` config).

## Format options

### Android

The community already publishes pre-converted ONNX checkpoints at
[`onnx-community/siglip2-base-patch16-224-ONNX`](https://huggingface.co/onnx-community/siglip2-base-patch16-224-ONNX/tree/main/onnx).
**Vision encoder only** (we don't need the text tower for the MVP):

| File | Size | Notes |
|---|---:|---|
| `vision_model.onnx` (fp32) | 372 MB | Reference |
| `vision_model_fp16.onnx` | 186 MB | Over budget |
| `vision_model_int8.onnx` | 94.6 MB | Acceptable, classic INT8 |
| `vision_model_q4.onnx` | 63.3 MB | 4-bit weights, fp32 activations |
| **`vision_model_q4f16.onnx`** | **54.6 MB** | **4-bit weights + fp16 activations — pick this** |
| `vision_model_bnb4.onnx` | 57.6 MB | bitsandbytes 4-bit, niche tooling |

ONNX Runtime Mobile (`com.microsoft.onnxruntime:onnxruntime-android`) is the standard delivery
path on Android ([docs](https://onnxruntime.ai/docs/tutorials/mobile/)). NNAPI EP is available
but heuristic — start with the CPU EP and benchmark before chasing acceleration.

TFLite is **not** chosen: SigLIP-2 has no first-party TFLite export, the conversion path
(PyTorch → ONNX → TF → TFLite) is brittle, and ONNX Runtime gives us a single-format pipeline
shared with future work.

### iOS

No pre-built Core ML package exists for SigLIP-2 today. Conversion path:

```
google/siglip2-base-patch16-224  (PyTorch)
  └─ trace SiglipVisionModel.forward → torch.jit
      └─ coremltools.convert(... convert_to="mlprogram", compute_precision=FP16)
          └─ SigLIP2Vision.mlpackage  (~85 MB target, fp16 weights)
```

`coremltools` 8.x supports ML Program output (`.mlpackage`) and FP16 weight compression
([guide](https://apple.github.io/coremltools/docs-guides/source/convert-to-ml-program.html)).
The vision tower is plain ViT — no exotic ops — so the trace should succeed without surgery.
Risk surface lives in attention pooling and the final projection layer; if those fail, we drop
to ONNX Runtime on iOS as a fallback (ORT has an iOS pod, lower performance than Core ML / ANE
but works).

We do **not** ship `.mlmodel` (legacy) — `.mlpackage` is required for ML Program with weight
compression.

### Desktop

Per the parent decision in #602, Desktop ships a no-op stub (mirrors `DownloadScheduler`).
Out of scope here.

## Preprocessing

SigLIP image processor defaults
([source](https://github.com/huggingface/transformers/blob/main/src/transformers/models/siglip/image_processing_siglip.py)):

| Parameter | Value |
|---|---|
| Resize | 224 × 224 (BICUBIC) |
| Rescale | `1/255` |
| Normalize mean | `[0.5, 0.5, 0.5]` (IMAGENET_STANDARD_MEAN) |
| Normalize std | `[0.5, 0.5, 0.5]` (IMAGENET_STANDARD_STD) |
| Color space | RGB, NHWC → NCHW |

**Important**: SigLIP uses 0.5/0.5/0.5 (the *standard* ImageNet stats), not the more common
0.485/0.456/0.406 OpenAI/CLIP stats. Mismatching this silently produces garbage embeddings —
the determinism unit test in #700/#701 must compare against a known-good reference embedding
generated from the HF Python pipeline.

## Embedding output

- Dimension: **768** (float32)
- L2-normalize after extraction so cosine similarity reduces to a dot product
- Storage cost per model: 768 × 4 B = **3 KB** raw, 1.5 KB at fp16. The Room `model_embeddings`
  table in #699 should store fp16 BLOBs and decompress at query time
- For 10 k cached models: ~15 MB DB footprint at fp16 — fine

## Latency expectations

No first-party benchmarks exist for SigLIP-2 base on iPhone 12 / Pixel 6. Reasoning by analogy
from CLIP ViT-B/16 and similar 86 M ViTs:

| Device | Engine | Expected single-image latency | Source of estimate |
|---|---|---|---|
| iPhone 12 (A14) | Core ML / ANE, fp16 | 40–80 ms | Apple's own CLIP-ViT-B/16 conversions land in this range |
| iPhone 12 (A14) | Core ML / GPU, fp16 | 80–150 ms | If ANE rejects an op, falls back here |
| Pixel 6 (G1) | ONNX Runtime CPU, q4f16 | 250–450 ms | ORT CPU on ARM64 4-bit ViT-Base reports |
| Pixel 6 (G1) | ONNX Runtime NNAPI, fp16 | 120–250 ms | If NNAPI accepts the graph (often partial) |

All numbers are **estimates**, not measurements. Phase #700/#701 PRs must include real
on-device timings or the feature flag stays off.

## Recommendation

| Decision | Choice |
|---|---|
| Variant | **`google/siglip2-base-patch16-224`** (FixRes) |
| Embedding dim | 768, L2-normalized, stored fp16 |
| iOS format | `.mlpackage` ML Program, fp16 weights, target ~85 MB |
| Android format | `vision_model_q4f16.onnx` from `onnx-community`, ~55 MB |
| Desktop | No-op stub |
| Preprocessing | Resize 224×224 BICUBIC, mean/std = 0.5/0.5/0.5, NCHW |
| Text encoder | **Not shipped in MVP** — text-to-image search is backlog (#602) |
| Bundle impact | iOS ≈ 85 MB, Android ≈ 55 MB — both well under 150 MB |
| Fallback | If Core ML conversion fails on the projection layer, ship ORT on iOS too |

### Why Base / patch16 / 224 over the alternatives

- **Larger families (Large/So400m/Giant)** — even at INT4 the vision encoders cross 200 MB and
  push latency past the 500 ms budget on Pixel 6
- **Higher resolutions (256/384/512)** — quadratic compute cost; `base-patch16-224` is the only
  cell where both platforms fit the latency budget without acceleration tricks
- **NaFlex** — better for OCR/document, irrelevant for "find similar AI artwork" and harder to
  export
- **patch32 base** — coarser features, measurable retrieval-quality drop on natural images per
  the SigLIP-2 paper. Save the bytes elsewhere

## Open questions deferred to implementation PRs

- Real on-device latency on a physical iPhone 12 and Pixel 6 (#700, #701)
- Whether `coremltools` traces the projection head cleanly or needs `model.vision_model` only
- Whether ORT NNAPI EP is worth enabling on Android, or if CPU q4f16 already meets budget
- Whether to download the model on first launch vs. bundling — bundling is the default; revisit
  if combined size pushes the AAB past Play Store thresholds

## References

- [SigLIP 2 release blog](https://huggingface.co/blog/siglip2)
- [SigLIP 2 paper (arXiv 2502.14786)](https://arxiv.org/abs/2502.14786)
- [`google/siglip2-base-patch16-224`](https://huggingface.co/google/siglip2-base-patch16-224)
- [`onnx-community/siglip2-base-patch16-224-ONNX`](https://huggingface.co/onnx-community/siglip2-base-patch16-224-ONNX)
- [coremltools — Convert to ML Program](https://apple.github.io/coremltools/docs-guides/source/convert-to-ml-program.html)
- [ONNX Runtime Mobile](https://onnxruntime.ai/docs/tutorials/mobile/)
- [HF SigLIP image processor source](https://github.com/huggingface/transformers/blob/main/src/transformers/models/siglip/image_processing_siglip.py)
