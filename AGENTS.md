# AGENTS.md

This file provides guidance to AI coding agents working with this repository.

## Project Overview

CivitDeck is a mobile client for [CivitAI](https://civitai.com/) — the largest open-source generative AI community. It allows users to browse models, images, and prompts natively on Android & iOS, built with Kotlin Multiplatform (KMP).

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
./gradlew ktlintCheck                 # Lint check
./gradlew ktlintFormat                # Auto-format
./gradlew detekt                      # Static analysis
```

## Architecture

### Tech Stack
- Kotlin Multiplatform (KMP) — shared logic across Android & iOS
- Ktor Client — HTTP client for CivitAI REST API
- Kotlinx Serialization — JSON parsing
- SQLDelight — local database (favorites, cache)
- Koin — dependency injection
- Jetpack Compose (Android) / SwiftUI (iOS) — UI
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
│       │       │   ├── local/         # SQLDelight DAOs
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
│           ├── ui/            # Compose screens & components
│           ├── viewmodel/     # Android ViewModels
│           └── di/            # Android DI module
└── iosApp/                    # iOS application (SwiftUI)
    └── CivitDeck/
        ├── Views/             # SwiftUI views
        └── ViewModels/        # iOS ViewModels
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
- SQLDelight for offline favorites and response caching with TTL

**Dependency Injection**
- Koin modules defined in `shared/di/`
- Platform-specific modules in `androidApp/di/` and `iosApp/`

## Development Guidelines

- All shared logic goes in `commonMain` — platform-specific code only when necessary
- DTOs in `data/api/` are separate from domain entities in `domain/model/`
- Use cases should be single-responsibility (one public function per use case, returns Flow)
- ViewModels live in platform modules, not in shared — shared only provides UseCases and domain logic
- Android UI uses Jetpack Compose with Material Design 3
- iOS UI uses SwiftUI with native navigation patterns
- Image loading: Coil (Android), AsyncImage/Kingfisher (iOS)
- All API responses should be cached locally for offline support

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
