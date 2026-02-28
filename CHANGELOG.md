# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

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
