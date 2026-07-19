# Roadmap

CivitDeck's development roadmap. Phases are sequential but individual items may ship out of order based on community demand.

Want to influence priorities? [Open an issue](https://github.com/rioX432/CivitDeck/issues/new/choose) or upvote existing ones.

---

## Positioning

CivitDeck is the only actively-maintained native multiplatform client for CivitAI (Android, iOS, Desktop). CivitAI-RN, the earlier React Native native client, is no longer maintained.

**Current direction — be the CivitAI app for discovery.** The wedge is helping users find the right model and prompt faster: browse, personalize, and search across sources. On-device generation is explicitly out of scope — CivitDeck is a browser and controller, not a generator.

**Distribution is FOSS-first.** Two channels ship from one codebase:
- `githubFull` — GitHub Releases sideload; bundles on-device ML (SigLIP-2 / ONNX) and in-app self-update.
- `fdroid` — F-Droid FOSS-store build; no ML assets and no self-update, to stay reproducible and store-policy clean.

Local-first design (Room KMP cache, offline browsing) hedges against CivitAI platform risk.

The core loop:
1. **Discover** — browse, search, and get a personalized feed of CivitAI models and prompts on mobile or desktop
2. **Curate** — save, collect, compare, and note what you want to use
3. **Bridge** — send models to your local ComfyUI / SD WebUI setup, or build and export a training dataset ready for kohya-ss

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
- [x] Offline browsing improvements ([#122](https://github.com/rioX432/CivitDeck/issues/122))

## Phase 2 -- Power User Expansion (Complete)

Advanced features for users who generate, not just browse.

- [x] Model version comparison -- side-by-side view ([#118](https://github.com/rioX432/CivitDeck/issues/118))
- [x] Power user mode -- toggleable advanced metadata panel
- [x] Prompt history and template system
- [x] Large screen and tablet support ([#125](https://github.com/rioX432/CivitDeck/issues/125))
- [x] Push notifications for model updates ([#120](https://github.com/rioX432/CivitDeck/issues/120)) -- shipped as Model Update Notifications (Phase 8)

## Phase 3 -- Generation Server Integration (Complete)

Bridge CivitAI browsing with your local generation setup.

- [x] ComfyUI connection management and workflow export
- [x] Home screen widget -- trending model/image of the day ([#124](https://github.com/rioX432/CivitDeck/issues/124))
- [x] Model file browser with hash verification
- [x] SD WebUI (Automatic1111/Forge) API support ([#178](https://github.com/rioX432/CivitDeck/issues/178))
- [x] Civitai Link integration -- one-tap send model to PC ([#179](https://github.com/rioX432/CivitDeck/issues/179))

**Backlog (Phase 3 scope) -- Complete:**
- [x] Live generation preview in-app -- WebSocket-driven real-time preview with interrupt support
- [x] Workflow template library -- template editor + picker, ComfyHub import, APP-mode metadata support
- [x] Remote queue management (send prompt, interrupt, monitor status) -- ComfyUI generation queue screen + WebSocket live status

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

## Phase 6 -- Training Pipeline Export (Complete)

Export datasets in formats compatible with kohya-ss / sd-scripts for handoff to a training machine. No on-device training.

Epic: [#273](https://github.com/rioX432/CivitDeck/issues/273) Dataset Export for LoRA Training *(depends on #271, #272)*

- [x] [#287](https://github.com/rioX432/CivitDeck/issues/287) KMP: `ExportRepository` + `ExportDatasetUseCase` (zip + JSONL manifest)
- [x] [#288](https://github.com/rioX432/CivitDeck/issues/288) Android: Export UI + share sheet
- [x] [#289](https://github.com/rioX432/CivitDeck/issues/289) iOS: Export UI + share sheet

**DoD**: Select dataset → one-click zip generation → share to training machine; format loadable by kohya-ss with correct `caption.txt` directory structure

---

## Phase 6.5 -- Community & Personalization (Complete)

Features shipped between Phase 6 and 7 that were not originally planned in the roadmap.

- [x] Saved search filters (PR [#359](https://github.com/rioX432/CivitDeck/pull/359))
- [x] External server integration (PR [#360](https://github.com/rioX432/CivitDeck/pull/360))
- [x] Model notes & personal tags ([#186](https://github.com/rioX432/CivitDeck/issues/186), PR [#362](https://github.com/rioX432/CivitDeck/pull/362))
- [x] QR code sharing ([#188](https://github.com/rioX432/CivitDeck/issues/188), PR [#362](https://github.com/rioX432/CivitDeck/pull/362))
- [x] Analytics dashboard ([#191](https://github.com/rioX432/CivitDeck/issues/191), PR [#362](https://github.com/rioX432/CivitDeck/pull/362))
- [x] Creator follow & feed ([#123](https://github.com/rioX432/CivitDeck/issues/123), PR [#362](https://github.com/rioX432/CivitDeck/pull/362))
- [x] Video preview support ([#187](https://github.com/rioX432/CivitDeck/issues/187), PR [#381](https://github.com/rioX432/CivitDeck/pull/381))
- [x] Background downloads (PR [#385](https://github.com/rioX432/CivitDeck/pull/385))
- [~] Reviews & ratings ([#189](https://github.com/rioX432/CivitDeck/issues/189), PR [#386](https://github.com/rioX432/CivitDeck/pull/386), [#433](https://github.com/rioX432/CivitDeck/pull/433)) — later removed ([#991](https://github.com/rioX432/CivitDeck/pull/991)): reviews are CivitAI's own territory and off the discovery wedge
- [x] Backup & restore ([#190](https://github.com/rioX432/CivitDeck/issues/190), PR [#387](https://github.com/rioX432/CivitDeck/pull/387))
- [x] Plugin system ([#409](https://github.com/rioX432/CivitDeck/issues/409)–[#414](https://github.com/rioX432/CivitDeck/issues/414), PR [#419](https://github.com/rioX432/CivitDeck/pull/419))

---

## Phase 7 -- UX Consolidation (Complete)

The app had grown to 44 Android routes / 38 iOS views / 230+ use cases. User feedback indicated feature density was becoming overwhelming. This phase simplified navigation and reduced cognitive load, on Android, iOS, and Desktop (PR [#979](https://github.com/rioX432/CivitDeck/pull/979)).

- [x] Navigation information architecture redesign — bottom nav reduced to 4 top-level destinations (Discover / Create / Library / Settings), related features grouped under each
- [x] Settings consolidation — deduplicated section headers, regrouped into Account / Browsing / Data & Storage / Advanced
- [x] Integrations hub — Create tab is now a live status hub (per-server cards, quick actions) instead of a static link list, covering ComfyUI, SD WebUI, and External Server
- [x] Progressive disclosure — e.g. ntfy push-notification setup hidden until a ComfyUI connection exists
- [x] Refactor `SettingsViewModel` into domain-scoped ViewModels — split into `AuthSettingsViewModel`, `DisplaySettingsViewModel`, `ContentFilterSettingsViewModel`, `StorageSettingsViewModel`, `AppBehaviorSettingsViewModel`, `BackupViewModel`

Also shipped in this pass: results-first search (recommendation carousels hidden while searching), working NSFW filtering (bitmask mapping fix + safest-first thumbnails + 3-level control), decision-order model detail, seamless list-to-detail image handoff, and a 54% smaller Android APK (275.6 MB → 126.7 MB) via conditional SigLIP-2/ONNX asset inclusion.

---

## Phase 8 -- Discovery Intelligence (Complete)

Go beyond CivitAI-only search. Help users find the right model faster with cross-platform search and smart recommendations.

- [x] Multi-platform model search (HuggingFace + TensorArt) — unified search with platform filter and source indicators
- [x] Unified search bar ([#988](https://github.com/rioX432/CivitDeck/pull/988)) — one Discover search field with `Idle / Editing / Results` states; the earlier separate Text Search and Similar Models entry points are retired and folded in here
- [~] Text-to-image & image similarity search (experimental, Android-only, off by default) — on-device SigLIP-2 embeddings power both natural-language ("text-to-image") and "find similar" queries. Not shipped: build-flag gated (`githubFull` only), indexes only locally-cached models, no working iOS/Desktop encoder. Corpus-index path scoped in [docs/research/989-semantic-corpus-index-spike.md](research/989-semantic-corpus-index-spike.md).
- [x] Smart feed with quality filtering (anti-Buzz-farming) — quality score based on downloads, favorites, and ratings
- [x] Model update notifications for followed models
- [x] ComfyHub workflow integration — browse and import community workflows directly into the workflow library

---

## Phase 9 -- AI-Assisted Curation (Future)

Advanced curation using Claude API integration.

- [ ] Claude-powered automatic tag generation -- image content analysis + tag suggestions
- [ ] Prompt clustering -- group similar prompts automatically
- [ ] Style grouping -- auto-classify by visual style
- [ ] Duplicate semantic detection -- hash + embedding-based deduplication
- [ ] Auto dataset suggestion -- derive training sets from collections automatically

---

## Phase 10 -- Ecosystem (In Progress)

Extend CivitDeck beyond mobile.

- [x] Desktop companion app ([#193](https://github.com/rioX432/CivitDeck/issues/193)) -- Compose Desktop app (macOS/Windows/Linux) sharing KMP business logic; all 10 sub-issues (#465-#474) closed
- [ ] Cloud sync — cross-device favorites, collections, and settings

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
Phase 6.5: (independent — community-driven features)
Phase 7:   UX Consolidation (independent — no feature deps)
Phase 8:   Discovery Intelligence (independent)
Phase 9:   AI-Assisted Curation (can start after Phase 7)
Phase 10:  Ecosystem (can start after Phase 7)
```

---

## Monetization Philosophy

CivitDeck is and will remain **free and open source**.

- Core browsing, discovery, and curation features are **never paywalled**
- Future premium features (if any) would be limited to optional extras like cloud sync or advanced workflow management
- Development is sustained through GitHub Sponsors and community contributions
