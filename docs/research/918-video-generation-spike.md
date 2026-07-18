# SPIKE #918 — Video Generation via ComfyUI (Wan / Hunyuan / LTX-2)

Status: Research spike (no implementation). Decision doc only.
Date: 2026-07-18

---

## Decision: GO — but as a *thin, deferred-scope* MVP, gated behind the v2.4 launch + first feedback cycle

**One-liner:** Ship "trigger a user-provided video ComfyUI workflow → monitor with existing progress/notification infra → retrieve and save the resulting mp4/webm", and explicitly do NOT bundle, download, or manage any video model in-app.

**Rationale**
- The generation *submission* path is already model-agnostic (arbitrary workflow JSON is posted straight to `/prompt`), so a video workflow is genuinely "just another workflow template". The reuse claim in the issue holds up against the code.
- The real work is small and localized: ComfyUI returns video outputs under a *different* history key (`gifs`/`videos`) than images, and the app currently only parses `images`. That is a concrete, bounded gap — not an architecture change.
- Demand is real but the *mobile-triggered video* niche is narrower than mobile-triggered image gen, and the heavy-hardware reality means the addressable user is "someone with a capable desktop GPU who is away from it". This is a bet, so we keep the surface minimal and validate before investing further.
- A direct competitor (Comfy Portal) already advertises video outputs on mobile, so this is a parity gap, not a moonshot.

**Why "deferred / thin" rather than "full GO now":** Both prioritization axes (user requests + metrics) can't yet be confirmed for *video specifically* — we have image-gen usage but no video-gen signal from our own users. Per `ai-ops.md`, we file this as a Dev-Ready issue and let it compete, rather than committing a large build. The MVP is deliberately shaped so it costs little if the bet is wrong.

---

## Demand findings (cited)

- ComfyUI itself now treats video as a first-class, mainstream capability: the official native Wan 2.2 workflow docs and a single-interface "supports every major video model (Wan 2.2, LTX, Seedance, LongCat...)" positioning confirm video is *the* 2026 trend, not a fringe use case. ([docs.comfy.org Wan 2.2](https://docs.comfy.org/tutorials/video/wan/wan2_2), [ComfyUI Wiki Wan2.2](https://comfyui-wiki.com/en/tutorial/advanced/video/wan2.2/wan2-2))
- Mobile ComfyUI clients exist and are actively adding exactly this: **Comfy Portal** ("native iOS & Android client — run AI image generation workflows from your phone", connects to home or cloud/RunPod servers) explicitly added "support for multi-image and **video outputs**". This is the clearest demand + parity signal. ([Comfy Portal GitHub](https://github.com/ShunL12324/comfy-portal), [App Store](https://apps.apple.com/us/app/comfy-portal/id6741044736))
- The "queue a long job and come back to it" workflow is a recognized ComfyUI pain point — third-party queue-manager nodes exist specifically for persistence/archiving/resume of long renders across devices. This validates the "start it, walk away, get notified" UX we already have for images being valuable for the longer video jobs. ([ComfyUI-Queue-Manager](https://comfyai.run/custom_node/ComfyUI-Queue-Manager), [forum.comfy.org queue idea](https://forum.comfy.org/t/pause-button-add-queues-without-starting-the-calculation-immediately/814))

**Honest read:** demand for *remote/mobile video triggering* is inferred (competitor feature + general video-model momentum + long-job queue pain), not a loud direct chorus. Treat as "plausible, unproven for our users" — hence deferred scope.

---

## VRAM / accessibility reality — 2026 (cited)

The "niche 40–48GB hardware" objection has softened but not disappeared. Quantization made 14B video models run on 12–16GB consumer cards, at the cost of long generation times:

| Model | Naive VRAM | Quantized / offloaded reality | Speed (RTX 4090 unless noted) |
|---|---|---|---|
| Wan 2.2 14B | ~54 GB | GGUF Q4/Q5 + T5-CPU offload → **6–8 GB @480p, 12–16 GB @720p**; runs on RTX 3060 12GB / 4070 | 480p 5s clip: **18–22 min** on a 3060/4070; 720p **2–4 min** on RTX 5060 Ti 16GB ($429) |
| Wan 2.2 5B | — | fits **8 GB** with native offloading | fast |
| HunyuanVideo (8.3B) | 47 GB (official 32GB+ min) | FP8 → **~8–13.6 GB** @720p with offload | 4s 720p: **2–4 min** |
| LTX-2 (Lightricks, Mar 2026) | official 32 GB | NVFP8 → runs on **16 GB**; 2B FP8+tiling → **6–8 GB** | 4s 720p: **<30 s** (fastest open model) |

Sources: [willitrunai.com Wan VRAM](https://willitrunai.com/blog/wan-2-2-vram-requirements), [runaihome.com Wan GPU guide 2026](https://runaihome.com/blog/wan-video-local-ai-gpu-guide-2026/), [QuantStack Wan2.2 GGUF (HF)](https://huggingface.co/QuantStack/Wan2.2-I2V-A14B-GGUF), [localaimaster low-VRAM T2V](https://localaimaster.com/blog/local-text-to-video-low-vram), [willitrunai video GPU guide](https://willitrunai.com/blog/video-generation-gpu-guide-2026), [spheron GPU cloud video](https://www.spheron.network/blog/gpu-cloud-video-ai-2026/).

**Implication for CivitDeck:** we are a *controller*, not the compute. The hardware burden is entirely on the user's ComfyUI box (or their RunPod/cloud instance). Accessibility has improved enough (12GB entry, LTX-2 sub-30s) that a meaningful slice of users can run this. The long generation times (minutes, not seconds, for Wan/Hunyuan) are exactly why "trigger + background + notify" mobile UX is the right — and already-built — shape. This *strengthens* the fit rather than weakening it.

---

## Competitor landscape (cited)

- **Comfy Portal** (iOS + Android, native ComfyUI client): multi-server (local + cloud/RunPod), real-time status, and now **video outputs**. Directly overlapping and slightly ahead of us on video. ([GitHub](https://github.com/ShunL12324/comfy-portal), [App Store](https://apps.apple.com/us/app/comfy-portal/id6741044736))
- **Comfy Cloud / Comfy.org** — first-party hosted ComfyUI, positions video models as core. Not a mobile competitor but sets the "video is mainstream" baseline. ([comfy.org/cloud](https://comfy.org/cloud/))
- ComfyUI native + community workflows (official Wan 2.2 templates, Kijai/GGUF variants) are the "content" our users would import — i.e. the ecosystem produces the exact `.json` workflows our custom-workflow path already accepts. ([comfyui-wiki Wan2.2](https://comfyui-wiki.com/en/tutorial/advanced/video/wan2.2/wan2-2))

**Takeaway:** video-on-mobile is table stakes among ComfyUI mobile clients, and we are currently the one that can view but not generate it. This is a parity gap on a Core Value ("スマホから自宅ComfyUIを完全操作").

---

## Feasibility — grounded in CivitDeck code

### What genuinely reuses existing infra (the issue's claim holds)

1. **Submission is already model-agnostic.** `ComfyUIGenerationParams.customWorkflowJson` is decoded and POSTed verbatim to `/prompt` — no node-type awareness at all:
   - `ComfyUIGenerationRepositoryImpl.buildWorkflow()` → `feature/feature-comfyui/.../data/repository/ComfyUIGenerationRepositoryImpl.kt:140-143` (`if (customJson != null) return json.decodeFromString(customJson)`)
   - Same short-circuit in `ComfyUIWorkflowBuilder.kt:21`.
   - A full template system already exists: `WorkflowTemplate` domain model with typed `variables` (`core/core-domain/.../domain/model/WorkflowTemplate.kt`), plus `ImportWorkflowTemplateUseCase`, `ExtractWorkflowParametersUseCase`, `InjectWorkflowParametersUseCase`, `ApplyWorkflowTemplateUseCase`, and app-mode metadata parsing. A Wan/LTX i2v/t2v workflow drops into this pipeline as data — **no new submission code**. The "video workflow = another template type" premise is confirmed.

2. **Progress + live preview reuses fully.** `ObserveGenerationProgressUseCase` → `ComfyUIGenerationRepositoryImpl.observeGenerationProgress()` → `ComfyUIWebSocketApi.observeProgress()` handles `Progress` (step/max), `Executing`, `Executed`, `ExecutionSuccess`, `ExecutionError`, and `PreviewImage` (`core/core-network/.../ComfyUIWebSocketDto.kt`, `ComfyUIWebSocketApi.kt`). During video sampling ComfyUI still emits latent **preview frames as PreviewImage bytes** — so the existing image-preview UI shows a live per-frame preview with zero changes. Good enough for MVP; a "scrub the preview as video" nicety is a non-goal.

3. **Completion notification is already robust to backgrounding / connection drop.** `NtfySubscriptionService` (`feature/feature-comfyui/.../data/NtfySubscriptionService.kt`) subscribes to an ntfy topic over an *independent* streaming connection with auto-reconnect + exponential backoff, and fires a local notification on completion. This is decoupled from the WebSocket, so a minutes-long video job that outlives a foreground WS session still notifies correctly. This is the single most important piece for long-job UX and it already exists (shipped v2.2.0).

4. **Viewing the result reuses existing video support.** The app already renders `MediaContentType.VIDEO/ANIMATION`, so playing back a retrieved mp4/webm is not new capability — only the retrieval + save plumbing is.

### Genuine gaps (bounded, localized)

- **GAP 1 — Output retrieval only understands images (the core change).** History parsing reads *only* the `images` key:
  - `HistoryNodeOutput` DTO has a single field `val images: List<ComfyUIOutputImage>? = null` — no `gifs`/`videos`/`animated` (`core/core-network/.../ComfyUIDto.kt:40-43`).
  - `pollGenerationResult()` flattens `entry.outputs.values.flatMap { it.images }` and, for a successful *video* job with no `images`, returns `GenerationStatus.Error("No images generated")` (`ComfyUIGenerationRepositoryImpl.kt:62-69`).
  - `ComfyUIHistoryRepositoryImpl.toGeneratedImages()` has the same images-only assumption.
  - **Fix shape:** add optional `gifs` (VHS_VideoCombine emits `outputs[node].gifs = [{filename, subfolder, type, format}]`) and native `videos`/`animated` fields to the output DTO; treat any of them as a completed result; carry a media-kind flag through `GenerationResult` (currently `imageUrls: List<String>`) so the UI knows to play vs show. This is the only non-trivial change and it's contained to DTO + two parse sites + one domain model.

- **GAP 2 — Saving assumes still images.** `ImageSaver.saveToGallery(imageBytes, filename)` writes image bytes to Photos/Pictures as an image (`feature/feature-comfyui/.../data/image/ImageSaver.kt`, platform impls Android MediaStore Pictures / iOS PHPhotoLibrary). Saving mp4/webm needs a video save path (MediaStore `Movies` / `PHAssetCreationRequest` video). New `expect/actual` method, mechanical per platform.

- **GAP 3 — Long-job / large-output robustness (verify, likely small).**
  - The WS flow catches `HttpRequestTimeoutException`/`ConnectTimeoutException` and retries (`ComfyUIWebSocketApi.observeProgress()` lines ~55-73). A multi-minute streaming WS must not be killed by any app-level Ktor request timeout — **verify the WS client has no finite request timeout** (or that a reconnect cleanly re-attaches without losing the terminal event; ntfy backstops completion regardless). Low risk, needs a one-line confirmation during implementation.
  - Large mp4/webm: retrieval via the `/view` endpoint (same as `getImageUrl`) streams fine, but a naive "load whole file into `ByteArray` then save" is heavy for longer clips. A 5s 720p clip is ~2–10 MB (fine); guard against unbounded loads for long clips by streaming to a file. Playback already downsamples/streams on the VIEW path.

**Net:** ~1 real feature (video output parsing + media-kind in the result model) + 1 mechanical platform save path + 1 verification. No new submission, progress, preview, notification, or connection code. The issue's feasibility premise is validated.

---

## Proposed MVP scope

**In scope (smallest viable):**
1. User imports/pastes a video ComfyUI workflow JSON (Wan i2v/t2v, LTX, Hunyuan — *their* file, *their* models) via the existing custom-workflow / template import path.
2. Trigger it via the existing `/prompt` submission — unchanged.
3. Monitor via existing WebSocket progress + latent preview frames, and existing ntfy completion notification.
4. **New:** parse video outputs (`gifs`/`videos`) from `/history`, mark the result as video-kind.
5. **New:** play the resulting mp4/webm using existing `MediaContentType.VIDEO` viewer, and save it to the gallery via a new video save path.

**Explicit non-goals (do NOT build):**
- No bundling, downloading, hosting, or managing of video models/checkpoints in-app (aligns with `CLAUDE.md` Won't-Do: heavy model management is ComfyUI's job).
- No in-app or on-device video inference of any kind (Won't-Do).
- No cloud/hosted generation service (Won't-Do) — user brings their own ComfyUI (local or their own RunPod).
- No visual node editor / workflow authoring for video — import only, reuse existing template variables.
- No video-specific live "preview scrubbing" — per-frame PreviewImage is sufficient for MVP.
- No frame-interpolation / upscaling / post-processing UI — that lives in the user's workflow.
- No curated/first-party video workflow catalog in the MVP (could follow if validated).

---

## Recommendation

**File a follow-up Dev-Ready issue now, but keep it below image-gen polish in priority until we see a video signal.**

Concretely:
1. Create a Dev-Ready issue "Video generation: parse & retrieve ComfyUI video outputs + save mp4/webm" with a Core Value Alignment section (Core Value 3: "スマホから自宅ComfyUIを完全操作") and the MVP scope above.
2. Scope it to the three gaps only (output DTO + result media-kind, video save path, long-WS verification). Estimate: small — reuses submission/progress/preview/notification/viewer.
3. **Revisit trigger to prioritize:** promote it when *either* (a) our own post-launch feedback/metrics show users importing video workflows or requesting video, **or** (b) a competitor parity concern is raised (Comfy Portal video traction). Until then it stays queued, not committed — consistent with the 2-axis rule in `ai-ops.md`.

The bet is de-risked because the expensive parts (submission genericity, async monitoring, background notification, video viewing) already exist; we're only teaching the app to *recognize and keep* a video that ComfyUI hands back.

---

## Codex cross-review (2026-07-18) — GO/defer CONFIRMED, "only 3 gaps" framing corrected

Codex agreed with GO-but-defer and that no video-specific graph builder is needed, but was
blunt that this is **not "GAP 1 + mechanical saving"** — file it as a bounded *media-output*
feature with a compatibility matrix and a fixture-validation gate, not near-total reuse.
Corrections the Dev-Ready issue must absorb:

- **Submission genericity holds, but "user-provided workflow JSON" is under-specified.** It must
  be **API-format** JSON (Save (API Format)), not a UI-editor export. The current importer only
  checks "non-empty JSON object" (`ComfyUIUseCases.kt:63`) — a UI export passes validation then
  fails at `/prompt`. MVP must validate API-format and declare a clear input-file contract:
  support **self-contained / server-side-file** workflows (text-to-video fits); arbitrary
  i2v/v2v/audio-conditioned workflows that reference uploaded video/audio/masks are **out of
  scope** (existing upload plumbing is image/mask-oriented). There is no `VIDEO` template type —
  "just another template" is misleading from a product/UI angle.
- **GAP 1 model is too narrow.** `gifs` commonly carries `.mp4`/`.webm` (the key name does NOT
  identify media kind); other nodes use `videos`/`video`/`images`. Replace "one media-kind flag
  + URL list" with a **per-output item**: `GeneratedMedia(filename, subfolder, type, url,
  mediaKind, mimeOrFormat?)`. Classify by **filename extension + `format`/MIME, not the JSON
  key**. Preserve `subfolder`/`type` for `/view`. Select final outputs via APP-mode declared
  outputs → `type=output` saved media, avoiding temp/preview intermediates. Update BOTH the
  immediate-result path AND the history path (`ComfyUIGeneratedImage.kt:7`,
  `ComfyUIHistoryRepositoryImpl.kt:38` are image-only). Fixture-test: VHS MP4 under `gifs`, VHS
  WebM under `gifs`, native `SaveVideo`/`SaveWEBM`, multiple clips, mixed preview+final, non-empty
  subfolder, completed-but-no-saved-media.
- **Live preview = best-effort, not full reuse.** Latent preview frames aren't guaranteed across
  node packs / sampler impls / server preview settings / non-sampling stages (interpolation,
  decode, encode). **Progress is node-local**: multi-stage video graphs emit several independent
  `value/max` sequences → the single bar jumps backward / shows "complete" while encoding
  continues. MVP: determinate while a node emits valid steps, **indeterminate "processing/
  encoding" between/after**, never present sampler % as whole-workflow %.
- **ntfy is an optional second signal, NOT the completion authority** (spike overstated it).
  `NtfySubscriptionService` starts as a side effect of collecting settings VM state (not app
  startup), lives in-process (dies on process kill), can't be assumed alive while iOS is
  suspended, treats **every** ntfy message as success, and uses the ntfy message ID as the prompt
  ID with **no prompt correlation**, and needs a server-side ntfy node configured. **History by
  `promptId` must remain authoritative.**
- **Missed gaps (the important ones):**
  - **Long-job reconciliation** — Ktor has 120s request/socket timeouts and WS reconnect caps at
    5 attempts (`HttpClientConfig.kt:25`); if the socket drops and the job finishes before
    reconnect, ComfyUI may not replay the terminal event. Reconnect MUST query history/queue and
    reconcile — don't rely on the WS terminal event.
  - **6-minute polling ceiling** — fallback poll is 120×3s ≈ 6 min (`GenerationExecutionDelegate.kt:246`),
    inadequate for video. Remove/raise the fixed ceiling for video jobs.
  - **Failure reporting** — OOM / missing nodes-models / encoder failure / interruption need the
    server exception + node details surfaced; current polling can read an unsuccessful terminal
    history entry as "still running" → eventually "timed out".
  - **Playback integration is real work** — result/history screens are image grids using image
    loaders; `MediaContentType.VIDEO` is infrastructure, not wired into these screens.
  - **Format compatibility** — iOS AVPlayer can't be assumed to play arbitrary WebM/VP9/AV1.
    **H.264 MP4 is the safe MVP baseline**; WebM best-effort or "unsupported — save/open externally".
  - **Large-file transport** — stream to a temp file / MediaStore-Photos with cancellation,
    progress, content-length, cleanup, disk-space errors; current image save reads the whole
    response into a `ByteArray` (`SaveGeneratedImageUseCase.kt:16`). Over a remote tunnel, retries
    + Range/resumability + interrupted-download cleanup matter.
  - **Saving is not fully mechanical** — the ComfyUI-specific saver always writes JPEG to
    Pictures; iOS decodes bytes as `UIImage`. Video needs a temp file + Photos asset creation.

**Revised acceptance criteria for the Dev-Ready issue:** API-format + server-self-contained
workflow only; MP4/H.264 required, WebM best-effort/unsupported on iOS; per-item media output
model; VHS + one native save-node fixture; reconnect/history reconciliation with no fixed 6-min
timeout; best-effort preview + node-local progress; streamed retrieval + platform video saving;
ntfy optional, never source of truth. **Add a short fixture-validation gate before estimating.**
Priority: still **defer below image-gen polish** — Portal parity alone isn't urgent given the
narrower "capable-GPU-but-away-from-it" audience.
