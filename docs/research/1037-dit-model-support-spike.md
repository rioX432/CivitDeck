# SPIKE #1037 — DiT-era model support in built-in ComfyUI generation

Status: research only (no implementation). Decision note for owner review on the PR.
Follow-up implementation issues are filed only after this document merges.
Code references are to `origin/master` at `37d4f306` (2026-09-26). ComfyUI references are to
v0.37.0 (`79be670e`, master on 2026-09-26). Template references are to
`Comfy-Org/workflow_templates` main at `25ff90a5`.

"DiT-era models" here means image models such as Krea 2 and Anima that ship the image model,
the text encoder and the VAE (the image decoder) as **separate files**. ComfyUI loads them with
three loader nodes (`UNETLoader`, `CLIPLoader`, `VAELoader`) instead of the one
`CheckpointLoaderSimple` that the app's built-in form uses today.

## Decision

| # | Question | Chosen option | One-line reason |
|---|---|---|---|
| 1 | Where the model lists come from | **`/object_info/UNETLoader`, `/object_info/CLIPLoader` and `/object_info/VAELoader`**, parsed like the LoRA list. On an older server a missing node or type value hides or disables the new choices; checkpoint generation is never blocked. | These are exactly the values the loader nodes accept, so a picked name always passes ComfyUI's `/prompt` check, and `CLIPLoader` also returns the list of `type` values the server supports. `/models/{folder}` has neither property. |
| 2 | Shape of the workflow | **One generic "split loader" builder plus an in-app table of model families** (CLIP `type`, companion-file hints, sampler defaults). Not ComfyUI's `/templates`. MiniMax H3 and anything the table does not cover go through imported workflows. | The builder already handles prompts, LoRAs, size and seed; only the loader part changes. `/templates` files are UI format inside subgraphs, and converting them would mean porting the ComfyUI frontend. |
| 3 | CivitAI `baseModel` → family | **The loader follows the server folder the file is in; the family comes from an exact match on `ModelVersion.baseModel`** (`Krea 2`, `Anima`). Unknown values keep today's checkpoint path, or ask the user to pick a family or use an imported workflow. No file-name guessing. | CivitAI's file type `Model` is used for both all-in-one and diffusion-only files, and file names are unreliable (an Anima model named `novaAnimeAM_…`, a Wan 2.2 model named `…-Animate-…`). |
| 4 | Generation form and model picker | **One model picker with "Checkpoints" and "Diffusion models" groups.** Picking a diffusion model shows family, text encoder and VAE pickers and hides ControlNet and inpainting. Android and iOS get the same shared state; Desktop is unchanged. | Keeps a single entry point, and a server with no diffusion models looks exactly as it does today. |
| 5 | Split | **12 follow-up issues** (8 shared, 2 Android, 2 iOS), each ≤5 production files. See §5. | The API, builder, form state, selection logic, platform UI and prefill are separate layers that can each be reviewed and merged on their own. |

Coverage: Krea 2 and Anima are 52 of CivitAI's 100 most downloaded models this month. MiniMax
H3 (21 of 100) is a video-and-audio model, so the image form cannot run it; it stays on the
imported-workflow path together with #984 (video outputs).

---

## Current state (verified)

App:

- The live builder is `ComfyUIGenerationRepositoryImpl.buildWorkflow`
  (`feature/feature-comfyui/.../data/repository/ComfyUIGenerationRepositoryImpl.kt:141-190`).
  Node `"3"` is always `CheckpointLoaderSimple` (`buildCheckpointNode`, `:235-238`). The LoRA
  chain (`buildLoraChain`, `:351-373`), both `CLIPTextEncode` nodes and `VAEDecode` take their
  model, CLIP and VAE from node `"3"` outputs 0, 1 and 2. Inpainting (`:192-231`) does the same.
  A custom workflow JSON bypasses the builder (`:142-146`).
- `ComfyUIWorkflowBuilder.kt` is a dead duplicate used only by the unregistered
  `ComfyUIRepositoryImpl`.
- Model lists: checkpoints come from `/object_info/CheckpointLoaderSimple` through a typed DTO
  (`core/core-network/.../api/comfyui/ComfyUIApi.kt:66-69,271-277`, the #1023 bug). LoRAs and
  ControlNets use `parseNodeInputList`, which reads element 0 of the input and returns an empty
  list for a missing node (`:78-93,284-302`). It also returns an empty list for the V3
  `["COMBO", {"options": […]}]` shape (see ComfyUI below).
- `ComfyUIGenerationParams` (`core/core-domain/.../model/ComfyUIConnection.kt:70-104`) has one
  `checkpoint: String` and no UNET, text encoder or VAE fields. `samplerName` and `scheduler`
  default to `euler` / `normal`.
- `GenerationUiState` (`ComfyUIGenerationViewModel.kt:16-63`) holds `samplerName` and
  `scheduler`, but no form shows them and the VM has no setter for them. `width`/`height`
  default to 512.
- `ComfyUIGenerationViewModel` has 27 functions. detekt 1.23.8 `TooManyFunctions`
  (`config/detekt/detekt.yml`, `thresholdInClasses: 30`) reports a class once its count
  reaches 30. #1038 (`applyPrefill`) and #1041 (template apply) each add one, which leaves room
  for **zero** more.
- `GenerationResourceLoader` shows checkpoint load errors and ignores LoRA and ControlNet load
  errors (`GenerationResourceLoader.kt:20-35,37-57`).
- Imported workflows: `ImportWorkflowUseCase` accepts any non-empty JSON object
  (`ComfyUIUseCases.kt:63-82`), including the UI format that `/prompt` rejects. Parameter
  extraction (`ExtractWorkflowParametersUseCase.kt:301-315`) covers `KSampler`,
  `KSamplerAdvanced`, `CLIPTextEncode`, `CheckpointLoaderSimple`, `LoraLoader` and
  `EmptyLatentImage` only. `git grep widgets_values` finds nothing, so no UI→API conversion exists.
- Observed, not run against a live server: the APP-mode import path accepts an API-format map
  with a top-level `extra` block (`ExtractWorkflowParametersUseCaseTest.kt:73-88`), and the submit
  path posts that JSON unchanged (`ComfyUIGenerationRepositoryImpl.kt:142-146`). ComfyUI rejects
  any top-level entry without `class_type` (`execution.py:1128-1144`). Out of scope here.
- `ModelVersion.baseModel: String?` (`core/core-domain/.../model/ModelVersion.kt:9`) is passed
  through from the API unchanged. `BaseModel` (`BaseModel.kt:3-11`) is a 7-value search-filter
  enum and does not include any DiT family.
- `PopulateGenerationFromModelUseCase` (`ComfyUIUseCases.kt:136-163`) replaces missing steps
  and CFG with the generic defaults 20 and 7.0. That is a problem for turbo models (§3).
- History reads LoRA names only from `LoraLoader` nodes
  (`ComfyUIHistoryRepositoryImpl.kt:109-113`).
- Try in ComfyUI: Android `LibraryNavEntries.kt:183-203` builds `ComfyUIBridgeRoute`
  (`NavRoutes.kt:39-50`: no base model and no file name), and `CreateNavEntries.kt:103-115`
  ignores its fields. iOS `ModelDetailScreen.swift:129,283-291` opens an empty sheet. #1038,
  #1039 and #1040 add the prefill.
- UI: Android `CheckpointSelector` (`ComfyUIParameterSection.kt:30-59`, hardcoded
  "Select checkpoint..."), generate enablement at `ComfyUIResultSection.kt:48-49`; iOS
  `checkpointPicker` (`ComfyUIGenerationView.swift:105-121`), generate enablement at `:312-313`.
  `ComfyUIGenerationView.swift` is 487 lines, and SwiftLint `file_length` warns at 500
  (`iosApp/.swiftlint.yml`), which `--strict` turns into an error. Desktop has its own checkpoint
  picker on the same VM (`DesktopComfyUIGenerationSection.kt:20-33,80-86`).

ComfyUI (v0.37.0):

- `UNETLoader`: `unet_name` from the `diffusion_models` folder, `weight_dtype` in
  `default | fp8_e4m3fn | fp8_e4m3fn_fast | fp8_e5m2` (`nodes.py:986-1009`).
- `CLIPLoader`: `clip_name` from `text_encoders`, `type` enum of 29 values including
  `stable_diffusion`, `lumina2`, `qwen_image`, `krea2` and `minimax`, but no `anima`
  (`nodes.py:1011-1036`). A value outside a combo list fails `/prompt` validation with
  "Value not in list" (`execution.py:1045-1072`).
- `VAELoader`: `vae_name` from `vae` plus synthetic entries (`taesd…`, `pixel_space`)
  (`nodes.py:774-834`). `DualCLIPLoader` exists (`:1038-1063`) but no v1 family needs it.
- `LoraLoader` patches only the keys it can map, so it also works for LoRAs without
  text-encoder weights (`comfy/sd.py:107-137`). The templates use `LoraLoaderModelOnly`
  (`nodes.py:759`).
- Folders: `text_encoders` = `text_encoders/` + legacy `clip/`, `diffusion_models` =
  `unet/` + `diffusion_models/` (`folder_paths.py:30-31`). The legacy names map only through
  `map_legacy` (`:111-114`). `.gguf` is not a supported extension (`:10`).
- Endpoints: `/object_info/{node_class}` returns `{}` for an unknown class
  (`server.py:816-822`). `/models/{folder}` lists a folder with no legacy mapping and returns
  404 for an unknown folder (`:342-354`). `/view_metadata/{folder}` returns only the optional
  `__metadata__` header of a safetensors file (`:666-687`). `/templates` depends on the
  installed templates package (`:1249-1273`).
- V3 nodes serialise a combo input as `["COMBO", {"options": […]}]`
  (`comfy_api/latest/_io.py:1932-1937`). V1 nodes, including all four loaders above, return
  `[[…], {…}]`.
- Text encoder routing (`comfy/sd.py:1943-1982`): the Krea 2 encoder (Qwen3-VL-4B) gets Krea 2
  conditioning **only** when `type` is `krea2`. The Anima (Qwen3 0.6B) and MiniMax H3
  (Qwen3-VL-32B) encoders are chosen by their weights, whatever the `type`.
- Model configs (`comfy/supported_models.py`): MiniMax H3 uses an audio-video latent
  (`:964-990`). Anima (`:1135-1165`) and Krea 2 (`:1988-2014`) define `clip_target` and a VAE key
  prefix, so an all-in-one file of either also loads through `CheckpointLoaderSimple`.
- At CFG 1 the sampler skips the negative prompt (`comfy/samplers.py:609-611`).

Official templates (all UI format, with the pipeline inside a subgraph):

| Template | Loaders | Sampler | Latent |
|---|---|---|---|
| `image_krea2_turbo_t2i.json` | `UNETLoader krea2_turbo_fp8_scaled / default`, `CLIPLoader qwen3vl_4b_fp8_scaled / krea2`, `VAELoader qwen_image_vae` | `KSampler` 8 steps, CFG 1, `euler`, `simple`; negative via `ConditioningZeroOut` | `EmptyLatentImage` 1024×1024 |
| `image_krea2_turbo_t2i_int8.json` | same, UNET `krea2_turbo_int8_convrot / default` | same | same |
| `image_anima_base_v1.json` | `UNETLoader anima-base-v1.0`, `CLIPLoader qwen_3_06b_base / stable_diffusion`, `VAELoader qwen_image_vae` | `KSampler` 30 steps, CFG 4, `euler`, `simple`; real negative prompt | `EmptyLatentImage` 1024×1024 |
| `image_z_image_turbo.json` | `CLIPLoader qwen_3_4b / lumina2`, `VAELoader ae` | `ModelSamplingAuraFlow` 3, `KSampler` 8, CFG 1, `res_multistep`, `simple` | `EmptySD3LatentImage` 1024×1024 |
| `video_minimax_h3_t2v.json` | `CLIPLoader qwen3vl_32b / minimax`, two `VAELoader`s | `SamplerCustomAdvanced`, `MiniMaxH3ImageToVideo` | video + audio, `SaveVideo` |

CivitAI (`/api/v1/models`, fetched 2026-09-26T07:57Z without an API key):

- Top 100 by downloads this month: Krea 2 31 (20 LoRA, 10 Checkpoint, 1 Workflow), Anima 21
  (11 / 9 / 1), MiniMax H3 21 (16 / 2 / 3), Illustrious 9, Qwen 2.1 5, ZImageTurbo 4,
  SDXL 1.0 3, others ≤2.
- A "Checkpoint" version can ship a diffusion-only file (`Diffusion Model` or `Model`), an
  all-in-one file (`…_full_fp8`, `Model`), separate `Text Encoder` and `VAE` files (WAI-ANIMA),
  and quantised variants (fp8, int8, int4, nvfp4). The `Model` file type appears for both
  diffusion-only and all-in-one files.

---

## 1. Where the model lists come from

**Chosen:** three per-node calls, `/object_info/UNETLoader` (`unet_name`),
`/object_info/CLIPLoader` (`clip_name` and `type`) and `/object_info/VAELoader` (`vae_name`),
parsed by `parseNodeInputList`. **Reason:** these are the lists `/prompt` validates against, and
the `type` list tells the app which families the server can run.

| Option | Verdict | Why |
|---|---|---|
| A. `/object_info/<Loader>` per loader | **Chosen** | Same mechanism as LoRAs and ControlNets. A missing node returns `{}`, which the parser already turns into an empty list. |
| B. `/models/{folder}` | Rejected | It has no `type` list, so the app cannot tell whether the server supports `krea2`. The folder key is not mapped from legacy names, so the right key depends on the server version (`unet`/`clip` on older servers). It also leaves out the VAE entries the loader accepts. |
| C. The full cached `/object_info` (`FetchObjectInfoUseCase`) | Rejected | The 30-minute cache hides files the user just added, and the response describes every installed node when three are needed. |

Parser requirement: `parseNodeInputList` must also read the V3 `["COMBO", {"options": […]}]`
shape. Today it returns an empty list for it. The four loaders are V1 today, but a later
ComfyUI release could migrate them, and the parser would then fail silently.

Behaviour on older or partial servers:

- **No `UNETLoader`** (`{}`): the diffusion list is empty and the picker looks as it does today.
- **The family's CLIP `type` is missing from the server's `type` list** (for example `krea2` on
  an older release): the family is shown disabled with "Needs a newer ComfyUI".
- **Anima cannot be gated**, because its `type` is `stable_diffusion`, which every server has.
  On a server that is too old for Anima the run fails, and #1031 shows ComfyUI's error.
- **A fetch fails**: the lists stay empty with no error message, like LoRAs. Checkpoint
  generation never depends on these calls.
- Lists load once when the form opens, like checkpoints.

---

## 2. Shape of the workflow

**Chosen:** extend the existing builder with a split-loader variant and add an in-app table of
families. **Reason:** only the loader part of the graph differs, and the builder already injects
prompts, LoRAs, size and seed.

| Option | Verdict | Why |
|---|---|---|
| A. Generic split-loader builder + family table | **Chosen** | One graph serves every UNET + one-text-encoder + VAE family. A new family is one table row, plus optional extra nodes when its template needs them (S8). |
| B. Fetch `/templates` and convert them | Rejected | Every template is UI format with the pipeline inside a subgraph and frontend-only nodes (`MarkdownNote`, `TextGenerate`). Converting it means rebuilding widget-to-input mapping, subgraph flattening and link resolution from the ComfyUI frontend, and the result depends on the templates package version the server has installed. |
| C. Imported workflows only | Rejected as the main path | The user would need a desktop to export API JSON for every model, and Try in ComfyUI could not open a ready form for the most downloaded models. It stays the path for everything outside the table (S6, S7). |
| D. One hardcoded API graph per family (like `BuiltInWorkflowsJson.kt`) | Rejected | Every graph would need its own LoRA, seed and size injection, which the builder already does once. |

Graph for a diffusion model (S2). Node ids keep the model link at `"3"`, so the LoRA chain and
`KSampler` code stays the same:

| Node | Class | Inputs |
|---|---|---|
| `"3"` | `UNETLoader` | `unet_name`, `weight_dtype: "default"` |
| `"40"` | `CLIPLoader` | `clip_name`, `type` from the family |
| `"41"` | `VAELoader` | `vae_name` |
| `"10"`… | `LoraLoader` | first link: model `["3",0]`, clip `["40",0]` |
| `"6"`, `"7"` | `CLIPTextEncode` | clip from the last LoRA or `["40",0]` |
| `"5"`, `"4"`, `"8"`, `"9"` | `EmptyLatentImage`, `KSampler`, `VAEDecode` (vae `["41",0]`), `SaveImage` | as today |

Choices inside this graph:

- **`LoraLoader`, not `LoraLoaderModelOnly`.** It works for model-only LoRAs (unmapped keys are
  only logged), keeps the existing strength UI, and keeps LoRA names in history, which reads
  only `LoraLoader`. Whether each DiT text encoder accepts a CLIP LoRA patch has not been run on
  a live server. S2's manual check covers it.
- **`weight_dtype` is always `"default"`**, as in both Krea 2 templates, including the int8 one.
  Quantised files are detected by ComfyUI. int4 and nvfp4 files were not run; a failure
  surfaces through #1031.
- **The negative prompt stays a `CLIPTextEncode`.** At CFG 1 ComfyUI ignores it, which matches
  the template's `ConditioningZeroOut`. One graph then serves every CFG value.
- **ControlNet and inpainting stay checkpoint-only.** The builder rejects a diffusion model
  combined with either, and the form hides both (§4).

Family table (v1 rows S4, later row S8; sampler values from the templates above):

| Family | CivitAI `baseModel` | CLIP `type` (server must list it) | Text encoder hint | VAE hint | Steps | CFG | Sampler / scheduler | Size |
|---|---|---|---|---|---|---|---|---|
| Krea 2 | `Krea 2` | `krea2` | `qwen3vl_4b` | `qwen_image_vae` | 8 | 1 | `euler` / `simple` | 1024×1024 |
| Anima | `Anima` | `stable_diffusion` | `qwen_3_06b` | `qwen_image_vae` | 30 | 4 | `euler` / `simple` | 1024×1024 |
| Z-Image Turbo (S8) | `ZImageTurbo` | `lumina2` | `qwen_3_4b` | `ae.safetensors` | 8 | 1 | `res_multistep` / `simple` | 1024×1024, plus `EmptySD3LatentImage` and `ModelSamplingAuraFlow` shift 3 |

A hint is a case-insensitive prefix of the file's base name (the part after the last `/` or `\`).
It only preselects the text encoder or VAE, and the user can change it. It never decides the
family or the loader.

Not in the table:

- **MiniMax H3**: video and audio → imported workflow, with #984 for video outputs.
- **GGUF files**: ComfyUI core does not list them (they need a custom loader node) → imported
  workflow.
- **Families not yet verified** (for example Qwen 2.1, 5 of the top 100) → a later table row,
  each checked against its official template first.

---

## 3. CivitAI `baseModel` → family and loader

**Chosen:** two separate decisions. **Reason:** the file's folder on the server decides which
loader can open it, and only the CivitAI base model reliably names the family.

1. **Loader.** The loader follows the server list the selected file is in: `checkpoints` →
   `CheckpointLoaderSimple`, `diffusion_models` → `UNETLoader`. `baseModel` and the CivitAI
   file type are never used for this, because `Model` covers both all-in-one and diffusion-only
   files. On prefill, a file found in both lists counts as a diffusion model when a family is
   known, and as a checkpoint otherwise.
2. **Family.** The family is set on prefill by an exact match of `ModelVersion.baseModel`
   (trimmed, case-insensitive) against the table's `CivitAI baseModel` column. Otherwise the
   user picks it. It is not guessed from file names. `novaAnimeAM_v5029B.safetensors` is an
   Anima model with no `anima` in its name. The official
   `template_purz_wan22_animate_auto_character_replace.json` loads
   `Wan2_2-Animate-14B_fp8_e4m3fn_scaled_KJ.safetensors`, which contains "Anima" but is a Wan
   model. `/view_metadata` cannot identify the family either: it returns only the optional
   `__metadata__` header, not the weights ComfyUI uses to detect a model.

Fallbacks:

| Case | Result |
|---|---|
| Known family, file in `diffusion_models` | Split-loader graph. Text encoder and VAE are preselected by hint. |
| Known family, file in `checkpoints` (an all-in-one file) | `CheckpointLoaderSimple`. On prefill, the family's steps, CFG, sampler, scheduler and size are used as defaults. |
| Unknown `baseModel`, file in `checkpoints` (SD 1.5, SDXL, Pony, Illustrious, NoobAI, …) | Today's checkpoint path, unchanged. |
| Unknown `baseModel`, file in `diffusion_models` | No family is preselected. Generate stays disabled until the user picks one. If none fits, a hint points to imported workflows. |
| Known family, file on neither list (not downloaded to the server) | No model is selected (instead of the first checkpoint, as #1038 does for unknown checkpoints), because the family's defaults would be wrong for an arbitrary checkpoint. |

Defaults on prefill (S5): the CivitAI image metadata wins where it has a value, and the family
fills in the rest. `PopulateGenerationFromModelUseCase` takes the family's steps and CFG
instead of 20 and 7.0 when the metadata has none. When a family is known, sampler and scheduler
come from the family, because the form cannot show them and the metadata uses A1111 names that
ComfyUI rejects (#1038 Risks). Non-turbo Krea 2 fine-tunes can need more steps than the Turbo
defaults. Their metadata, or the visible steps and CFG fields, cover that.

The mapping is a string table in the app. It does not use the `BaseModel` enum and does not
depend on #1080.

---

## 4. Generation form and model picker

**Chosen:** extend the one model picker and show the extra pickers only for a diffusion model.
**Reason:** one entry point, and no visible change on servers without diffusion models.

Shared (KMP, used by Android and iOS):

- `GenerationUiState` gains `diffusionModels`, `textEncoders`, `vaes` and `serverClipTypes`
  (S3), and `modelSource` (`CHECKPOINT` / `DIFFUSION_MODEL`), `selectedDiffusionModel`,
  `selectedFamily`, `selectedTextEncoder` and `selectedVae` (S4). `checkpoints` and
  `selectedCheckpoint` keep their meaning, so Desktop keeps working unchanged.
- A computed `canGenerate` replaces the platform checks. It is true for a custom workflow, or
  when there is a prompt and either a checkpoint is selected, or a diffusion model, a family the
  server supports, a text encoder and a VAE are all selected.
- Selection logic lives in a new `GenerationModelSelector` that updates the same state flow.
  Like `GenerationResourceLoader` and `GenerationExecutionDelegate`, it is a private delegate
  of the VM. The VM keeps its flat API with four thin forwarding functions
  (`onDiffusionModelSelected`, `onModelFamilySelected`, `onTextEncoderSelected`,
  `onVaeSelected`), so Android and iOS call it the same way as every other form action. That
  takes the VM past detekt's `TooManyFunctions` limit, so the class gets
  `@Suppress("TooManyFunctions")` with a one-line reason. Precedent:
  `ModelSearchViewModel.kt:126`. Exposing the selector as a public property was rejected,
  because call sites would then mix two calling styles.
- Picking a family applies its steps, CFG, sampler, scheduler and size. Selecting the model
  that is already selected changes nothing. Picking a different checkpoint by hand clears the
  family and resets the hidden `samplerName` / `scheduler` to `euler` / `normal`, because the
  form cannot show them and the family no longer applies. Visible fields stay as the user left
  them. A prefill that selects a checkpoint of a known family keeps that family's sampler and
  scheduler (§3).
- The default selection is unchanged: the first checkpoint. A diffusion model is selected only
  by the user or by prefill. On a server with only diffusion models nothing is preselected.

Android (A1, A2):

- `CheckpointSelector` becomes a "Model" dropdown with "Checkpoints" and "Diffusion models"
  headers. The headers appear only when the diffusion list is non-empty. Each item calls the
  handler for its own group, so the same file name in both groups stays distinct. The hardcoded
  "Select checkpoint..." moves to `strings.xml`.
- Under it, for a diffusion model: "Model family" (unsupported families disabled with
  "Needs a newer ComfyUI"), "Text encoder" and "VAE" dropdowns, in a new
  `DiffusionModelSelector.kt` to stay under detekt `LongMethod`.
- `ComfyUIGenerationScreen` hides the ControlNet and inpainting items for a diffusion model.
  `ComfyUIResultSection` uses `state.canGenerate`.
- Try in ComfyUI passes `baseModel` through `ComfyUIBridgeRoute` (A2).

iOS (I1, I2):

- `checkpointPicker` becomes a `Picker` with `Section`s. Tags are prefixed (`ckpt:` / `unet:`),
  so the same file name in both folders stays distinct. The `Binding` setter updates
  `@Published` first, then calls the KMP VM (`.claude/rules/ios.md`).
- `ComfyUIGenerationView.swift` is 13 lines under the `file_length` limit. So the model
  picker moves out of it, together with the new family, text encoder and VAE pickers, into a
  new `GenerationModelPickers.swift`, and the main file gets shorter. The new file needs its
  four `project.pbxproj` entries. `controlNetSection` and `inpaintingSection` are hidden for a
  diffusion model. The generate button uses `canGenerate` plus the existing
  `hasValidDimensions`.
- Strings go in `Localizable.xcstrings`. Try in ComfyUI passes `baseModel` into the sheet (I2).

Desktop: no change in this split. It keeps its checkpoint-only picker, which stays correct
because the default source is still a checkpoint.

---

## 5. Split into implementation issues

Rules applied: ≤5 production files per issue (tests are not counted), one outcome, one proof
command, Android and iOS in separate issues, and each issue builds and passes tests on its own
(split moves "Architecture layer" and "Vertical slice" in `.claude/skills/issue/splitting.md`).

**File these only after this spike merges. Implement each only after the open issues that
touch the same files have merged:** #1023 and #1032 (`ComfyUIApi.kt`); #1024 and #1031
(`ComfyUIGenerationRepositoryImpl.kt`); #1038 and #1041 (VM and `GenerationResourceLoader.kt`);
#1039 and #1040 (Try in ComfyUI). The #1036 follow-up that moves the generation repository to
per-call clients also rewrites `ComfyUIGenerationRepositoryImpl.kt`; whichever lands second
adapts to the other.

Proof commands come from the CI config (`.github/workflows/ci.yml`); each must print
`BUILD SUCCESSFUL`, or `** BUILD SUCCEEDED **` for `xcodebuild`. "iOS build" means the CI
command:
`xcodebuild build -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' ARCHS=arm64 ONLY_ACTIVE_ARCH=NO CODE_SIGNING_REQUIRED=NO CODE_SIGNING_ALLOWED=YES EXPANDED_CODE_SIGN_IDENTITY=""`.
Paths below are shortened. `feature-comfyui` files are under
`feature/feature-comfyui/src/commonMain/kotlin/com/riox432/civitdeck/feature/comfyui/`.

### Shared (KMP)

| # | Title | Files (expected) | Done when | Depends on |
|---|---|---|---|---|
| S6 | Make DiT-era nodes in imported workflows editable | `domain/usecase/ExtractWorkflowParametersUseCase.kt` (`PRIORITY_NODES` + `priorityOrder`: `UNETLoader` `unet_name`, `CLIPLoader` `clip_name`/`type`, `DualCLIPLoader` `clip_name1`/`clip_name2`/`type`, `VAELoader` `vae_name`, `LoraLoaderModelOnly` `lora_name`/`strength_model`, `EmptySD3LatentImage` size fields, `RandomNoise` `noise_seed`, `BasicScheduler` `steps`/`scheduler`, `KSamplerSelect` `sampler_name`, `CFGGuider` `cfg`; `parseParamDefinition` also reads V3 `["COMBO", {"options": […]}]`) | `./gradlew :feature:feature-comfyui:jvmTest detekt` → `BUILD SUCCESSFUL`. `ExtractWorkflowParametersUseCaseTest`: an API workflow shaped like the Krea 2 template yields editable `unet_name`, `clip_name`, `type`, `vae_name`, `lora_name` and size parameters; a SELECT gets its options from both the V1 `[[…]]` and the V3 `COMBO` shapes. | — |
| S7 | Reject UI-format workflow JSON on import with an API-format hint | `domain/usecase/ComfyUIUseCases.kt` (`ImportWorkflowUseCase`: a root with a `nodes` array fails with a message naming "Export Workflow (API)"; everything accepted today, including a top-level `extra` block, still passes) | `./gradlew :feature:feature-comfyui:jvmTest detekt` → `BUILD SUCCESSFUL`. `ComfyUIBridgeUseCasesTest`: a UI-format sample (`{"nodes": […], "links": […]}`) fails with that message; the existing API and APP-mode samples still pass. | — |
| S1 | Fetch DiT model file lists from ComfyUI loader nodes | `core-network/.../ComfyUIApi.kt` (`getDiffusionModels`, `getTextEncoders`, `getClipTypes`, `getVaes`; `parseNodeInputList` also reads V3 `COMBO`), `core-domain/.../ComfyUIConnection.kt` (new `DiffusionModelResources`), `core-domain/.../ComfyUIGenerationRepository.kt` (`fetchDiffusionModelResources()`), `data/repository/ComfyUIGenerationRepositoryImpl.kt`, `data/repository/ComfyUIRepositoryImpl.kt` (the unregistered legacy class also implements the interface, so it needs the override to compile; it returns empty lists) | `./gradlew :feature:feature-comfyui:jvmTest :shared:testAndroidHostTest detekt` → `BUILD SUCCESSFUL`. `ComfyUIGenerationRepositoryImplTest` (MockEngine): V1 bodies give the four lists; a V3 `COMBO` body gives the same list; `{}` for `UNETLoader` gives an empty diffusion list. The fakes in `FetchObjectInfoUseCaseTest.kt` and shared `ComfyUIUseCasesTest.kt` implement the new function. | #1023, #1032 |
| S2 | Build a split-loader workflow for diffusion-model generation params | `core-domain/.../ComfyUIConnection.kt` (`DiffusionModelSelection(unetName, textEncoderName, clipType, vaeName)`, `ComfyUIGenerationParams.diffusionModel: DiffusionModelSelection? = null`), `data/repository/ComfyUIGenerationRepositoryImpl.kt` (graph from §2; `require` that no ControlNet or mask is combined with it) | `./gradlew :feature:feature-comfyui:jvmTest detekt` → `BUILD SUCCESSFUL`. Repository tests on the submitted JSON: nodes `3`/`40`/`41` with the given names and `type`; LoRA, text encode and decode links point to them; without `diffusionModel` the JSON is unchanged from today; diffusion model + ControlNet throws. Manual: a Krea 2 Turbo and an Anima model, each with one LoRA, generate an image on ComfyUI v0.37.0. | #1024, #1031 |
| S3 | Load diffusion model lists into the generation form state | `domain/usecase/ComfyUIUseCases.kt` (`FetchDiffusionModelResourcesUseCase`), `presentation/GenerationUseCases.kt` (add it to `GenerationResourceUseCases`), `presentation/GenerationResourceLoader.kt` (`loadDiffusionModelResources`, errors ignored like LoRAs), `presentation/ComfyUIGenerationViewModel.kt` (four list fields; call from `init`), `di/ComfyUIModule.kt` (register the use case) | `./gradlew :feature:feature-comfyui:jvmTest :desktopApp:compileKotlinJvm detekt` → `BUILD SUCCESSFUL`. `ComfyUIGenerationViewModelTest`: the lists appear in state; a failing fetch leaves them empty and `error` null; checkpoint selection is unchanged. | S1, #1038, #1041 |
| S4 | Generate from a diffusion model selected in the shared generation ViewModel | `domain/model/DiffusionModelFamily.kt` (new; Krea 2 and Anima rows from §2, `isSupportedBy(serverClipTypes)`, `forBaseModel(String?)`), `presentation/GenerationModelSelector.kt` (new private delegate; source, family, text encoder and VAE selection, hint preselect, family defaults, hidden-field reset), `presentation/ComfyUIGenerationViewModel.kt` (selection fields, computed `canGenerate`, four forwarding functions, `@Suppress("TooManyFunctions")` with its reason, `buildParams` sets `diffusionModel`; `onCheckpointSelected` switches the source back) | `./gradlew :feature:feature-comfyui:jvmTest :desktopApp:compileKotlinJvm detekt` → `BUILD SUCCESSFUL`. Tests: Krea 2 is disabled without `krea2` in the server's types; picking Anima sets 30 / 4 / `euler` / `simple` / 1024 and preselects `qwen_3_06b*`; `canGenerate` is false until family, text encoder and VAE are set; generate submits `diffusionModel`; picking a different checkpoint clears the family and restores `euler` / `normal`; re-selecting the current model changes nothing. | S2, S3 |
| S5 | Preselect the diffusion model family from the CivitAI base model on prefill | `domain/usecase/ComfyUIUseCases.kt` (`PopulateGenerationFromModelUseCase(…, baseModel: String? = null)`: family steps and CFG as fallbacks; family sampler and scheduler), `presentation/ComfyUIGenerationViewModel.kt` (`applyPrefill(params, baseModel: String? = null)`; no new function), `presentation/GenerationModelSelector.kt` (set family, preselect by hint), `presentation/GenerationResourceLoader.kt` (the pending file match waits for both lists; a failed diffusion load counts as empty; rules from §3) | `./gradlew :feature:feature-comfyui:jvmTest :desktopApp:compileKotlinJvm detekt` → `BUILD SUCCESSFUL`. Tests: `Krea 2` + a file in `diffusion_models` selects it with family Krea 2 and 8 / 1; metadata steps 12 wins over 8; a `Krea 2` file only in `checkpoints` selects that checkpoint with the family's defaults; `Krea 2` + an unknown file selects nothing; `SDXL 1.0` behaves as in #1038. | S4, #1038 |
| S8 | Add Z-Image Turbo as a diffusion model family | `domain/model/DiffusionModelFamily.kt` (row + latent node + optional AuraFlow shift), `core-domain/.../ComfyUIConnection.kt` (new enum `DiffusionLatentNode` in core-domain, because the feature module depends on core-domain and not the reverse; `DiffusionModelSelection` gains `latentNode` and `auraFlowShift: Double?`), `data/repository/ComfyUIGenerationRepositoryImpl.kt` (`EmptySD3LatentImage` and `ModelSamplingAuraFlow` when set), `presentation/GenerationModelSelector.kt` | `./gradlew :feature:feature-comfyui:jvmTest detekt` → `BUILD SUCCESSFUL`. Repository test: the graph contains `ModelSamplingAuraFlow` shift 3 between the UNET and `KSampler`, and `EmptySD3LatentImage`. Manual: a Z-Image Turbo model generates on ComfyUI v0.37.0. | S4 |

### Android

| # | Title | Files (expected) | Done when | Depends on |
|---|---|---|---|---|
| A1 | Pick a diffusion model in the Android generation form | `ui/comfyui/ComfyUIParameterSection.kt` (grouped model dropdown), `ui/comfyui/DiffusionModelSelector.kt` (new; family, text encoder and VAE dropdowns), `ui/comfyui/ComfyUIGenerationScreen.kt` (hide ControlNet and inpainting), `ui/comfyui/ComfyUIResultSection.kt` (`state.canGenerate`), `res/values/strings.xml` | `./gradlew :androidApp:assembleDebug detekt` → `BUILD SUCCESSFUL`. Manual: against a server with a Krea 2 model, pick it, pick "Krea 2", and generate; against a server without `diffusion_models` files, the form looks as before. | S4 |
| A2 | Pass the CivitAI base model from Try in ComfyUI on Android | `ui/navigation/NavRoutes.kt` (`ComfyUIBridgeRoute.baseModel: String?`), `ui/navigation/LibraryNavEntries.kt` (pass `version.baseModel`), `ui/navigation/CreateNavEntries.kt` (pass it to the use case and `applyPrefill`) | `./gradlew :androidApp:assembleDebug detekt` → `BUILD SUCCESSFUL`. Manual: Try in ComfyUI on a Krea 2 model whose file is on the server opens the form with that model and family Krea 2, with steps and CFG from the image metadata (8 / 1 when it has none). | S5, #1039, A1 |

### iOS

| # | Title | Files (expected) | Done when | Depends on |
|---|---|---|---|---|
| I1 | Pick a diffusion model in the iOS generation view | `Features/ComfyUI/ComfyUIGenerationView.swift` (use the moved picker, hide sections, `canGenerate`), `Features/ComfyUI/GenerationModelPickers.swift` (new; sectioned model picker plus family, text encoder and VAE pickers), `iosApp.xcodeproj/project.pbxproj` (4 entries), `Features/ComfyUI/ComfyUIGenerationViewModel.swift` (`@Published` fields, calls to the new VM functions), `Localizable.xcstrings` | `cd iosApp && swiftlint --strict` exits 0 and iOS build → `** BUILD SUCCEEDED **`. Manual on the simulator: same checks as A1. | S4 |
| I2 | Pass the CivitAI base model from Try in ComfyUI on iOS | `Features/Detail/ModelDetailScreen.swift`, `Features/ComfyUI/ComfyUIGenerationView.swift` (init parameter), `Features/ComfyUI/ComfyUIGenerationViewModel.swift` (forward to `applyPrefill`, always passing `baseModel` explicitly rather than relying on a Kotlin default argument from Swift) | `cd iosApp && swiftlint --strict` exits 0 and iOS build → `** BUILD SUCCEEDED **`. Manual: same as A2. | S5, #1040, I1 |

Implementation order: S6 → S7 → S1 → S2 → S3 → S4 → A1 → I1 → S5 → A2 → I2 → S8.
S6 and S7 help straight away: imported DiT workflows become editable, and exports in the wrong
format are refused with a clear message. Android users can pick a diffusion model after A1,
and iOS users after I1. Try in ComfyUI opens a ready form after A2 and I2.

Sizing gate (`.claude/skills/issue/SKILL.md` Step 3), checked for every row:

1. Single outcome: each title names one behaviour, with no "and" or comma list.
2. Bounded: at most 5 production files each, all named above. S1, S3, A1 and I1 have exactly 5.
3. Single proof: each has one command taken from CI, with its success line.
4. No open decisions: §1–§4 decide the endpoints, the graph, the family table, the mapping
   rules, the state fields and the UI placement.
5. Independently mergeable: each builds on its own. S1–S5 are shared-layer steps whose effect
   is shown by tests (the #1036 precedent). S6 and S7 change imported-workflow behaviour, A1,
   A2, I1 and I2 are visible on their platform, and S8 adds a family to pickers that already
   exist.

---

## Review status

Codex cross-review was attempted on 2026-09-26 and could not run (usage limit reached).
Following the fallback in `.claude/rules/behavior.md`, every ComfyUI and CivitAI fact above was
checked against the pinned sources below, and every app fact against `37d4f306`. A four-agent
review of this document (bug and logic, architecture, KMP, source check) ran after that. Its
findings are applied: the legacy repository in S1, the private selector with forwarding
functions, the hidden-field rule, the iOS picker file, and the core-domain enum in S8. The owner
review on this PR is the remaining gate. Not run against a live server: CLIP LoRA patches on
DiT text encoders, int4/nvfp4 files, and old-server failure messages. Each is covered by a
manual check or by #1030/#1031.

## Sources

- ComfyUI v0.37.0 `nodes.py` (loaders): https://github.com/Comfy-Org/ComfyUI/blob/79be670e2d9be63e238785af307369d2b9039ed1/nodes.py#L774-L1063
- ComfyUI `server.py` (`/models`, `/view_metadata`, `/object_info`, `/templates`): https://github.com/Comfy-Org/ComfyUI/blob/79be670e2d9be63e238785af307369d2b9039ed1/server.py#L342-L354
- ComfyUI `folder_paths.py` (folders, legacy names, extensions): https://github.com/Comfy-Org/ComfyUI/blob/79be670e2d9be63e238785af307369d2b9039ed1/folder_paths.py#L10-L114
- ComfyUI `execution.py` (`validate_prompt` requires `class_type`): https://github.com/Comfy-Org/ComfyUI/blob/79be670e2d9be63e238785af307369d2b9039ed1/execution.py#L1128-L1144
- ComfyUI `comfy/sd.py` (LoRA patching, text encoder routing): https://github.com/Comfy-Org/ComfyUI/blob/79be670e2d9be63e238785af307369d2b9039ed1/comfy/sd.py#L107-L137
- ComfyUI `comfy/supported_models.py` (MiniMax H3, Anima, Krea 2): https://github.com/Comfy-Org/ComfyUI/blob/79be670e2d9be63e238785af307369d2b9039ed1/comfy/supported_models.py#L964-L990
- ComfyUI `comfy/samplers.py` (CFG 1 skips the negative; sampler names): https://github.com/Comfy-Org/ComfyUI/blob/79be670e2d9be63e238785af307369d2b9039ed1/comfy/samplers.py#L609-L611
- ComfyUI `comfy_api/latest/_io.py` (V3 combo serialisation): https://github.com/Comfy-Org/ComfyUI/blob/79be670e2d9be63e238785af307369d2b9039ed1/comfy_api/latest/_io.py#L1932-L1937
- Workflow templates at `25ff90a5` (`image_krea2_turbo_t2i.json`, `image_krea2_turbo_t2i_int8.json`, `image_anima_base_v1.json`, `image_z_image_turbo.json`, `video_minimax_h3_t2v.json`, `template_purz_wan22_animate_auto_character_replace.json`): https://github.com/Comfy-Org/workflow_templates/tree/25ff90a51cc8f6ee56bd78ec3b11f294925fb36d/templates
- ComfyUI docs, workflow API format ("Export Workflow (API)"): https://docs.comfy.org/development/api-development/workflow-api-format
- ComfyUI `execution.py` (combo values outside the list fail validation): https://github.com/Comfy-Org/ComfyUI/blob/79be670e2d9be63e238785af307369d2b9039ed1/execution.py#L1045-L1072
- CivitAI REST API reference (models endpoint): https://developer.civitai.com/site/reference/models. Data from `https://civitai.com/api/v1/models` (`sort=Most Downloaded`, `period=Month`, `limit=100`, plus `types=Checkpoint&baseModels=…`) and `https://civitai.com/api/v1/enums`, fetched 2026-09-26T07:57Z.
