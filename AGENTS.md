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
├── shared/                    # KMP shared module
│   └── src/
│       ├── commonMain/        # Shared code (API, domain, DI)
│       │   └── kotlin/
│       │       ├── data/
│       │       │   ├── api/           # Ktor API client, DTOs
│       │       │   ├── local/         # Room database, DAOs, entities
│       │       │   └── repository/    # Repository implementations
│       │       ├── domain/
│       │       │   ├── model/         # Domain entities
│       │       │   ├── repository/    # Repository interfaces
│       │       │   └── usecase/       # Use cases
│       │       └── di/               # Koin modules
│       ├── androidMain/       # Android-specific implementations
│       └── iosMain/           # iOS-specific implementations
├── androidApp/                # Android application (Jetpack Compose)
│   └── src/main/
│       └── kotlin/
│           └── ui/
│               ├── navigation/    # Nav3 NavDisplay & route definitions
│               ├── search/        # Search screen + ViewModel
│               ├── detail/        # Detail screen + ViewModel
│               ├── creator/       # Creator profile screen
│               ├── favorites/     # Favorites screen
│               ├── gallery/       # Gallery/image browsing
│               ├── prompts/       # Prompts screen
│               ├── settings/      # Settings screen
│               ├── components/    # Reusable Compose components
│               └── theme/         # Design tokens (colors, typography, spacing)
└── iosApp/                    # iOS application (SwiftUI)
    └── iosApp/
        ├── Features/          # Feature-based modules
        │   ├── Search/        # Search screen + ViewModel
        │   ├── Detail/        # Detail screen + ViewModel
        │   ├── Creator/       # Creator profile
        │   ├── Favorites/     # Favorites screen
        │   ├── Gallery/       # Gallery/image browsing
        │   ├── Prompts/       # Prompts screen
        │   └── Settings/      # Settings screen
        └── DesignSystem/      # Design tokens + shared components
            ├── CachedAsyncImage.swift   # Custom image loader (no third-party lib)
            ├── CivitDeckColors.swift
            ├── CivitDeckFonts.swift
            ├── CivitDeckSpacing.swift
            └── ShimmerModifier.swift
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
- Koin modules defined in `shared/di/`
- Platform-specific modules in `androidApp/di/` and `iosApp/`

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
