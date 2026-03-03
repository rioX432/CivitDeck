# AGENTS.md

This file provides guidance to AI coding agents working with this repository.

## Project Overview

CivitDeck is a power-user mobile client for [CivitAI](https://civitai.com/) — the largest open-source generative AI community. It provides a native Android & iOS experience for browsing models, images, creators, prompts, and galleries, built with Kotlin Multiplatform (KMP).

## Commands

```bash
# Android
./gradlew :androidApp:installDebug    # Build & install Android debug
./gradlew :androidApp:assembleDebug   # Build Android debug APK
./gradlew :androidApp:assembleRelease # Build Android release APK
./gradlew :shared:testDebugUnitTest   # Run shared module tests

# iOS (no CocoaPods — uses Kotlin/Native framework directly)
open iosApp/iosApp.xcodeproj          # Open in Xcode

# Code Quality
./gradlew detekt                      # Static analysis + auto-format (autoCorrect enabled in build.gradle.kts)
cd iosApp && swiftlint --strict       # SwiftLint (config: iosApp/.swiftlint.yml)
```

## Architecture

### Tech Stack
- Kotlin Multiplatform (KMP) — shared logic across Android & iOS
- Ktor Client — HTTP client for CivitAI REST API
- Kotlinx Serialization — JSON parsing
- Room KMP — local database (favorites, cache)
- Koin — dependency injection
- Jetpack Compose (Android) / SwiftUI (iOS) — UI
- Navigation 3 (`androidx.navigation3`) — Android screen navigation
- Clean Architecture + MVVM pattern with UDF (Unidirectional Data Flow)

### Module Structure

```
CivitDeck/
├── build-logic/              # Convention Plugins (civitdeck.kmp.library, civitdeck.kmp.feature, civitdeck.android.application)
├── shared/                   # KMP coordinator — re-exports core modules via api(); KoinHelper for iOS
├── core/
│   ├── core-domain/          # Domain layer: models, repository interfaces, use cases, DomainModule (Koin)
│   ├── core-network/         # Network layer: Ktor client, DTOs (CivitAI + ComfyUI), NetworkModule (Koin)
│   ├── core-database/        # Database layer: Room KMP entities/DAOs/migrations (v24), DatabaseModule (Koin)
│   └── core-ui/              # Shared Compose components + design tokens (Android-only)
├── feature/
│   ├── feature-search/       # Model search & swipe discovery
│   ├── feature-detail/       # Model detail + model comparison
│   ├── feature-gallery/      # Image gallery with NSFW blur and prompt extraction
│   ├── feature-creator/      # Creator profile browser
│   ├── feature-collections/  # User model collections (create, rename, bulk manage)
│   ├── feature-prompts/      # Saved prompts + template library (built-in & user-created)
│   ├── feature-settings/     # App settings (NSFW, appearance, notifications, storage)
│   └── feature-comfyui/      # ComfyUI integration: generation, queue, LoRA/ControlNet, workflow import
├── androidApp/               # Android app entry point, Navigation 3, ModelCard, widgets, tiles
│   └── ui/dataset/           # Dataset list/detail screens + AddToDataset sheet (in androidApp, not feature module)
└── iosApp/                   # iOS app entry point (SwiftUI)
    └── iosApp/
        ├── Features/         # Feature screens + ViewModels (Search, Detail, Gallery, Creator, Collections,
        │                     #   Prompts, Settings, ComfyUI, Dataset, Compare, ModelFileBrowser, Tutorial)
        └── DesignSystem/     # Design tokens (CivitDeckColors, CivitDeckFonts, CivitDeckSpacing,
                              #   CivitDeckMotion, CivitDeckShapes) + CachedAsyncImage, ShimmerModifier
```

### Key Design Patterns

**MVVM + UDF**
- ViewModels are platform-specific: `androidx.lifecycle.ViewModel` (Android), `ObservableObject` (iOS)
- Shared module exposes UseCases returning `Flow`/`StateFlow` — ViewModels subscribe to these
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
- Koin modules per core layer: `NetworkModule` (core-network), `DatabaseModule` (core-database), `DomainModule` (core-domain)
- `shared/src/commonMain/di/` re-exports core modules; `ViewModelModule` for SettingsViewModel
- Android: `CivitDeckApplication.kt` registers platform ViewModels
- iOS: `KoinHelper.shared.getXxx()` in `shared/src/iosMain/di/KoinHelper.kt`

**Image Loading**
- Android: Coil 3.x with `SubcomposeAsyncImage` for loading states
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
