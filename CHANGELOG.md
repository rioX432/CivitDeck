# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

Discovery-engine refocus: sharpen the app around native CivitAI discovery and open the FOSS distribution channel.

### Added

- Personalization signals — recommendation-card taps are recorded via an append-only interaction event log (retained even without a prior history row, accumulating instead of overwriting), and recommendations refresh on return from a detail view (#987, #994)
- Unified search bar — keyword/tag, image, and experimental semantic search fold into one `ModelSearchViewModel` with explicit `Idle`/`Editing`/`Results` states; recommendations show only while idle (#988, #997)
- Build flavors — `fdroid` (FOSS, no ML/self-update) and `githubFull` (bundled ML + in-app self-update), with a dedicated `core-ml` module extracted from `core-domain` so the F-Droid build excludes ONNX at the dependency level (#986, #993)
- Deterministic QA foundation for the discovery flow — base-URL-injectable E2E seam, recorded CivitAI fixtures, stable discovery test tags, a semantic golden-query top-K test, and a Desktop Compose smoke test (PoC, not CI-gated) (#990, #998)
- Semantic corpus-index spike decision doc and a `SigLipTokenizer` HF-parity test (#989, #996)
- `ComfyUIGenerationViewModel.applyPrefill` fills the generation form from `ComfyUIGenerationParams`, selecting the requested checkpoint by file name even when the server list loads afterwards; sampler and scheduler are intentionally not applied (#1038)
- `ComfyUIGenerationViewModel.onTemplateApplied` applies a workflow template: a template imported from a raw ComfyUI workflow loads as the custom workflow, and any other template clears a loaded custom workflow and fills the form from its variables (#1041)
- `ComfyUIHistoryViewModel.loadOlder()` re-fetches history with a window one page (200 entries) wider, exposing `canLoadOlder`/`isLoadingOlder`; `refresh()` keeps the widened window and a new fetch cancels the one in flight. No UI button yet (#1053)
- Android ComfyUI Outputs list shows a "Load older outputs" button at the older end of the list (bottom for Newest, top for Oldest) when more history may exist; it calls `loadOlder()` and shows a spinner while loading (#1054)
- `ComfyUIServerTrust` and `createComfyUIHttpClient(trust, timeoutConfig)` — Android/JVM ComfyUI clients can accept only the server whose leaf certificate matches a stored SHA-256 pin and record the fingerprint the server presented; iOS keeps system trust for now (#1118)
- ComfyUI onboarding captures the certificate of a self-signed HTTPS server: the test fails with `CertificateUnconfirmed` (no pin yet) or `CertificateChanged` (different certificate) and the presented SHA-256 fingerprint, and `ConnectionOnboardingViewModel.onTrustCertificate()` tests again with that pin and saves the connection with it; `onReviewCertificate(saved)` re-tests a saved connection. The tester uses a one-off pinned client per test instead of the trust-all client (#1119)
- Android ComfyUI onboarding shows the server certificate's SHA-256 fingerprint in the `openssl x509 -noout -fingerprint -sha256` format, with a hint to compare it on the server and a "Trust this certificate" button, when the test fails because the certificate is not confirmed yet or differs from the trusted one (#1120)
- Android ComfyUI settings show a "Review certificate" button next to Test when the active connection uses HTTPS with self-signed certificates accepted; it opens onboarding on the certificate review for that connection, so a regenerated server certificate can be re-confirmed without re-adding the connection (#1127)
- iOS ComfyUI Outputs list shows a "Load older outputs" button at the older end of the list (below the grid for Newest, above it for Oldest) when more history may exist beyond the loaded window, with a spinner in its place while loading (#1055)
- iOS ComfyUI clients built with `ComfyUIServerTrust.PinnedLeaf` accept only the server whose leaf certificate matches the SHA-256 pin, for HTTP and WebSocket, and record the presented fingerprint; any other certificate, or no pin, cancels the handshake instead of falling back to system trust. The public `ComfyUIServerTrustEvaluator` makes the same decision for Swift `URLSession` delegates (#1128)
- iOS ComfyUI onboarding shows the server certificate's SHA-256 fingerprint in the `openssl x509 -noout -fingerprint -sha256` format, with a hint to compare it on the server and a "Trust this certificate" button that saves the connection with that pin, when the test fails because the certificate is not confirmed yet or differs from the trusted one (#1129)
- iOS loads ComfyUI images (history, generation, output detail and its zoom viewer, mask editor) and "save to Photos" from a server with a confirmed certificate pin: `ImageURLSession` answers server-trust challenges for a pinned host:port with `ComfyUIServerTrustEvaluator` (via `KoinHelper.getComfyUIImageTrustEvaluator(host:port:)`) and leaves every other host, CivitAI included, on system trust. Confirming, changing or clearing a pin clears the image cache and resets the session's connections, because `URLCache` cannot remove entries by host (#1130)
- iOS ComfyUI settings show a "Review certificate" row for an active HTTPS connection that accepts self-signed certificates (with or without a stored pin). It opens onboarding directly on a re-test of that saved connection via `onReviewCertificate`, so a regenerated server certificate shows its new fingerprint and can be trusted without re-adding the connection (#1131)
- Android loads ComfyUI images (history, generation result, output detail, mask editor, image viewer) from a server with a confirmed certificate pin: `ComfyUIPinnedFetcherFactory` fetches `https` URLs on a pinned host:port with an OkHttp client that trusts only that certificate (`OkHttpClient.Builder.trustOnlyPinnedLeaf`) and leaves every other URL, CivitAI included, to Coil's default fetcher. Disk-cache entries for those images are keyed by pin, and `ComfyUIImageCacheEvictor` removes a host:port's memory-cache entries when its pin is confirmed, changed or cleared (#1126)

### Changed

- SigLIP-2 semantic search relabeled as **Android-only, experimental, off by default** and kept off the default retrieval path (keyword/tag stays primary); iOS/Desktop have no working encoder (#989, #996)
- "Send to PC" (CV2) demoted to show only with a compatible connection; detail-screen Share/QR moved into an overflow menu; Analytics/Backup/Compare/Dataset/Hardware peripherals moved off the discovery-primary surface (kept, reversible) (#991, #1000)
- Docs & positioning — README now leads with the single discovery promise, the ViewModel count is unified to the measured 42, and ROADMAP is reframed as a forward-looking compass (#992, #1001)
- ComfyUI Outputs list requests only the newest 200 history entries (`GET /history?max_items=200`, the same page size as ComfyUI's own frontend) instead of the server's entire history; older outputs are not reachable until history paging lands (#1051)
- `FetchComfyUIHistoryUseCase(maxItems)` emits a `ComfyUIHistoryPage(images, hasMore)`; `hasMore` is true when `/history` returned the full `maxItems` entries (counting history entries, including failed prompts without images, not images). No user-visible change (#1052)
- ComfyUI connection test reports `LoopbackHost` when a test against `localhost`, `127.x.x.x` or `0.0.0.0` fails without an HTTP response (unreachable or timed out), so the UI can explain that the address points at this device rather than the PC; HTTP, auth, non-ComfyUI and TLS failures keep their cause (#1061)
- ComfyUI generation (HTTP and foreground WebSocket progress) and saving a generated image resolve the server URL and TLS trust per call through the new `ComfyUIApiProvider`: a connection with HTTPS and "Accept self-signed certificates" uses a client pinned to its confirmed certificate, every other connection keeps system trust. The provider also keeps a host:port → pin snapshot for image loaders. Queue, history, ComfyHub import and the settings test still use the shared `ComfyUIApi` until #1122/#1123 (#1121)
- The Android background generation monitor opens its WebSocket through `ComfyUIApiProvider.forUrl(baseUrl)` instead of the shared `ComfyUIWebSocketApi`, so its progress notification uses the same TLS trust as the foreground progress and advances against a server with a pinned self-signed certificate (#1124)
- ComfyUI connection test reports `Refused` instead of `Unreachable` when nothing accepts the connection at the host and port (typically ComfyUI started without `--listen`): a `Connection refused` message anywhere in the error's cause chain (JVM, OkHttp) or an iOS `NSURLErrorDomain` `Code=-1004` error; DNS and no-route failures stay `Unreachable`, and `LoopbackHost` still wins for loopback hosts (#1062)
- ComfyUI queue polling and cancel, history (including its `/view` image URLs) and ComfyHub "import to server" resolve the server URL and TLS trust per call through `ComfyUIApiProvider.forActive()`, so they reach the active connection with its pinned certificate; queue polling re-resolves the active connection on every poll. The settings test and system stats still use the shared `ComfyUIApi` until #1123 (#1122)
- ComfyUI settings "Test" (and the workflow plugin's `connect()`) and system stats resolve the server URL and TLS trust per call through `ComfyUIApiProvider`: the test and the stats shown after it use the tested connection's URL and pinned certificate, so a connection whose self-signed certificate was trusted no longer shows "Connection Error"; `FetchSystemStatsUseCase()` (model detail VRAM fit) uses the active connection and returns null when none is active (#1123)
- Importing a ComfyUI workflow saved with ComfyUI's default Save (UI format, a top-level `nodes` array) now fails at import with a hint to use "Export Workflow (API)", instead of being accepted and failing at generation with a `class_type` error that does not name the wrong format (#1143)
- Android ComfyUI onboarding failure step shows the fix for the detected cause instead of a generic "check the host": connection refused → start ComfyUI with `--listen` and check the port (8188, or 8000 on older ComfyUI Desktop); timeout → same network and firewall; HTTP 401/403 → servers behind a login are not supported yet; non-ComfyUI response → check the port; localhost/127.0.0.1/0.0.0.0 → enter the PC's local IP (#1064)

### Removed

- Reviews list/submit feature (reads/writes CivitAI's tRPC, conflicts with the "no SNS/community" scope) — `Model.stats.rating` is retained and still shown; the 2 tRPC review requests per model detail are eliminated (#991, #1000)
- The trust-all ComfyUI HTTP client (`createComfyUIHttpClientWithSelfSignedTls`, `createPlatformComfyUIHttpClient` and their Koin-named client), which accepted every server certificate and, on Android, every hostname. A self-signed server is reached only through a client pinned to its confirmed certificate (#1132)
- The shared `ComfyUIApi` and `ComfyUIWebSocketApi` Koin singletons — ComfyUI calls build their API per call (`ComfyUIApiProvider`, or a transient instance for the connection test and the LAN scan), so no code can reach the server through a shared instance that ignores the connection's TLS trust. The `named("comfyui")` HTTP client stays for Civitai Link, the LAN scan and ntfy (#1125)

### Fixed

- ComfyUI generation shows why the server rejected a workflow — a non-2xx `/prompt` response throws `ComfyUIResponseException` whose message is ComfyUI's `error.message: error.details` (for example `Prompt outputs failed validation: Value -1 smaller than min of 0: seed`), an older server's plain-string `error`, or `HTTP <status>` for a non-JSON body, instead of a JSON-decoding error about the missing `prompt_id` (#1030)
- Every other ComfyUI request (`/queue`, `/object_info`, `/history`, `/interrupt`, `/upload/image`, `/system_stats`) throws `ComfyUIResponseException` on a non-2xx status, so a 401 from an auth proxy or a 500 no longer shows up as an empty queue or model list, and an interrupt or cancel the server rejected no longer reports success (#1032)
- A ComfyUI job that fails on the server is reported as a generation error right away with ComfyUI's `exception_message` (for example `CUDA out of memory`) from the `/history` entry's `execution_error`, instead of polling until "Generation timed out"; an interrupted job reports an error with no message, so the screen shows "Generation failed" (#1031)
- Android and Desktop browse cards (Discover grid, swipe cards, recommendation rows, creator page) show a still frame for models whose latest version has only CivitAI-hosted video previews, instead of an empty thumbnail; a resized thumbnail URL now sets its width inside an existing comma-separated CDN transform instead of adding a second segment, which made the CDN return the video (#1075)
- Android and Desktop show a plain-HTTP ComfyUI connection to a Tailscale address (`100.64.0.0/10` or a `*.ts.net` MagicDNS name) as LAN instead of "HTTP over internet", since tailnet traffic is WireGuard-encrypted (#1066)
- iOS ComfyUI settings security badge uses the shared security level, matching Android and Desktop: a Tailscale host shows "LAN", and public `172.x` addresses outside `172.16.0.0/12` (such as `172.217.x.x`) show the "HTTP" warning instead of "LAN" (#1067)
- iOS ComfyUI connection test reports a self-signed server's certificate as not confirmed yet or changed, with its fingerprint, instead of "unreachable": the pinned client's rejection surfaces on iOS as a cancelled request, so the test now also treats a presented certificate that differs from the pin as a certificate failure (#1203)
- iOS no longer crashes when opening the Create tab — the shared logger passed a Kotlin string to `NSLog` as a `%@` argument, which faulted with `EXC_BAD_ACCESS`; it now logs the whole line as the format with `%` escaped (#1204)
- iOS declares `NSLocalNetworkUsageDescription`, so reaching a LAN ComfyUI or SD WebUI server triggers the system Local Network prompt with CivitDeck's reason text (#1056)
- Android ComfyUI output detail opens on the tapped image with exactly the Outputs list's images in the list's sort order — the list hands its snapshot to the detail instead of the detail re-fetching history with the default Newest order, so it no longer shows a blank screen when that fetch fails or is empty (#1050)
- iOS generation screen's template picker now lists saved templates and applies the picked one to the form; its category and type filter chips now filter the list (#1043)
- Swipe discovery's right swipe (heart) now only adds to Favorites: a model that is already favorited stays favorited, and Undo removes a favorite only when that swipe added it, including an Undo pressed before the favorite check finishes (#1074)
- Restoring a backup with "Merge" no longer deletes or renames an existing collection whose id matches a backup collection; backup collections merge into same-name local collections or are added as new ones (#1019)
- iOS ComfyUI generation form now sends the checkpoint, prompts, sampler settings, size, seed, ControlNet and denoise values to the shared ViewModel, so Generate submits what was entered instead of silently doing nothing (#1025)
- Download Queue no longer crashes on Android — registered the missing `AndroidDownloadScheduler` Koin binding (`DownloadScheduler` was bound on iOS/Desktop but not Android since #694) (#1003)
- Bounded the interaction-event query with a limit (#987)
- ComfyUI generation now reaches Completed/Error over the WebSocket — `/prompt` sends the same `client_id` the progress socket connects with, so ComfyUI delivers `execution_success`/`execution_error` to the app (#1021)
- iOS External Server gallery now shows the server's images, loading and error states, and its Filter/Generate actions — the screen observes the shared ViewModel state instead of re-requesting page 1, and the Filter/Generate sheets open and close through the shared ViewModel so they no longer reopen or close on their own (#1029)
- Swipe discovery now sends the NSFW filter setting with its `/models` requests and filters card images to the chosen level, so users with Soft/All see NSFW models and Off no longer gets blank cards (#1071)
- iOS ComfyHub browser and workflow detail screens now show their data — both defined `observeUiState()` on their owner but never called it, so the shared ViewModel state (workflows, loading, errors, import/save results) never reached the view (#1029 follow-up)
- ComfyUI checkpoint picker loads on current ComfyUI servers — the checkpoint names are read from the first element of `ckpt_name` and the appended `{"tooltip": ...}` object is ignored, instead of failing to decode and leaving the built-in generation form without checkpoints (#1023)
- Creator profile now sends the NSFW filter setting with its `/models` requests and filters card images to the chosen level, so users with Soft/All see a creator's NSFW models and Off no longer gets blank cards (#1072)
- Tagged releases publish the Android APK again — the release workflow builds the signed `githubFull` flavor and copies it from its flavored output path, which the product-flavor split (#986) had broken (#1002)
- ComfyUI generation with the default seed no longer fails — a negative seed (the app's -1 "random" sentinel) is resolved to a random non-negative value right before submission, for both the built-in workflow and imported workflows (#1024)
- ComfyUI connection test (onboarding and the settings Test button) passes only when `/queue` answers 2xx with ComfyUI's `queue_running` field, so an auth proxy's 401 JSON or another JSON service on the port is no longer reported as "Connected" (#1057)
- Swipe discovery keeps serving new cards — it follows CivitAI's `nextCursor` instead of refetching page one, skips ahead (up to 5 requests per load) past pages whose cards were all already seen, and stops loading once the API reports no next page (#1073)
- Android ComfyUI generation now applies the template picked from "Load template" — the picker opens as a full-screen dialog inside the generation screen and fills the form through `onTemplateApplied`, instead of a separate screen that discarded the choice (#1042)
- ComfyHub "import to server" posts the workflow to the active connection — it used the shared `ComfyUIApi` without setting its base URL, so it reached whichever server a queue, history or settings test call had set last, or failed when none had run in the session; with no active connection it now reports "No active ComfyUI connection" (#1122)
- Android "Try in ComfyUI" now opens the generation screen with the first sample image's prompt, negative prompt, steps, CFG and seed filled in, and selects the model's primary file as the checkpoint for Checkpoint models; the prefill applies once per screen so edits survive returning from another screen (#1039)
- iOS "Try in ComfyUI" now opens the generation sheet with the first visible sample image's prompt, negative prompt, steps, CFG and seed filled in, and selects the model's primary file as the checkpoint for Checkpoint models when the server has it; the prefill applies once per sheet so it never overwrites edits (#1040)
- ComfyUI onboarding's manual host field accepts a pasted URL such as `http://192.168.1.5:8188/` or `https://pc.tailnet.ts.net` — a scheme or port in the host text overrides the port field and HTTPS toggle (an `http://`/`https://` URL without a port uses 8188/443), instead of building `http://http://…:8188` and failing as unreachable; an unparseable host such as `host:abc` fails immediately without a network call (#1058)

### Infrastructure

- Extracted `core-ml`; network→database→core-data module load order; per-flavor packaging exclusions and manifest permissions (#986, #993)
- Synced ai-dev-template common + KMP layer files (#980)
- CI runs the `core-ml` unit tests; the Maestro smoke-test template builds and installs the `githubFull` debug APK from the `androidApp` module (#1002)

## [2.4.0] - 2026-07-11

### Added

- Create hub — the Create tab now shows a live server-connection status card (ComfyUI/SD WebUI/external server) with quick actions instead of a static settings shortcut, on Android, Desktop, and iOS (c3d0038c, 13f57a75, dadd5f73)
- Seamless list-to-detail image handoff — the cached list/grid thumbnail is used as a placeholder while the full-resolution detail image loads, eliminating the blank-then-pop-in flash on Android/iOS/Desktop (3a5c45f7)
- Per-image NSFW card blur/badge with safest-first thumbnail selection and a reachable "Soft" filter level, replacing model-level-only blurring (9c53f1ed, 7bb3bbdf, 8df3c276)

### Changed

- Reordered the model detail screen into decision order (safety/compatibility info before description) and fixed a version-chip row layout blowup (3c84606e)
- Reduced APK size (275MB → 127MB) by excluding the SigLIP asset and ONNX native libraries from the build when on-device similarity search is disabled (6f5bafe8)
- Hid recommendation carousels while actively searching and capped idle-state sections at 2 to reduce home-screen scroll depth (d54e3c05)
- Localized all Create-hub strings, made connection status mapping honest (no false "connected"), and removed dead code found in review (d2d01ce2)
- Deduplicated settings section headers, added CI domain/network tests, and added a blur-effect fallback for devices below Android 12 (b64abf3e)
- Set up iOS String Catalog and externalized remaining hardcoded Settings/NSFW strings (e953161e)
- Bundled over-injected use cases into cohesive groups and removed stale `LongParameterList` suppressions (#936)

### Fixed

- Fixed navigation thumbnail args not matching the loaded card URL, which broke the shared-element placeholder cache hit on recommendation rows and creator grids (017d37fd)
- Fixed the iOS NSFW-level segmented control visually reverting by updating `@Published` immediately in the picker setter, before the KMP flow emits (ef329e0c)
- Fixed NSFW level bitmask mapping and ensured `nsfw` query params are always sent explicitly, never omitted (41e23293)
- Fixed the SD WebUI generation flow race so a failed generation always emits an `Error` state instead of silently hanging (100c93d4)

### Infrastructure

- Added commonTest unit test coverage across ~20 previously-untested use cases (ComfyUI export/import/workflow, core-plugin themes, external-server connections, dataset/download/history cleanup, model updates, embedding search) (a94145ef, 899af506, 753fdeca, f9fadbe0, 0e7b1c9a, ec563fd0, 26d10e32, 9f24b245, d2c8c5bf, 11c73759, 6346e40b, da072ec8, 966e1c94, cd3a0ffd, ec45b868, #931)
- Enabled `returnDefaultValues` for host tests so `Logger` calls don't throw under test (8b858d92)
- Resolved remaining file-level `TooManyFunctions`/`LongMethod` detekt debt via Composable config exemption, composable splitting, and delegate extraction (c20ed9d7, 1e78cc8c, 20c9c70d, ae00a128)

## [2.3.0] - 2026-06-21

### Added

- Android in-app self-update — the update banner now downloads, integrity-verifies (package name + signing certificate, with key-rotation support and downgrade rejection), and installs the APK in-app via `PackageInstaller`, instead of dead-ending on the GitHub release web page in a browser (#967, #968)
- civitai.red front-door support — choose which CivitAI site web & share links open (SFW `civitai.com` / full `civitai.red`), independent of the NSFW content filter, default SFW (#913)
- Guided remote ComfyUI connection onboarding — LAN auto-detect (Android/Desktop), QR, and manual entry, each followed by a connection test with actionable failure reasons and self-signed TLS handling (#914)
- ComfyUI APP mode support — auto-detect APP-mode metadata on workflow import and in ComfyHub, parse grouped/collapsible parameter sections, and handle IMAGE and BOOLEAN parameter types in the workflow editor (c5240659, 50068699, 2c8a0f75)
- Hardware info display via the `/system_stats` endpoint (VRAM/RAM/GPU) for connected ComfyUI servers on all platforms (e3831afb)
- VRAM compatibility badges and per-model optimization suggestions (418b8de9)
- Background ComfyUI generation monitoring — an Android foreground service keeps the WebSocket alive and fires a local notification on completion (8b225dc4, e7a1c52a)
- ntfy.sh push notification integration for ComfyUI generation events (ccbb55fc)
- SigLIP-2 text encoder for Android cross-modal (text-to-image) search (57847321)
- iOS haptic feedback via `UIImpactFeedbackGenerator` (#947)
- Content descriptions on actionable icons for screen-reader accessibility (#939)

### Changed

- Extracted `core-data` module so `core-database` no longer depends on `core-network`, restoring Clean Architecture dependency direction (#916)
- Migrated platform services from `expect class()` to commonMain interfaces + Koin constructor injection, removing Service-Locator (`KoinPlatform.getKoin().get()`) usage — Phase A: FileScanner, LocalIpProvider, ImageSaver, MaskPngEncoder (#917); Phase B: embedding models, notification/monitor/lifecycle services, and DatasetZipWriter (split into factory + path provider) (#928)
- Established shared `core-testing` module (fake repositories, `TestScope`-based `ApplicationScope`, Turbine) and added commonTest coverage for key ViewModels (#915)
- Raised `minSdk` from 24 to 29 (Android 10) and removed the deprecated `getExternalStoragePublicDirectory` gallery fallback (#818)
- Expanded the `Won't Do` scope: cloud generation services, proprietary AI models, on-device model preview
- Grouped search/detail ViewModel use cases into cohesive bundles to reduce constructor injection sprawl (#936)
- Split `ComfyUISettingsScreen` into section composables (#933)
- Migrated KMP library modules to the `com.android.kotlin.multiplatform.library` AGP plugin (#935)
- Externalized remaining hardcoded user-facing strings to resource files (#932)
- Extracted built-in workflow JSON into dedicated resource files (#944)
- Moved Auth/Review/Tag repository implementations to `core-data` (#937)
- Centralized the default ComfyUI host/port into shared constants (#940)
- Extracted shared ComfyUI parameter components into `core-ui` (8722e821)
- Applied design tokens for spacing and elevation across remaining screens (#946)
- Extracted a `launchSafe` extension to deduplicate ViewModel delegate boilerplate (f1c03c29)

### Fixed

- End-of-view duration tracking now records reliably on screen exit via an injected application scope, instead of being dropped when launched into the already-cancelled `viewModelScope` in `onCleared()` (#912)
- Handle a missing ComfyUI connection gracefully instead of crashing (#934)
- Surface previously-swallowed LAN scan errors to the UI state (#941)
- Return a typed failure for a missing API key in `ReviewRepositoryImpl` (#948)
- Use `firstOrNull` to avoid `NoSuchElementException` on empty flows (#943)
- Log failures before returning null in ML model loaders (#938)
- Remove a force unwrap in `DesktopAuthSettingsSection` (#945)
- Prevent an unwanted API reload on back navigation that broke shared element transitions (d525746a)
- Stop the stagger animation from replaying on back navigation from the detail screen (19a10382)

### Infrastructure

- Restored green CI: cleared accumulated `detekt` violations across all modules and repaired broken `shared` commonTest compilation (#920)
- Fixed pre-existing iosApp compile breakage (KMP type exports, actor isolation, `@retroactive`) and SwiftLint violations that went undetected while the iOS CI job was being path-filter-skipped (#922)
- iOS CI now runs on `macos-26` / Xcode 26 (Swift 6.2) with `ARCHS=arm64` to match the KMP `iosSimulatorArm64`-only framework; SwiftLint runs before the build (#920, #922)
- Expanded the CI Android job to run feature/core module unit tests in addition to shared tests, and added `core/**` + `feature/**` to the path filter (#915, #916)
- Added unit tests for data-layer repositories (#931)
- Added a reference-vector comparison test for `ImageEmbeddingModel` (574245c1)
- Release workflow now publishes a SHA-256 checksum (`*.apk.sha256`) for the Android APK (#968)

## [2.2.0] - 2026-04-10

### Added

- Text-to-image search with SigLIP-2 text encoder (#713, #715)
- Background embedding indexer — auto-embed on browse/favorite (#712, #714)
- On-device image embedding with ONNX Runtime (Android) and Core ML (iOS) (#699-#711)
- On-device Stable Diffusion feasibility research (#610, #716)
- Image quality auto-curation filter with Quality sort option (#605)
- Workflow template library with parameter editor and curated templates (#603)
- Personalized feed with affinity scoring, diversity controls, and click tracking
- Download queue management with pause/resume and hash verification
- Real-time WebSocket generation preview with interrupt support (#601)
- UiLoadingState interface for shared loading/error patterns across 16 UiState classes (#727)

### Changed

- Move 24 ViewModels from shared to owning feature modules (#724, #745)
- Split large ViewModels with delegate pattern: SearchPageLoader, DetailReviewDelegate, SearchFilterDelegate, GenerationExecutionDelegate, DetailNotesTagsDelegate, GalleryGenerationDelegate, DatasetExportDelegate (#720, #721, #736, #742)
- Split KoinHelper.kt (684 lines) into 7 focused files (#719, #740)
- Split WorkflowTemplateUseCases into 7 individual files (#736)
- Centralize CivitAI URLs into CivitAiUrls constants (#723, #739)
- Enable Dynamic Type support in iOS CivitDeckFonts (#726, #739)
- Remove lifecycle.viewmodel dependency from core-domain (#728, #739)
- Unify 37 ViewModels in shared commonMain with SKIE Observing pattern (#684-#694)
- Port download queue and workflow template screens to Desktop

### Fixed

- Fix DatasetZipWriter resource leak with try-finally (#717, #739)
- Fix silent error swallowing in WorkflowTemplateUseCases (#730, #739)
- Add iOS accessibility: 50+ accessibilityLabel/accessibilityHidden additions (#725, #739)
- Fix hardcoded spacing in ModelCardLayout (#732), badge colors (#737), empty state fonts (#738)
- Fix Desktop DI missing embedOnBrowseUseCase parameter (#743)
- Fix iOS SwiftLint type_body_length violation (#744)

### Infrastructure

- Codebase health audit with automated issue creation (#717-#738)
- Add HuggingFace/TensorArt badge colors to iOS design system (#737)

## [2.1.0] - 2026-04-01

### Added

#### Navigation Redesign
- Redesign tab navigation to Discover/Create/Library/Settings (#595, #596) (50744f25, 8e49f571)
- Add CreateHub and Library screens for nav redesign (#594) (2351e2c9)
- Add sidebar navigation to Desktop matching new IA (#597) (c49f1ec7)
- Restore custom navigation shortcuts in bottom nav bar (449595ca)

#### Multi-Source Search
- Add MultiSourceSearchUseCase for unified search (#592) (553226a5)
- Add HuggingFace API client for model search (#590) (154afbf2)
- Add TensorArt API client for model search (#591) (81e464d5)
- Add TensorArt repository interface and DI registration (#591) (fcb8dfb8)
- Add ModelSource enum and source field to Model (#589) (7427bff8)
- Add platform filter and source indicator UI (#593) (5495d216)
- Integrate MultiSourceSearchUseCase into search pipeline across all platforms (7219871b)

#### External Server Gallery
- Add nav shortcut for ExternalServer Gallery and image zoom modal (7a4482fe)
- Add horizontal pager to ExternalServer image detail (e7fd6aa9)
- Add image selection mode and bulk delete for ExternalServer gallery (f77e9907)

#### Discovery & Recommendations
- Add history TTL/cleanup, time-decay scoring, and recommendation diversity (c0ae9bfe)
- Track engagement signals: view duration and interaction type (#580) (954eea15)
- Add Recently Viewed section to search screen on all platforms (94e4c8b2)
- Add dedicated Browsing History screen (277f602d)

#### Integrations
- Add ComfyHub workflow integration (#445) (52407a04)
- Add image similarity search (#442) (a52e2657)
- Add model update notifications for followed models (#444) (596f1d93)

### Changed

- Unify ExternalServer gallery/detail UI with Output pattern (86add7c7)
- Fix ExternalServer test connection and remove hardcoded /api/ prefix (860dd4bf)
- Reduce initial load latency by lowering prefetch size and eliminating double paging (2682c038)
- Remove Recently Viewed from search, add dedicated Browsing History screen (277f602d)
- Consolidate cleanup API, unify weighted score logic (74fe66a5)

### Fixed

- Fix detail screen showing wrong image after 96th item (d2aa52e4)
- Fix NavDisplay onBack against emptying backstack (139a755a)
- Fix MainScope leak in ModelDetailViewModel.onCleared() (da2329a8)
- Replace unsynchronized mutableSetOf with MutableStateFlow for thread safety (f65a3a92)
- Add error logging to CivitaiLinkApi deserialization (85cace00)
- Replace non-atomic state updates with MutableStateFlow.update {} (6d6090a7)
- Filter out legacy history entries without model name from Recently Viewed (cfdf7688)

### Removed

- Remove unused parallaxOffset computation and rememberGridItemScrollOffset (a40ca33c)
- Remove duplicate LicensesRoute and ModelFileBrowserRoute entries (37d3d9f2)

### Infrastructure

- Extract PaginatedLoader helper for shared pagination pattern (a753c814)
- Split large files: NavGraph, SettingsScreen, Database (2e3cbbdd)
- Move cross-feature use cases to core-domain to fix module isolation (d1bf74f5)
- Move combined repos from core-database to shared, fix layer violation (b877a819)
- Move FormatUtils to core-domain, remove shared dep from core-ui (137da7fa)
- Replace java.net.URL with OkHttp in ImageDownloader (97edb209)
- Extract shared error handler in ComfyUIGenerationViewModel (f80a1d56)
- Extract dataset image entity-to-domain mapper to reduce duplication (f9f8009f)

## [2.0.1] - 2026-03-17

### Added

#### App Distribution
- Add GitHub Actions release workflow for signed APK and Desktop packages (9c8d321)
- Add /release Claude Skill for version bump and release automation (ea5f0b0)
- Add in-app update checker with GitHub Releases API polling (bdc2698)

### Infrastructure

- Add keystore files to .gitignore (2dab648)

## [2.0.0] - 2026-03-17

### Added

#### Social Sharing
- Add social share sheet with hashtag management for ComfyUI outputs (24335ab)
- Extend SocialShareSheet to External Server, Gallery, and Model Detail screens (5b414d5)
- Add hashtag management in Settings, allow custom hashtags and deleting all (05c0120)

#### Desktop Application
- Create desktopApp module with Compose Desktop shell (#467) (9acb5cb)
- Add JVM target to KMP modules with platform implementations (#466) (a9ea572)
- Convert core-ui from Android library to KMP module (#465) (8b5fd84)
- Add desktop packaging, keyboard shortcuts, and CI (#473) (e13b63e)

#### Plugin System
- Add core-plugin module with Plugin API, registry, and DI wiring (9e7b9f1)
- Add plugin storage, persistence and management use cases (#410) (e64707e)
- Add plugin management UI for Android and iOS (#411) (a94cf57)
- Add WorkflowEnginePlugin interface and ExternalServer/ComfyUI adapters (#412) (9221224)
- Add ExportFormatPlugin interface and kohya-ss adapter (#413) (d9c28fe)
- Add ThemePlugin interface with JSON import and theme selection (#414) (45339e7)

#### Desktop Enhancements
- Add desktop QR code generation and scanning (#511) (aad0fd9)
- Add desktop keyboard Tab navigation and focus rings (#508) (429ddff)
- Add Discovery/Trending tab to desktop navigation (#512) (1225e41)

#### Quality & Discovery
- Add quality filter toggle to search screen (#459) (a892527)
- Add quality score calculator and threshold preference (#457) (08adb03)
- Add integrations hub to unify server management (#438) (7ddfa25)

#### Community & Social
- Add community reviews and ratings to model detail (#189) (fedcd3e)
- Add creator follow and activity feed (#123) (538f982)
- Add QR code sharing (#188) (08c0180)
- Add model notes and personal tags (#186) (9862bff)

#### Analytics & Backup
- Add usage stats and analytics screen (#191) (9a371ac)
- Add backup and restore with granular category selection (#190) (48d2c70)

#### Dataset Export
- Add ExportRepository + ExportDatasetUseCase for dataset export (#287) (87fd4f9)
- Add Android dataset export UI with share sheet (#288) (855f3e4)
- Add iOS dataset export UI with share sheet (#289) (19b40c1)

#### Media
- Add video preview support in gallery and detail screens (#187) (d860743)
- Add background model file download manager (#382, #383, #384) (502a4be)

#### Accessibility
- Add contentDescription to Icons in SocialShareSheet (#566) (699d8a2)
- Add accessibility labels to CachedAsyncImage instances (#569) (78bc262)
- Add accessibilityLabel to remaining iOS icon views (#549) (92aa1fe)
- Add accessibilityLabel to icon-only buttons (#541) (7aa1e26)
- Add onClickLabel to clickable modifiers for accessibility (#451) (d63c4ef)
- Add empty state to BatchTagEditorScreen (#449) (89ee79c)
- Add missing accessibility labels across iOS views (#429) (262fc59)
- Add accessibility labels in ImageComparisonSlider (#427) (62b577d)
- Add accessibility labels to clickable elements (#366) (d28cc22)
- Add accessibility labels to iOS icon buttons (#367) (65f3731)
- Add accessibilityHidden to decorative stat icons (#369) (5f53e47)
- Add contentDescription to actionable Icon/AsyncImage components (#373) (897d3a1)
- Add accessibility annotations to decorative Image elements (#398) (5302204)
- Add accessibility traits to tappable elements (#399) (d029563)

#### Code Quality
- Add DomainException hierarchy for structured error handling (#564) (96471d7)
- Add error logging to ComfyUI and SDWebUI API calls (#547) (03d2e19)
- Add @Suppress to generic exception catch blocks (#551) (75aa4a5)
- Add timeout to ExternalServer polling loop (#536) (1939f9a)
- Add logging to swallowed exceptions across core modules (#389) (151ea08)
- Add key parameter to items() in AnalyticsScreen (#374) (95ba429)
- Add key params to itemsIndexed to prevent state leaks (#388) (d5f7e8a)
- Add logging to swallowed exceptions in ComfyUIGenerationViewModel (#372) (6a031fd)

### Changed

#### Desktop Porting
- Port dataset, backup, plugins, compare to desktop (#472) (6d6002b)
- Port settings and server integrations to desktop (#471) (ca4a0b4)
- Port collections, prompts, and feed to desktop (#470) (9085a0a)
- Port search, detail, and image viewer to desktop (#469) (9be4534)
- Extract ModelCard into core-ui for KMP reuse (#468) (cd2a361)
- Change Feed from card list to adaptive grid layout (all platforms) (e1e5ff1)

#### Settings Redesign
- Consolidate settings from 11 sub-screens to 5 groups (#437) (a750905)
- Split SettingsViewModel into 5 domain-scoped ViewModels (#440) (2e9a6b1)
- Gate Analytics and Datasets behind Power User Mode (#439) (ad23424)

#### External Server
- Enhance External Server with filters, image detail, generation, and pull-to-refresh (f665055)

#### Performance
- Improve performance: O(1) gallery lookup, batch observe tasks, add contentType (#377, #378, #379) (54751ed)
- Replace AsyncImage with SubcomposeAsyncImage in ImageComparisonSlider (#450) (94935e4)
- Use cached image loader in widget instead of AsyncImage (#428) (d06958e)

#### Design Token Migration
- Replace hardcoded fonts with civitTitleSmall in toast views (#567) (2d40f29)
- Replace hardcoded font sizes with civitLabelXSmall token (#568) (d9b2d98)
- Replace hardcoded elevation with design token (#555) (0ed541d)
- Add Spacing.xxs token and replace hardcoded 2.dp (#543) (af200a4)
- Replace magic numbers with spacing constants in widget (#542) (ef478dc)
- Make primary colors dynamic based on accent selection (#520) (c3d5f7e)
- Replace remaining hardcoded colors with design tokens in iOS views (#454) (39e61cd)
- Replace .font(.system()) with design tokens in iOS views (#453) (ac9728d, 6a2d70f)
- Replace hardcoded colors with design tokens in iOS views (#452) (5ab7a5b, b3e7aa6)
- Replace hardcoded Color.White with CivitDeckColors.onScrim (#425) (2da74aa)
- Replace hardcoded magic numbers in iOS views with design tokens (#426) (99c406e)
- Replace FormatUtils with native Swift formatting (#401) (ef0e390)
- Replace hardcoded font sizes with CivitDeckFonts tokens (#396) (c4595ec)
- Replace hardcoded AMOLED colors with design tokens (#397) (eaeef34)
- Replace hardcoded padding with Spacing tokens (#395) (c4d9b2e)
- Replace hardcoded dp values with Spacing tokens (#375) (f9265d8)
- Replace magic number paddings with Spacing tokens (#368) (30e1025)
- Replace hardcoded colors with theme tokens (#365) (08dd87c)

#### Architecture
- Use structured applicationScope instead of unmanaged CoroutineScopes (#565) (65ab39e)
- Split ComfyUIRepositoryImpl into separate repository classes (#498) (cae2b75)
- Move WorkflowTemplateViewModel registration to platform modules (#497) (3209fdc)
- Refactor ModelDetailScreen.kt into smaller composables (#421) (1a7b4bf)
- Refactor ModelSearchScreen.kt into smaller composables (#420) (04e8a7e)
- Switch review display from 5-star rating to thumbs up/down (cb2306d, e992e75)

#### Code Quality
- Replace println() with Logger utility (#371, #363) (556cca2, 400fed0)
- Replace force try with safe regex init (#364) (7829758)

### Fixed

- Fix Desktop build: add Elevation imports, extend ViewModel, remove protected onCleared calls (bd9376d)
- Fix test fake DAO return types to match Int signatures (855e9e4)
- Fix DAO return type mismatch in repository implementations (#554) (a093d55)
- Add DisposableEffect cleanup for Desktop ViewModels (#546) (783553d)
- Fix KMP IOException import: use Exception fallback (#537) (18af729)
- Update TileService API for Android 14+ (#540) (ec5feca)
- Fix image viewer: use fullScreenCover with non-optional index and dismiss() (850e915)
- Fix gesture conflicts: prevent UIKit tap recognizers from blocking SwiftUI buttons (8a7379b)
- Fix Saved tab navigation by using NavigationLink instead of Button (#523) (9be8018)
- Fix image viewer black screen on dismiss and add download/share buttons (#521) (5af6b20)
- Fix double back button in Settings on iPad (#519) (4f6ddeb)
- Fix Toggle/Picker bindings with optimistic local state update (#517) (654291f)
- Limit tab shortcuts on iPhone to prevent More tab (#518) (dcf5a8e)
- Register built-in plugins on desktop startup (acba766)
- Fix desktop audit issues: settings integration, NSFW, navigation, empty states (63cf05f)
- Apply theme mode, accent color, and AMOLED settings to desktop (906c6a9)
- Apply grid columns setting to desktop search screen (7670065)
- Fix desktop detail screens showing through from previous screen (ad03f9b)
- Fix desktop crash: add kotlinx-coroutines-swing for Dispatchers.Main (ff39d56)
- Remove quality filtering from Feed, keep only in Search (5210125)
- Fix CivitAI API stats mapping: use thumbsUpCount/thumbsDownCount (#458) (b05974b)
- Fix Feed back button crash when backStack has only root entry (3b5cb08)
- Fix feed showing empty when cached items have no stats (#458) (0025bc1)
- Fix built-in plugins not visible in Plugin Management screen (43f7d6f)
- Fix thread-safe register/unregister and isolate test instances (739847c)
- Fix SectionHeader divider layout in Row and reduce VersionDetail top spacing (6117814)
- Fix Notes/Tags divider layout and reduce Base Model section top spacing (3290f9c)
- Fix CollectionsScreen empty state during initial loading (#392) (68b70da)
- Fix DetailRow text overflow in model detail (#391) (2cfcaa2)
- Fix review findings: add deleteAll to connection DAOs, fix modifier ordering and key usage (d2c89c1)
- Fix review findings: add key() to reviews loop, use design tokens (505ce1f)
- Fix test fake DAOs: add missing abstract method implementations (721b0ce, c33958b)

### Removed

- Remove data layer dependency from domain use cases (#400) (cda072d)

### Infrastructure

- Update documentation for desktop target (#474) (4116c3f)
- Update ROADMAP: add Phase 6.5 (completed), Phase 7-10 (planned) (5c6f987)
- Reduce BackupRepositoryImpl complexity (#394) (abe8155)
- Extract ZoomableImageView to separate file for SwiftLint file_length (ae2e3a9)
- Fix detekt ImportOrdering and SwiftLint violations (bbadea0, 9faa0df, 48e61f8, 0f55f30, d4bb667, fde76fb, f9073fc, cb845ae, 6548b58, 2d93fe2)

---

## [1.2.0] - 2026-03-05

### Added

#### Custom External Server Integration
- Add custom external server integration — Android + iOS (#358) (4017cda)

#### Saved Search Filters
- Add saved search filters feature — Android + iOS (b95cb83)

#### Dataset Feature
- Add DatasetCollection domain models + ImageSource enum (#280) (d154c8f)
- Add Dataset Room entities, DAOs, migration v23→v24, repository + use cases (#281) (ccca6f4)
- Add Android Dataset list/detail screens (CRUD) (#282) (f7bdc56)
- Add iOS Dataset list/detail screens (CRUD) (#283) (2160c49)
- Add 'Add to Dataset' action in ComfyUI gallery + detail screen (#290) (ef0410d)
- Add 'Add to Dataset' action in iOS ComfyUI output detail and gallery (#291) (291d7cf)

#### Dataset Curation
- Add tag/caption repositories and batch edit use cases (#284) (a4c0a7a)
- Add caption editor and batch tag editor for Android (#285) (d8891d4)
- Add caption editor and batch tag editor for iOS (#286) (dacd504)
- Add trainable, licenseNote, pHash, excluded, and duplicate detection (#292, #294) (dab58fc)
- Add source badge, trainable toggle, and source filter UI (#293) (28b5428)
- Add duplicate review UI and resolution filter for Android (#295) (4df27ca)
- Add duplicate review UI and resolution filter for iOS (#296) (2f4c25e)

#### Output Gallery
- Add horizontal swipe navigation and scroll-to-top for Output Gallery (eb696a4)
- Add fullscreen image viewer to Output Gallery Detail (#338) (a6441b1)
- Add sort/filter controls to Output Gallery (#337) (3b7ea72)
- Add Output Gallery shortcut to Advanced Settings (#336) (4334e3e)

#### Settings & UX
- Add Theme (Light/Dark/System) setting to Appearance Settings (#334) (58e584f)
- Show current/total page indicator on image gallery expand button (#347) (5d228d8)
- Add carousel accessibilityLabel; fix .task in alert (#323 #324) (2bfb25b)

#### ComfyUI Generation History
- Add ComfyUI output gallery and detail screen — Android (#278) (cf8ff16)
- Add ComfyUI output gallery and detail screen — iOS (#279) (e3d6e0d)
- Add ComfyUI /history API client and FetchComfyUIHistoryUseCase (#277) (4fec378)

#### New Integrations
- Add SD WebUI (Automatic1111/Forge) API support (#178) (92c2228)
- Add Civitai Link integration (#179) (659cf91)

#### Error Handling & Accessibility
- Add missing key parameters to LazyColumn/LazyRow items calls (#355) (21fd04e)
- Add error handling to AuthRepositoryImpl.validateApiKey using Result<String> (#316) (dde32e1)
- Add accessibilityLabel to images in iOS views (#311) (7450d4b)
- Add onClickLabel to clickable containers in SettingsScreen, ImageGallery, Collections (#308) (b151d2e)
- Add contentDescription to navigation arrow icon in SettingsScreen (#307) (e3adba4)

### Changed

#### External Server
- Fix hardcoded spacing/corner-radius values in ExternalServerGalleryView (61d184c)

#### Navigation & UX
- Merge Prompts into Collections tab and add custom bottom nav shortcuts (334588d)
- Persist Search ViewModel across tab switches to prevent refetch (#332) (a162a1d)
- Move image gallery expand button to bottom-right with image count (#347) (180827e)
- Re-search on focus lost, regardless of query content (#345) (fa802b5)
- Re-search when text is manually cleared and focus leaves the field (#345) (d4238fb)
- Trigger re-search when clearing the search text field (#345) (ebea1f9)
- iOS parity: re-search on blur, per-item history delete, image counter (#345 #346 #347) (5aed107)
- Add scope description to Blur Intensity settings section (#341) (e95477c)
- Clarify Power User Mode scope and add Civitai Link subscription notice (#340) (bc1675f)
- Add logging to swallowed exceptions in shared layer (#325 #326 #327 #328) (32efe15)

#### Architecture & Code Quality
- Split repository interfaces by concern, removing @Suppress("TooManyFunctions") (#333) (d3abaf9)
- Split DatasetCollectionDao into two DAOs to fix TooManyFunctions detekt violation (994d6ab)
- Split monolithic DomainModule into feature-scoped sub-modules (#320) (f1af807)
- Split multi-class UseCase files into single-class files (#319) (7352b20)
- Wrap ContentConvertException in DataParseException to prevent Ktor types leaking into domain (#317) (2122f62)
- Allow cleartext HTTP traffic for ComfyUI local/Tailscale connections (5afa974)

#### iOS Design Token Migration
- Replace hardcoded status indicator colors in CivitaiLinkSettingsScreen with design tokens (#301) (a456e60)
- Replace hardcoded accent hex colors in TutorialStep with design tokens (#302) (f5b0048)
- Replace hardcoded Color.White/Black in overlay screens with design tokens (#303) (7e08319)
- Replace hardcoded .foregroundColor(.white/.red) with design tokens across iOS views (#304) (dc5d089)
- Replace Color.White tint with design token in AccentColorSwatch (#310) (01af5ba)
- Replace hardcoded .red foreground color with design token in CivitaiLinkSettingsView (#313) (e7900a4)
- Replace magic number padding with Spacing design tokens in iOS gallery/detail (#312) (a490538)
- Replace .onAppear with .task for async pagination callbacks (#314) (d976cbf)
- Replace .onAppear with .task for animation triggers in GestureAnimationView (#315) (fb8d065)
- Document intentional raw hex usage in AccentColorSwatch (#309) (a2a9d12)

### Fixed

- Fix stale ComfyUI output image: append promptId to URL for cache busting (ecbcef5)
- Fix iOS type errors: use native Swift types for non-nullable KMP params (#296) (da3bb49)
- Fix missing error handling in TagRepositoryImpl.getTags (#354) (bec058d)
- Fix missing error handling in CivitAiApi getModel/Version methods (#353) (c48c6c9)
- Revert SKIE type mapping: restore KotlinInt/KotlinBoolean wrappers (#351) (fef773f)
- Revert CachedAsyncImage in widget — target not yet set up as Xcode native target (#352) (b4e3e2d)
- Fix Color.civitError, reorder imports (detekt), add DB schema v25 (214590f)
- Fix Output Gallery: remove meaningless workflow filter, move shortcut to Settings main (#336 #337) (994ecca)
- Fix inverted sort order in Output Gallery (#337) (c8070c8)
- Fix misaligned dividers in ComfyUIOutputDetailScreen (#339) (86deea1)
- Fix filter modal infinite scroll by replacing verticalScroll with LazyColumn (#344) (e8f8357)
- Fix search history delete button to delete item instead of triggering search (#346) (af49416)
- Fix NSFW blur intensity setting having no effect (#329) (d3b899c)
- Fix action button overflow in ModelDetailScreen Android (#331) (e5c7758)
- Fix collection model count always showing 0 (#330) (4089e8f)
- Fix CoroutineScope leak in CivitaiLinkRepositoryImpl (#322) (e861bba)
- Fix iOS build: add NavShortcut/DeleteSearchHistoryItem type aliases, fix List selection binding (78e536f)
- Fix iOS build: add Dataset domain type aliases (7208d6f)
- Fix ComfyUI gallery/detail UX: push navigation, grid overlap, image fit, fullscreen tap (5b099e0)
- Fix ComfyUI image URL encoding to handle & and special characters in filenames (43bf0b8)
- Fix ComfyUI gallery HTTP image loading: iOS ATS and Android coil-network-okhttp (06bf445)
- Fix ComfyUI gallery images not loading: omit empty subfolder in view URL (e28d259)
- Fix Android ComfyUI detail screen not rendering: observe uiState reactively (301dfa4)
- Fix DomainModule type error and ModelSearchViewModel SwiftLint violation (16c23e7)
- Fix NotificationCenter observer leak in ModelSearchViewModel (#299) (4046722)
- Seed built-in workflow templates in DB onOpen callback (#318) (2bc8038)
- Fix iOS build: add missing KMP type aliases in SharedTypeAliases (e07847a)
- Fix lint and test issues from SD WebUI and Civitai Link PRs (1270d25)

### Removed

- Remove fullscreen tap dialog from ComfyUI detail screen (8e8281b)
- Remove println debug statement from CheckModelUpdatesUseCase (#300) (7e2c764)

### Infrastructure

- Fix SwiftLint file_length and type_body_length violations in ModelSearch files (41218c1)
- Add iOS build step to Quality Gate in dev and dev-all skills (9a08f33)
- Fix Android smart cast and detekt TooManyFunctions violations (ebabdea)
- Fix import ordering (detekt autocorrect) + add Room schema v24 (0823e17)
- Add cross-feature integration design document (#342) (5229598)
- Fix lint issues (line length) (fa15ffd)
- Apply Detekt formatting and add Room schema for migration 20→21 (8b21c55)
- Update ROADMAP to Phase 6 and fix ARCHITECTURE DB version (9260750)
- Fix SwiftLint line length in ComfyUIOutputDetailView (24fe8e6)

---

## [1.1.0] - 2026-02-28

### Added

#### ComfyUI Integration
- Add workflow template library with built-in and user-created templates (#177) (aed86da)
- Add LoRA/ControlNet support and custom workflow import (#195) (666aaa5)
- Add ComfyUI queue management and CivitAI model bridge (#194) (79ced8e)
- Add real-time WebSocket progress for ComfyUI generation (#176) (e51ed83)
- Add ComfyUI generation screen, navigation, and result preview (#148) (cf3640f)
- Add ComfyUI settings screen and connection management — Android (#148) (3e95484)
- Add ComfyUI settings screen and connection management — iOS (#148) (dfec10c)
- Add ComfyUI use cases for connection and generation (#148) (5c49a44)
- Add ComfyUI repository interface and implementation (#148) (4e5b698)
- Add ComfyUI API client, DTOs, and domain models (#148) (d6662c3)
- Add workflow export to ComfyUI and A1111 from metadata sheet (6d6b460)

#### Discovery & Collections
- Add swipe-based model discovery card stack (#168) (b88cf31)
- Add gesture-driven quick actions on model cards (#169) (8f02132)
- Add multi-collection management system (#121) (8af42fc)
- Add model comparison side-by-side view (#118) (ae01cc0)
- Add image slider comparison overlay (#170) (90f2cb5)
- Add model file browser with hash verification (#152) (289da9e)
- Add power user mode with advanced metadata panel (#150) (63ad619)

#### Prompts & Templates
- Add prompt history auto-save, template system, and search (8018b27)

#### Platform & System
- Add home screen widgets: Glance (Android) and WidgetKit (iOS) (#180 #124) (d2b6c10)
- Add iOS Siri Shortcuts via App Intents (#182) (61318e4)
- Add iOS Spotlight search and deep link infrastructure (#181) (2e35927)
- Add Android Quick Settings tile for search (#184) (1cb188c)
- Add background polling and local notifications for favorited model updates (1e60851)
- Add large screen and tablet support with adaptive grids and navigation (e56a05b)
- Add iOS shared element transition (zoom) for Search → Detail on iOS 18+ (d5799f9)

#### UX & Animations
- Add enhanced animations and transitions (#171) (1e6fe27)
- Add interactive gesture tutorial onboarding (#175) (5f548ab)
- Add haptic feedback system (#172) (f11dc51)
- Add custom accent colors and AMOLED dark mode (#174) (d3ca2bb)
- Add NSFW blur with adjustable strength (#173) (0c3128c)
- Add offline browsing with enhanced caching (#122) (4684143)

#### Accessibility
- Add accessibilityLabel to ImageViewer control buttons (#216) (c26a948)
- Add accessibilityLabel to SwipeDiscovery action buttons (#215) (5453f55)
- Add accessibilityLabel to ComfyUISettingsView radio button (#214) (8be7c0e)
- Add contentDescription to model images (#222) (582a350)
- Add semantic roles to clickable elements (#221) (1b7b2f8)
- Add empty state handling in SavedPromptsScreen and ModelDetailScreen (#219 #220) (a7f4b3f)
- Add empty state to SettingsScreen (#223) (95e0c49)

#### Design System Components
- Extract LoadingStateOverlay/LoadingStateView component (#233) (0d0106c)
- Extract ErrorStateView component (Android & iOS) (#232) (c925e5b)
- Extract EmptyStateView/EmptyStateMessage component (#231) (164df9a)
- Extract ModelStatsRow component (Android & iOS) (#234) (aa1287e)
- Extract FilterChipRow/ChipButton component (Android & iOS) (#235) (10f4e0b)
- Extract SectionHeader composable to ui/components (#236) (2a75ead)
- Extract ExpandableTextSection composable to ui/components (#239) (529b51d)
- Extract SearchBarView component to DesignSystem (#237) (a5dbffc)
- Extract DetailRow component to DesignSystem (#238) (ebc1dc6)
- Extract CivitAsyncImage composable to ui/components — Android (#230) (5c0e7f2)
- Extract CivitAsyncImageView to DesignSystem — iOS (#230) (9caf428)
- Adopt Design System components across remaining screens (#229) (9805674)

#### Foundation
- Add CivitAI API key authentication foundation (485c2d6)
- Add unit tests for use cases, repositories, and ViewModels (46f4b47)
- Add GitHub Sponsors badge (81d2001)

### Changed

#### Architecture: Core + Feature Module Split
- Add core module build infrastructure and convention plugins (#251) (771ae03)
- Introduce Convention Plugins in build-logic/ for KMP modules (#242) (966674d)
- Add civitdeck.kmp.feature plugin and create empty feature modules (#257) (a9bfaf3)
- Migrate domain layer to :core:core-domain (#252) (00c5cba)
- Migrate network layer to :core:core-network (#253) (82f8ef5)
- Migrate database layer to :core:core-database (#254) (22063c1)
- Migrate Android UI components to :core:core-ui (#255) (bcdd4f8)
- Migrate Settings feature to :feature:feature-settings (#258) (a865c37)
- Migrate Creator feature to :feature:feature-creator (#259) (bd7a7c3)
- Migrate Prompts feature to :feature:feature-prompts (#260) (7cabb27)
- Migrate Gallery feature to :feature:feature-gallery (#261) (f13a8bd)
- Migrate Collections feature to :feature:feature-collections (#262) (2220d35)
- Migrate Detail feature to :feature:feature-detail (#263) (c1d2458)
- Migrate search/discovery feature to :feature:feature-search (#264) (20e2e36)
- Migrate ComfyUI feature to :feature:feature-comfyui (#265) (8069b0b)
- Migrate SettingsViewModel to commonMain (#243) (18ffe4d)

#### Settings & Appearance
- Reorganize Settings into hierarchical sub-screens (#208) (c200122)
- Replace hardcoded font sizes with CivitDeckFonts tokens (#225) (b100284)
- Replace hardcoded padding values with Spacing tokens (#226) (bf64eab)

#### Architecture & Code Quality
- Fix domain model layer violations: move entity mappers to data layer (#210) (717de13)
- Fix use case layer violations: inject domain interfaces (#209) (f13ca06)
- Return domain model from GetHiddenModelsUseCase instead of entity (#213) (7f1b39a)
- Reorder DB migration definitions to sequential version order (#212) (a1e063b)
- Replace DispatchQueue.asyncAfter with Task.sleep in ModelDetailComponents (#218) (211b260)
- Replace CommonCrypto with pure Kotlin SHA-256 for iOS simulator compatibility (14ef861)
- Use alias() for kotlin.android plugin in androidApp for AGP 9 prep (#246) (5218c5a)
- Fix pagination sort order inconsistency across page boundaries (40e6621)

### Fixed

- Fix quality gate: move ImageSaver to feature-comfyui, fix Detekt/SwiftLint violations (716baad)
- Fix iOS CI: use ad-hoc signing so KGP embed step is not skipped (b0ab3d7)
- Fix FakeComfyUIRepository missing observeGenerationProgress (#176) (f53354f)
- Fix ComfyUIUseCasesTest imports after feature-comfyui migration (6c68c77)
- Fix swipe discovery crashes and search scroll issues (ca12534)
- Fix DB migration crash: drop orphan category column and add @ColumnInfo defaults (3387693)
- Fix iOS ComfyUI build errors: use correct design token names (11011b3)
- Fix iOS 16 share sheet and A1111 empty metadata handling (1eae448)
- Fix iOS 16 compatibility: replace @AlertActionsBuilder with @ViewBuilder (81954c2)
- Fix iOS FileScanner: revert function extraction that broke K/N CStructVar (8635c41)
- Fix review feedback: CancellationException, recursive scan, nativeHeap leak (fb87286)
- Fix MetadataSheet: use Dictionary API instead of NSDictionary (77ee627)
- Fix KotlinBoolean type error and remove redundant cast (29bc0cb)
- Fix SKIE suspend function name: doInitializeAuth → initializeAuth (02b01b1)
- Fix ModelSearchScreen to use SwipeableModelCard with animations (f514cca)
- Fix CivitDeckDatabase migration 14→15 for accent colors (ec96ba6)
- Fix AccentColor.entries to allCases for Swift/SKIE (c7af98e)
- Fix Bool property access in SwipeDiscoveryView (6e373bb)
- Fix CacheInfo format for KMP commonMain (8d564a4)
- Replace AsyncImage with SubcomposeAsyncImage in ComfyUIGenerationScreen (#224) (c420658)
- Fix review findings: API level guard, permission request, rotation, bind limit (a22a6e7)
- Fix Phase 2 quality issues: bugs, dead code, and feature gaps (c24ca24)
- Fix review feedback: use design tokens, add power user mode tests (f39b47f)
- Fix review feedback: NULL dedup bug, encapsulate state, escape consistency (c3e1ec0)
- Fix Android build errors: duplicate string, Glance API, TileService API (d34beea)
- Fix iOS build errors: add Equatable to DeepLink, import CoreSpotlight (54ab53f)

### Infrastructure

- Apply Detekt auto-corrections (import ordering, indentation) (f5f7059)
- Move docs to docs/ directory and fix mermaid arrows (c8a5380)
- Update docs and add platform-specific reviewer subagents (11cc082)
- Rewrite CLAUDE.md, AGENTS.md, add skills/rules, fix docs (87272ab)
- Add audit and dev-all skills for codebase health check and batch development (7ea253d)
- Update roadmap with completed items across all phases (a02fee9)

---

## [0.1.0] - 2026-02-15

Initial release of CivitDeck.

### Added

- Model search and browse with filtering by type, sort, period, and tags
- Image gallery with staggered grid, full-screen viewer, and pinch-to-zoom
- Prompt metadata viewer with generation parameters and one-tap copy
- Favorites — save models and images locally for offline access
- Fresh Find — discover recently published models before they trend
- Creator profile browser
- Native Android (Jetpack Compose) and iOS (SwiftUI) from a shared KMP codebase
- CivitAI REST API integration via Ktor Client
- Room KMP local database for offline favorites and response caching
- Clean Architecture + MVVM with Koin dependency injection
- GitHub Actions CI (Android + iOS)

[Unreleased]: https://github.com/rioX432/CivitDeck/compare/v2.4.0...HEAD
[2.4.0]: https://github.com/rioX432/CivitDeck/compare/v2.3.0...v2.4.0
[2.3.0]: https://github.com/rioX432/CivitDeck/compare/v2.2.0...v2.3.0
[2.2.0]: https://github.com/rioX432/CivitDeck/compare/v2.1.0...v2.2.0
[2.1.0]: https://github.com/rioX432/CivitDeck/compare/v2.0.1...v2.1.0
[2.0.1]: https://github.com/rioX432/CivitDeck/compare/v2.0.0...v2.0.1
[2.0.0]: https://github.com/rioX432/CivitDeck/compare/v1.2.0...v2.0.0
[1.2.0]: https://github.com/rioX432/CivitDeck/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/rioX432/CivitDeck/compare/v0.1.0...v1.1.0
[0.1.0]: https://github.com/rioX432/CivitDeck/releases/tag/v0.1.0
