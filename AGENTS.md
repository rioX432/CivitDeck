# AGENTS.md

This file provides guidance to AI coding agents working with this repository.

## Project Overview

CivitDeck is a power-user client for [CivitAI](https://civitai.com/) — the largest open-source generative AI community. It provides a native Android, iOS & Desktop experience for browsing models, images, creators, prompts, and galleries, built with Kotlin Multiplatform (KMP).

## Commands

```bash
# Android
./gradlew :androidApp:installDebug    # Build & install Android debug
./gradlew :androidApp:assembleDebug   # Build Android debug APK
./gradlew :androidApp:assembleRelease # Build Android release APK
./gradlew :shared:testDebugUnitTest   # Run shared module tests

# Desktop (Compose Desktop / JVM)
./gradlew :desktopApp:run             # Run desktop app
./gradlew :desktopApp:packageDmg      # Package macOS .dmg
./gradlew :desktopApp:packageMsi      # Package Windows .msi
./gradlew :desktopApp:packageDeb      # Package Linux .deb

# iOS (no CocoaPods — uses Kotlin/Native framework directly)
open iosApp/iosApp.xcodeproj          # Open in Xcode

# Code Quality
./gradlew detekt                      # Static analysis + auto-format (autoCorrect enabled in build.gradle.kts)
cd iosApp && swiftlint --strict       # SwiftLint (config: iosApp/.swiftlint.yml)
```

## Architecture

### Tech Stack
- Kotlin Multiplatform (KMP) — shared logic across Android, iOS & Desktop
- Ktor Client — HTTP client for CivitAI REST API
- Kotlinx Serialization — JSON parsing
- Room KMP — local database (favorites, cache)
- Koin — dependency injection
- Jetpack Compose (Android) / SwiftUI (iOS) / Compose Desktop (JVM) — UI
- Navigation 3 (`androidx.navigation3`) — Android screen navigation
- Desktop navigation — state-based routing (no Navigation 3)
- Clean Architecture + MVVM pattern with UDF (Unidirectional Data Flow)

### Module Structure

```
CivitDeck/
├── build-logic/              # Convention Plugins (civitdeck.kmp.library, civitdeck.kmp.feature, civitdeck.android.application)
├── shared/                   # KMP coordinator — re-exports core modules via api(); shared ViewModels (presentation/); KoinHelper for iOS
├── core/
│   ├── core-domain/          # Domain layer: models, repository interfaces, use cases, DomainModule (Koin)
│   ├── core-network/         # Network layer: Ktor client, DTOs (CivitAI + ComfyUI + WebUI + ExternalServer), NetworkModule (Koin)
│   ├── core-database/        # Database layer: Room KMP entities/DAOs/migrations (v42), DatabaseModule (Koin)
│   ├── core-ui/              # Shared Compose components + design tokens (KMP: Android + Desktop)
│   └── core-plugin/          # Plugin system: interfaces, registry, capability adapters, PluginModule (Koin)
├── feature/                  # Feature modules with shared ViewModels in commonMain/presentation/
│   ├── feature-search/       # Model search & swipe discovery (shared ViewModels)
│   ├── feature-detail/       # Model detail + model comparison (shared ViewModels)
│   ├── feature-gallery/      # Image gallery with NSFW blur and prompt extraction (shared ViewModels)
│   ├── feature-creator/      # Creator profile browser (shared ViewModels)
│   ├── feature-collections/  # User model collections (shared ViewModels)
│   ├── feature-prompts/      # Saved prompts + template library (shared ViewModels)
│   ├── feature-settings/     # App settings (NSFW, appearance, notifications, storage)
│   ├── feature-comfyui/      # ComfyUI integration (shared ViewModels)
│   └── feature-externalserver/ # Custom external server (shared ViewModels)
├── androidApp/               # Android app entry point, Navigation 3, ModelCard, widgets, tiles
│   └── ui/                   # Platform-specific VM (DuplicateReview) + screens (dataset, compare, analytics, etc.)
├── desktopApp/               # Desktop app entry point (Compose Desktop / JVM), state-based navigation, thin VM wrappers
└── iosApp/                   # iOS app entry point (SwiftUI)
    └── iosApp/
        ├── Features/         # Feature screens consuming shared ViewModels via SKIE Observing
        │                     #   (Search, Detail, Gallery, Creator, Collections, Prompts, Settings, ComfyUI,
        │                     #   Dataset, Compare, ExternalServer, ModelFileBrowser, Analytics, Backup,
        │                     #   Discovery, Feed, Download, QRCode, Plugin, Shortcuts, Tutorial)
        └── DesignSystem/     # Design tokens (CivitDeckColors, CivitDeckFonts, CivitDeckSpacing,
                              #   CivitDeckMotion, CivitDeckShapes) + CachedAsyncImage, ShimmerModifier
```

### Key Design Patterns

**MVVM + UDF**
- 37 ViewModels are shared in `commonMain` using `androidx.lifecycle.ViewModel` (lifecycle 2.9.0), all 3 platforms consume the same VM class
- Android: `koinViewModel()` + `collectAsStateWithLifecycle()`; Desktop: `koinViewModel()` + `collectAsState()`; iOS: `*Owner` class + SKIE async sequence observation
- Platform-specific deps use expect/actual (e.g., `DownloadScheduler`); only 2 Desktop-only VMs remain (`DesktopUpdateViewModel`, `DesktopDiscoveryViewModel`)
- Complex screens may use sealed class Action/State for UDF; simple screens use plain StateFlow

**API Client**
- Base URL: `https://civitai.com/api/v1`
- Auth: Optional Bearer token (API key from CivitAI account settings)
- Endpoints: `/models`, `/models/:id`, `/model-versions/:id`, `/images`, `/creators`, `/tags`
- Pagination: Cursor-based for images, page-based for others

**Repository Pattern**
- Repository interfaces in `domain/repository/`
- Implementations in `data/repository/` combining API + local cache
- Room KMP for offline favorites and response caching with TTL

**Dependency Injection**
- Koin modules per core layer: `NetworkModule` (core-network), `DatabaseModule` (core-database), `DomainModule` (core-domain), `PluginModule` (core-plugin)
- `shared/src/commonMain/di/` re-exports core modules; `SharedViewModelModule`, `Phase3ViewModelModule`, `SettingsViewModelModule` for shared ViewModels
- Android: `CivitDeckApplication.kt` registers platform-specific bindings (DownloadScheduler actual, DuplicateReviewViewModel)
- iOS: `KoinHelper.shared.getXxx()` in `shared/src/iosMain/di/KoinHelper.kt` for ViewModel access

**Image Loading**
- Android: Coil 3.x with `SubcomposeAsyncImage` for loading states
- Desktop: Coil 3.x (no context required — JVM target does not use `LocalContext`)
- iOS: Custom `CachedAsyncImage` in `DesignSystem/` (no third-party dependency)

## Code Quality

After making code changes, run the appropriate linter before committing:

```bash
# Android / shared
./gradlew detekt                      # autoCorrect is enabled in build.gradle.kts

# iOS
cd iosApp && swiftlint --strict
```

## Git Commits

- Keep commit messages concise (one line)
- Do NOT add AI stamps (e.g., `🤖 Generated with Claude Code`) or `Co-Authored-By` lines

## Language

All written content in this project must be in English, including:
- Code comments and documentation strings
- Git commit messages
- Pull request titles, descriptions, and review comments
- GitHub Issues (titles and body text)
- CI/CD configuration comments
- README and other documentation files
