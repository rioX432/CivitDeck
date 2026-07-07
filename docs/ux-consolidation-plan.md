# UX Consolidation Plan (Phase 7)

Author: Claude (autonomous run, 2026-07-07). Branch: `fable5/ux-consolidation`.

This plan is grounded in (a) driving the debug build on an Android emulator (Pixel-class,
API 36) through the search, gallery, NSFW, collections, ComfyUI, and settings flows —
screenshots captured during the session; (b) community research with sources (CivitAI
GitHub discussions/issues, competitor app reviews); (c) reading the code paths behind each
observed problem.

## The target user loop

CivitAI mobile users overwhelmingly use the phone as a **browse + curate + remote-control**
surface, not a generation device (multiple Japanese guide articles independently describe
"browse and favorite on the phone, generate on the PC" as the de facto pattern; ComfyUI
mobile clients converge on "hide the node graph, show a form + queue"):

1. **Browse fast** — find models/images during downtime.
2. **Filter NSFW reliably** — trust what is (and is not) on screen.
3. **Curate** — favorites/collections, offline-capable.
4. **Hand off** — send the model/prompt to a PC (download, ComfyUI queue), get notified.

Everything on the phone screen must serve one of these within one step. CivitDeck already
implements all four loops; the problem is noise and reliability, not missing features.

## What I saw on the emulator (pain points, with evidence)

P1. **Search results are buried under recommendation carousels.** With the query
    `illustrious` active, results appeared ~2.5 screens below five stacked horizontal
    carousels ("Recommended for You", "Trending Checkpoint", "Popular in photorealistic",
    "Popular in anatomical", "Rising Fast"), with the same model (RedCraft) appearing in
    three of them. Code: `ModelGrid` renders `recommendationItems()` unconditionally above
    results; the VM never clears/hides recommendations while a query or filter is active.
    Screenshots: `04_carousel_overload.png`, `05_search_results_below_carousels.png`.

P2. **NSFW handling on the model grid is not trustworthy.**
    - Model cards select `modelVersions.first().images.first()` as thumbnail with no
      content-type or NSFW-level check (`ModelGridSection.kt`). NSFW models whose first
      preview is a video (common) render as a broken-image icon — seen on "One obsession",
      "Babes", "Illustrious Style Pack" (screenshot `03_discover_nsfw_broken_thumbs.png`).
    - `ModelCard`/`SwipeableModelCard` apply **no blur at all** — the per-level blur
      settings only affect the image gallery. Community evidence says per-image (not
      per-model) handling is what users want (civitai discussion #266), plus a visible
      18+ badge (discussion #244, implemented by CivitAI itself).
    - The 3-level `NsfwFilterLevel` (Off/Soft/All) is exposed in Settings as a **binary
      switch** that toggles Off<->All (`NsfwToggleRow`) — `Soft` is unreachable on Android.
    - Changing the browsing level requires Settings > Content & Behavior (4+ taps from the
      grid); community asks for quick level switching near the browse surface
      (discussion #582). The filter sheet has Source/Type/Base Model/Sort/Period/tags but
      no NSFW level.

P3. **Model detail reading order fights the browse loop.** Empty "My Notes", "My Tags",
    and "Reviews (0) — No reviews yet" sections sit above Description and Versions/Files.
    For deciding "do I want this model", Description/trained words/files matter; personal
    annotations are secondary and reviews are a local-only feature with no content on a
    fresh install. Additionally the Versions area shows a large blank region (~1/3 screen)
    between the version chips and "Base Model", and a nested scrollable there swallows
    scroll gestures (screenshot `02_detail_versions_gap.png`). `FilterChipRow` is also a
    non-scrolling `Row`, so >3 version chips clip off-screen.

P4. **The Create tab is a dead launcher.** Four static rows (ComfyUI / SD WebUI / External
    Server / Model File Browser), no connection status, no queue/last-generation info —
    90% empty screen. Pocket Comfy's home screen leads with per-service up/down status;
    Comfy Portal leads with a server dashboard. Also, ComfyUI settings surface ntfy push
    configuration copy before any server is configured (progressive-disclosure violation).

P5. **Settings list doubles its own length.** Every row repeats its section header
    ("Appearance" header + "Appearance" row, etc.), so the top-level list is ~2x taller
    than needed and reads as noise.

P6. **The APK ships 165 MB of dead weight.** `FEATURE_SIMILARITY_SEARCH = false`, yet the
    debug APK (275 MB) contains `assets/ml/siglip2_vision_q4f16.onnx` (54 MB) and
    libonnxruntime.so for 4 ABIs (~112 MB). For GitHub-Releases-only distribution
    (no store splits) this directly hurts install trust and speed.

Minor (not fixed in this pass, recorded for later): first-run gesture tutorial appears
before any content; speed-dial FAB labels are illegible over images; sort change wipes the
gallery grid with a full-screen spinner; ~40px of extra dead space above screen titles.

## Bets (in execution order)

Each bet is DONE only when the full Android CI gate is green
(`:shared:testAndroidHostTest :core:core-database:jvmTest :core:core-data:testAndroidHostTest
:feature:feature-{search,detail,gallery,prompts,creator,externalserver}:testAndroidHostTest
:feature:feature-comfyui:jvmTest detekt :androidApp:assembleDebug`) plus the noted extra
module tests, plus an emulator smoke check of the changed flow.

### Bet 1 — Search results first
Hide recommendation carousels whenever a query or non-default filter is active, and cap
visible recommendation sections at 2 when browsing idle.
- Touches: `feature-search` `ModelSearchViewModel` (shared VM — all 3 platforms),
  `ModelSearchViewModelTest`.
- Extra gate: none beyond standard (feature-search tests cover it).
- iOS/Desktop: same VM feeds them; behavior change is intended and identical. iOS runtime
  UNVERIFIED — listed in PR.

### Bet 2 — NSFW you can trust on the grid
(a) Domain: `Model.browseThumbnail(filterLevel)` — first image with
`contentType == IMAGE` and allowed NSFW level; fallback to the least-NSFW image; never a
video. Unit tests next to `ModelImageUrlTest`.
(b) Android grid cards: blur the thumbnail according to the existing per-level
`NsfwBlurSettings` when its own `nsfwLevel` > None (reuses `NsfwBlurOverlay` from core-ui)
and show a small 18+ level badge.
(c) Make `Soft` reachable: replace the binary settings switch with a 3-option segmented
row (Safe / Moderate / Everything), and add the same 3-option row to the search filter
sheet, writing the same `ContentFilterPreferencesRepository` value (single source of
truth).
- Touches: `core-domain` (helper + tests), `androidApp` ModelCard/SwipeableModelCard/
  ModelGridItem/FilterSheetSection/ContentFilterComponents, `feature-search` VM only if
  wiring requires (it already exposes `nsfwFilterLevel` + setter).
- Extra gate: `:core:core-domain:testAndroidHostTest` locally; add core-domain tests to CI
  (they are currently missing from the CI task list — coverage gap).
- iOS: iOS cards/settings keep current behavior (binary toggle stays functional since the
  repository enum is unchanged); parity work listed in PR as follow-up. UNVERIFIED.
- Query semantics guard: `nsfw=false` when Off, `null` otherwise — unchanged; tests assert
  this stays true (SearchPageLoader tests already cover it; extend for level mapping).

### Bet 3 — Model detail reads in decision order
Reorder detail items to: media -> header/stats/actions -> tags -> Description ->
Versions + Files -> My Notes -> My Tags -> Reviews. Collapse empty Notes/Tags/Reviews to
single compact rows. Fix the Versions dead-space/nested-scroll bug and make
`FilterChipRow` horizontally scrollable.
- Touches: `androidApp` ModelDetailContent/VersionSection, `core-ui` FilterChipRow
  (shared with Desktop — compile-check Desktop).
- Extra gate: `:desktopApp:compileKotlinJvm` (core-ui changed).
- iOS: separate SwiftUI detail view unchanged; noted in PR.

### Bet 4 — Create tab becomes a remote-control hub
Replace the static launcher with status cards: each configured integration shows a
connection status (reusing existing connection state/test-connection use cases) and its
primary actions (Generate / Queue / Output History for ComfyUI). Nothing configured =>
single "Connect a server" CTA into the existing guided setup. Hide ntfy setup copy until
a server is configured.
- Touches: `androidApp` CreateHubScreen (+ strings), reads existing `feature-comfyui` /
  `feature-externalserver` VMs; no shared VM signature changes.
- Extra gate: `:feature:feature-comfyui:jvmTest` (already in gate) — no shared changes
  expected; if a shared VM needs a new field, its tests are updated in the same commit.
- iOS/Desktop: unchanged; parity follow-up in PR.

### Bet 5 — Settings without the echo
Remove duplicated per-row section headers on the top-level settings screen; group rows
under 4 headers: Account / Appearance & Content / Data & History / Advanced. No route
changes, no sub-screen changes, no ViewModel changes.
- Touches: `androidApp` SettingsScreen only.
- iOS/Desktop: unchanged.

### Bet 6 — Ship an APK that respects the user's disk (attempted last)
When `FEATURE_SIMILARITY_SEARCH` is off: exclude the SigLIP asset and onnxruntime native
libs from the APK. Must first verify the background embedding indexer and any ONNX class
use is already gated by the flag (if not, gate it). Success = assembleDebug drops from
~275 MB to ~110 MB and the app runs clean on the emulator with no ONNX crash.
- Touches: `androidApp/build.gradle.kts` (packaging/asset exclusion), possibly the flag
  wiring around the embedding indexer.
- Risk: highest of the six — if gating turns out to leak ONNX calls at runtime, I stop,
  keep the dependency, and only exclude the 54 MB asset (or drop the bet and record why).

## KEEP / HIDE / REMOVE

No feature is hard-deleted in this pass. There is no usage data; all reductions are
reversible presentation changes.

- KEEP (untouched): collections/datasets/prompts, downloads queue, backup/restore,
  Civitai Link, workflow templates, ComfyHub, QR sharing, analytics, feed, multi-source
  search, plugin system, video preview, browsing history, hidden models.
- HIDE / DEMOTE: recommendation carousels while searching (Bet 1); empty
  Notes/Tags/Reviews sections demoted to bottom + compact (Bet 3); ntfy setup until a
  server exists (Bet 4); redundant settings headers (Bet 5); ML payload for a
  feature-flagged-off capability (Bet 6).
- REMOVE: nothing. (Candidates like the local-only Reviews feature need the user's call;
  recorded in the PR as discussion items, not acted on.)

## Protected contracts respected

- No Room schema/migration changes, no backup/export DTO changes, no NavRoutes
  added/removed, no Koin module signature changes.
- Shared VM changes are limited to `ModelSearchViewModel` recommendation gating (Bet 1)
  and are behavior all three platforms should share; every consumer compiles against the
  unchanged API surface.
- iOS is compile-checked at the KMP boundary only (`linkDebugFrameworkIosSimulatorArm64`);
  all iOS-facing behavior changes are listed as UNVERIFIED in the PR.

## Verification tiers

- Per WIP commit: compile of changed modules + changed-module unit tests + detekt.
- Per bet + before PR: the full Android CI gate above, plus per-bet extra gates, plus
  emulator drive-through of the changed flow with screenshots (smoke check only, not
  regression proof).
