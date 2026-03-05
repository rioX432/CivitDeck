# Roadmap

CivitDeck's development roadmap. Phases are sequential but individual items may ship out of order based on community demand.

Want to influence priorities? [Open an issue](https://github.com/rioX432/CivitDeck/issues/new/choose) or upvote existing ones.

---

## Positioning

CivitDeck is the only native iOS/Android client for CivitAI.
Focus: **Browse → Curate → Generate-ready** (not an on-device generator).
Local-first design hedges against CivitAI platform risk.

The core loop:
1. **Browse** CivitAI models and images on mobile
2. **Curate** — save, collect, and compare what you want to use
3. **Prepare** — build a training dataset and export it, ready for kohya-ss

---

## Phase 1 -- Core (Complete)

The foundation: browse, search, and save CivitAI models from your phone.

- [x] KMP project setup (Android + iOS + Shared)
- [x] CivitAI API client (Ktor)
- [x] Model search with filters (type, sort, period, tags)
- [x] Model detail screen with version info
- [x] Image gallery with staggered grid and full-screen viewer
- [x] Prompt metadata viewer with one-tap copy
- [x] Local favorites and offline cache (Room KMP)
- [x] iOS feature parity (SwiftUI)
- [x] Fresh Find -- discover recently published models
- [x] CI/CD pipeline (GitHub Actions)

## Phase 1.5 -- Quick Wins (Complete)

Quality-of-life improvements and account features.

- [x] CivitAI API key authentication ([#119](https://github.com/rioX432/CivitDeck/issues/119))
- [x] Tag-based exploration
- [x] Enhanced collection management ([#121](https://github.com/rioX432/CivitDeck/issues/121))
- [x] Creator profiles
- [ ] Offline browsing improvements ([#122](https://github.com/rioX432/CivitDeck/issues/122))

## Phase 2 -- Power User Expansion (Complete)

Advanced features for users who generate, not just browse.

- [x] Model version comparison -- side-by-side view ([#118](https://github.com/rioX432/CivitDeck/issues/118))
- [x] Power user mode -- toggleable advanced metadata panel
- [x] Prompt history and template system
- [x] Large screen and tablet support ([#125](https://github.com/rioX432/CivitDeck/issues/125))
- [ ] Push notifications for model updates ([#120](https://github.com/rioX432/CivitDeck/issues/120)) -- moved to Phase 5 backlog

## Phase 3 -- Generation Server Integration (Complete)

Bridge CivitAI browsing with your local generation setup.

- [x] ComfyUI connection management and workflow export
- [x] Home screen widget -- trending model/image of the day ([#124](https://github.com/rioX432/CivitDeck/issues/124))
- [x] Model file browser with hash verification
- [x] SD WebUI (Automatic1111/Forge) API support ([#178](https://github.com/rioX432/CivitDeck/issues/178))
- [x] Civitai Link integration -- one-tap send model to PC ([#179](https://github.com/rioX432/CivitDeck/issues/179))

**Backlog (Phase 3 scope, not yet scheduled):**
- [ ] Live generation preview in-app
- [ ] Workflow template library
- [ ] Remote queue management (send prompt, interrupt, monitor status) -- see note below

> **Note — Remote Execution Epic**: If the goal extends to fully operating ComfyUI from mobile (submit prompt, queue/interrupt, real-time progress), that warrants a dedicated epic covering: authenticated connection (Tailscale / reverse proxy), Prompt API, Queue API, and `/ws` live status. This is not yet scoped as a separate issue but is the natural next step after Phase 4.

---

## Phase 4 -- ComfyUI Output Gallery (Complete)

Retrieve and browse locally generated ComfyUI output images from mobile. Bridge generation results into collections and the dataset pipeline.

Epic: [#270](https://github.com/rioX432/CivitDeck/issues/270) ComfyUI Output Gallery Integration

- [x] [#277](https://github.com/rioX432/CivitDeck/issues/277) KMP: `/history` API client + `FetchComfyUIHistoryUseCase`
- [x] [#278](https://github.com/rioX432/CivitDeck/issues/278) Android: Output gallery screen + generation metadata detail
- [x] [#279](https://github.com/rioX432/CivitDeck/issues/279) iOS: Output gallery screen + generation metadata detail

**DoD**: `/history` image list retrieval → full metadata detail (prompt, seed, sampler, LoRA, CFG) → save to favorites / collections

---

## Phase 5 -- Dataset Collection & Curation (Complete)

Enable LoRA training dataset collection, organization, and quality control directly from mobile.

### 5a. Core Dataset Structure

Epic: [#271](https://github.com/rioX432/CivitDeck/issues/271) Dataset Collection System (Core)

- [x] [#280](https://github.com/rioX432/CivitDeck/issues/280) KMP: Domain models (`DatasetCollection`, `DatasetImage`, `ImageSource`)
- [x] [#281](https://github.com/rioX432/CivitDeck/issues/281) KMP: Room entities + DAOs + migration + Repository + UseCases
- [x] [#282](https://github.com/rioX432/CivitDeck/issues/282) Android: Dataset list screen (CRUD)
- [x] [#283](https://github.com/rioX432/CivitDeck/issues/283) iOS: Dataset list screen (CRUD)

### 5b. Caption & Tag Editing

Epic: [#272](https://github.com/rioX432/CivitDeck/issues/272) Caption & Tag Batch Editor

- [x] [#284](https://github.com/rioX432/CivitDeck/issues/284) KMP: Tag/Caption domain + `BatchEditTagsUseCase` + `EditCaptionUseCase`
- [x] [#285](https://github.com/rioX432/CivitDeck/issues/285) Android: Caption editor + batch tag editor
- [x] [#286](https://github.com/rioX432/CivitDeck/issues/286) iOS: Caption editor + batch tag editor

### 5c. Generated Image → Dataset Pipeline

Epic: [#274](https://github.com/rioX432/CivitDeck/issues/274) Generated Image → Dataset Pipeline *(depends on #270, #271)*

- [x] [#290](https://github.com/rioX432/CivitDeck/issues/290) Android: One-tap add to dataset from ComfyUI / SD WebUI gallery
- [x] [#291](https://github.com/rioX432/CivitDeck/issues/291) iOS: One-tap add to dataset from ComfyUI / SD WebUI gallery

### 5d. License & Source Tracking

Epic: [#275](https://github.com/rioX432/CivitDeck/issues/275) License & Source Tracking System *(depends on #271)*

- [x] [#292](https://github.com/rioX432/CivitDeck/issues/292) KMP: `ImageSource` domain extension + `trainable` flag + export warning logic
- [x] [#293](https://github.com/rioX432/CivitDeck/issues/293) Android + iOS: Source badge + trainable toggle UI

### 5e. Duplicate & Quality Filtering

Epic: [#276](https://github.com/rioX432/CivitDeck/issues/276) Duplicate & Quality Filtering *(depends on #271)*

- [x] [#294](https://github.com/rioX432/CivitDeck/issues/294) KMP: pHash duplicate detection + resolution filter UseCase
- [x] [#295](https://github.com/rioX432/CivitDeck/issues/295) Android: Duplicate review UI + resolution filter settings
- [x] [#296](https://github.com/rioX432/CivitDeck/issues/296) iOS: Duplicate review UI + resolution filter settings

**DoD**: Group images into a dataset → edit captions and tags → flag non-trainable content → filter duplicates and low-quality images

---

## Phase 6 -- Training Pipeline Export (Next)

Export datasets in formats compatible with kohya-ss / sd-scripts for handoff to a training machine. No on-device training.

Epic: [#273](https://github.com/rioX432/CivitDeck/issues/273) Dataset Export for LoRA Training *(depends on #271, #272)*

- [ ] [#287](https://github.com/rioX432/CivitDeck/issues/287) KMP: `ExportRepository` + `ExportDatasetUseCase` (zip + JSONL manifest)
- [ ] [#288](https://github.com/rioX432/CivitDeck/issues/288) Android: Export UI + share sheet
- [ ] [#289](https://github.com/rioX432/CivitDeck/issues/289) iOS: Export UI + share sheet

**DoD**: Select dataset → one-click zip generation → share to training machine; format loadable by kohya-ss with correct `caption.txt` directory structure

---

## Phase 7 -- AI-Assisted Curation (Future)

Advanced curation using Claude API integration.

- [ ] Claude-powered automatic tag generation -- image content analysis + tag suggestions
- [ ] Prompt clustering -- group similar prompts automatically
- [ ] Style grouping -- auto-classify by visual style
- [ ] Duplicate semantic detection -- hash + embedding-based deduplication
- [ ] Auto dataset suggestion -- derive training sets from collections automatically

---

## Dependency Graph

```
Phase 4: #270
           │
Phase 5:  #271 ──── #272 ──── #275
           │                   │
           └──── #274          └──── #276
                 (also needs #270)
Phase 6:         #273
                 (needs #271, #272)
```

---

## Monetization Philosophy

CivitDeck is and will remain **free and open source**.

- Core browsing and discovery features are **never paywalled**
- Future premium features (if any) would be limited to optional extras like cloud sync or advanced workflow management
- Development is sustained through GitHub Sponsors and community contributions
