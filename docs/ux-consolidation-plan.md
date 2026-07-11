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
- Query semantics (REVISED during implementation, verified against the live API): the
  API changed under the app. Omitting the `/models` `nsfw` param now makes the server
  strip non-PG images (NSFW models arrive with empty image arrays -> broken cards), and
  omitting the `/images` `nsfw` param returns PG-only. So the params are now ALWAYS sent:
  `/models` `nsfw=false` for Off / `nsfw=true` otherwise (level narrowing stays
  client-side via `filterNsfwImages`), `/images` `nsfw=None|Soft|X` by level. Tests
  assert these mappings (SearchPageLoader, ImageGalleryViewModel, DtoMapper).
- Grid-card blur uses fixed per-level strengths (16/24 dp) rather than the gallery blur
  sliders — the sliders are documented as gallery-specific and cards must stay
  predictable; on Android < 12 (`Modifier.blur` no-op) an opaque scrim covers the
  thumbnail instead.

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

## Cross-platform parity pass (added 2026-07-11, user directive)

The user directed that every improvement must land on Android, iOS, AND Desktop — not
Android-only. Inventory of where each bet stands per platform after the first pass:

| Bet | Android | iOS | Desktop |
|---|---|---|---|
| 1 Results-first search | done | done (shared VM; carousels render from shared state) | N/A — Desktop renders no recommendation carousels |
| 2a Safest-first card thumbnail + blur/badge | done | done | done via shared `ModelCardLayout`; but `DesktopModelCard` still wraps it in a redundant first-image `NsfwBlurOverlay` (wrong image, double blur) — remove |
| 2c 3-level NSFW control | done (settings + filter sheet) | done (settings + filter sheet) | settings dropdown exists but shows raw enum names (`Off/Soft/All`); filter bar has NO NSFW control — align labels (`Safe/Moderate/Everything`) + add filter-bar chips |
| 3 Decision-order detail | done | done | InfoPanel order is header → stats → versions → tags → files → description; reorder to header → stats → tags → description → versions → files |
| 4 Create hub with live status | done | NOT done — `CreateHubView` is still a static 4-row launcher | NOT done — `DesktopCreateHubScreen` is still a static 4-row launcher |
| 4b ntfy progressive disclosure | done | NOT done — `ntfySection` renders before any server is configured | N/A (no ntfy UI on Desktop) |
| 5 Settings dedupe | done | N/A — iOS List sections have no echo | N/A — Desktop sections have no echo |
| 6 APK slimming | done | N/A (no ONNX payload in iOS bundle) | N/A |

Additional Desktop-only bug found during the inventory: `DesktopSearchScreen` applies a
local NSFW filter that is INVERTED — at level `All` (Everything) it removes NSFW models,
at `Off` (Safe) it shows everything. The shared VM already handles filtering (server
param + client narrowing), so the local filter is deleted rather than fixed.

### Bet 7 — Seamless list→detail image handoff (all 3 platforms; user's top ask)

Navigating from a model card to the detail screen re-loads the hero image at a different
URL/size, so the shared-element-like transition lands on a shimmer and the flow visibly
breaks. Fix per platform, using each image loader's official low-res-first mechanism:

- **Android**: grid cards keep loading `image.thumbnailUrl()` (450-wide variant). The
  detail carousel loads full-res `image.url` — set Coil's `placeholderMemoryCacheKey`
  to the image's `thumbnailUrl()` so the already-cached grid thumbnail paints instantly
  under the shared-element animation while full-res loads, then crossfades. This is
  Coil's documented recipe for exactly this transition (docs/recipes: "Using a Memory
  Cache Key as a Placeholder"). `SubcomposeAsyncImage`'s loading slot must render
  `SubcomposeAsyncImageContent()` when a placeholder painter exists (shimmer only when
  there is none), otherwise the placeholder never shows.
- **Desktop**: same Coil recipe in `DesktopImageViewer` (full-res view); the detail
  `ImageGridPanel` already reuses `thumbnailUrl()` so it hits the memory cache as-is.
- **iOS**: `CachedAsyncImage` gets an optional `placeholderURL`. Before fetching the
  main URL it decodes the placeholder from `URLCache` if present (no extra network) and
  shows it as an instant low-res phase, then swaps when the full image arrives. The
  detail carousel passes `image.thumbnailUrl(width: 450)` — the exact URL the card
  cached. The carousel also gets `maxPixelSize: 1200` (documented DesignSystem rule for
  detail views; it currently decodes at the 400 default).

Verification: Android emulator drive-through (this is the flow the user called out),
Desktop `:desktopApp:run` visual check, iOS compile-level only (runtime UNVERIFIED,
listed in the PR).
