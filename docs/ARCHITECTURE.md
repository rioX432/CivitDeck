# Architecture

This document describes CivitDeck's architecture, module structure, data flow, and key design decisions.

## Module Structure

```
CivitDeck/
├── build-logic/              # Convention Plugins (civitdeck.kmp.library, civitdeck.kmp.feature, civitdeck.android.application)
├── shared/                   # KMP coordinator — re-exports core modules via api()
│   └── src/
│       ├── commonMain/       # DI wiring, ViewModelModule (SettingsViewModel)
│       ├── androidMain/      # Android-specific Koin setup
│       └── iosMain/          # iOS Koin setup, KoinHelper.kt
├── core/
│   ├── core-domain/          # Domain layer: models, repository interfaces, use cases
│   │   └── src/commonMain/kotlin/.../
│   │       ├── domain/model/         # Pure Kotlin domain entities
│   │       ├── domain/repository/    # Repository interfaces (contracts)
│   │       ├── domain/usecase/       # Single-responsibility use cases (return Flow)
│   │       └── di/                   # DomainModule (Koin)
│   ├── core-network/         # Network layer: Ktor client, DTOs, API services
│   │   └── src/commonMain/kotlin/.../
│   │       ├── data/api/             # CivitAI + ComfyUI API clients, DTOs
│   │       └── di/                   # NetworkModule (Koin)
│   ├── core-database/        # Database layer: Room KMP entities, DAOs, migrations
│   │   └── src/commonMain/kotlin/.../
│   │       ├── data/local/           # Entities, DAOs, CivitDeckDatabase (v21)
│   │       ├── data/local/migration/ # Sequential migrations (1→2 … 20→21)
│   │       └── di/                   # DatabaseModule (Koin)
│   └── core-ui/              # Shared Compose components + design tokens (Android-only)
│       └── src/main/kotlin/.../
│           ├── ui/components/        # LoadingStateOverlay, ErrorStateView, ModelStatsRow, …
│           └── ui/theme/             # CivitDeckColors, CivitDeckTypography, CivitDeckSpacing
├── feature/
│   ├── feature-search/       # Model search & swipe discovery
│   ├── feature-detail/       # Model detail view + model comparison
│   ├── feature-gallery/      # Image gallery with NSFW blur and prompt extraction
│   ├── feature-creator/      # Creator profile browser
│   ├── feature-collections/  # User model collections (create, rename, bulk manage)
│   ├── feature-prompts/      # Saved prompts + template library (built-in & user-created)
│   ├── feature-settings/     # App settings (NSFW, appearance, notifications, storage)
│   ├── feature-comfyui/      # ComfyUI integration: generation, queue, LoRA/ControlNet, workflow import
│   ├── feature-dataset/      # (Phase 5-6) Dataset collection, tagging, caption editor
│   └── feature-export/       # (Phase 6) Dataset export UI (zip, kohya-ss format)
├── androidApp/               # Android app entry point
│   └── src/main/kotlin/
│       ├── CivitDeckApplication.kt   # Koin init + ViewModel DI
│       ├── ui/navigation/            # Navigation 3 routes & NavDisplay
│       ├── ui/components/            # ModelCard, SwipeableModelCard (Nav3 dependency)
│       ├── widget/                   # Glance home screen widgets
│       ├── tile/                     # Quick Settings tile
│       └── notification/             # Background polling notifications
└── iosApp/                   # iOS app entry point (SwiftUI)
    └── iosApp/
        ├── Features/         # Feature-based screens + ViewModels
        │   ├── Search/       │   ├── Detail/       │   ├── Gallery/
        │   ├── Creator/      │   ├── Collections/  │   ├── Prompts/
        │   ├── Settings/     │   ├── ComfyUI/      │   └── Compare/
        └── DesignSystem/     # Design tokens + shared components
            ├── CivitDeckColors.swift   ├── CivitDeckFonts.swift
            ├── CivitDeckSpacing.swift  ├── CivitDeckMotion.swift
            ├── CivitDeckShapes.swift   ├── CachedAsyncImage.swift
            └── ShimmerModifier.swift
```

## Data Flow

```mermaid
graph TB
    api["CivitAI REST API"] --> ktor["Ktor Client"]
    ktor --> dto["DTOs (data/)"]
    dto -- "map to domain" --> repo["Repository (impl)"]
    room["Room KMP (cache)"] <--> repo
    repo --> usecase["Use Case"]
    usecase -- "Flow&lt;T&gt;" --> avm["Android ViewModel"]
    usecase -- "Flow&lt;T&gt;" --> ivm["iOS ViewModel"]
    avm --> compose["Jetpack Compose"]
    ivm --> swiftui["SwiftUI Views"]
```

## Layer Responsibilities

### Data Layer (`core/core-network/` + `core/core-database/`)

- **API** (`core-network`): Ktor HTTP client targeting `https://civitai.com/api/v1`. Endpoints include `/models`, `/models/:id`, `/model-versions/:id`, `/images`, `/creators`, and `/tags`. Pagination is cursor-based for images and page-based for others. Also includes a ComfyUI API client for local generation workflows.
- **Local** (`core-database`): Room KMP database (version 21) for offline favorites, user collections, saved prompts, SD WebUI connections, and response caching with TTL. Migrations tracked sequentially from version 1.
- **Repository Implementations**: Combine remote API calls with local cache. Return domain models, not DTOs.

#### Phase 5-6 Domain Extensions (planned)

New domain models for dataset curation and training pipeline preparation:

| Model | Description |
|---|---|
| `DatasetCollection` | Top-level dataset group (independent of Favorites) |
| `DatasetImage` | Single image within a dataset — references a FavoriteImage or ComfyUI output |
| `ImageTag` | Custom tag supporting batch editing across multiple images |
| `Caption` | Per-image training caption (plain text, maps to `caption.txt` in kohya format) |
| `ExportManifest` | Metadata definition for JSONL/CSV export |
| `ImageSource` | Provenance enum: `CivitAI`, `Local`, `Generated` |

New repository interfaces (to be added to `core-domain`):

| Repository | Responsibility |
|---|---|
| `DatasetCollectionRepository` | CRUD + add/remove images within a dataset |
| `ImageTagRepository` | Batch tag operations across multiple images |
| `ExportRepository` | Zip generation and manifest output |
| `ComfyUIHistoryRepository` | ComfyUI `/history` API wrapper (extends existing `ComfyUIRepository`) |

New use cases (to be added to `core-domain`):

- `CreateDatasetCollectionUseCase`
- `AddImageToDatasetUseCase`
- `BatchEditTagsUseCase`
- `EditCaptionUseCase`
- `ExportDatasetUseCase`
- `FetchComfyUIHistoryUseCase`

### Domain Layer (`core/core-domain/`)

- **Models**: Pure Kotlin data classes with no framework dependencies.
- **Repository Interfaces**: Contracts that the data layer implements.
- **Use Cases**: Single-responsibility classes with one public function each, returning `Flow` or `StateFlow`.

### UI Layer (Platform-specific)

- **Android**: Jetpack Compose with Material Design 3. Navigation uses AndroidX Navigation 3 with type-safe routes. ViewModels extend `androidx.lifecycle.ViewModel`.
- **iOS**: SwiftUI with native navigation (`NavigationStack`). Feature-based structure under `Features/`. ViewModels use `ObservableObject` protocol. Custom `CachedAsyncImage` for image loading (no third-party dependency). Design tokens in `DesignSystem/`.

## Key Design Decisions

### Why KMP?

Kotlin Multiplatform allows sharing business logic (networking, caching, domain models) between Android and iOS while keeping UI fully native. This avoids the compromises of cross-platform UI frameworks while eliminating duplicate business logic.

### Why Clean Architecture?

Strict separation between data, domain, and UI layers enables:
- Testable business logic independent of frameworks
- Swappable data sources (API, cache, mock)
- Platform-specific UI without touching shared code

### Why Room KMP over SQLDelight?

Room KMP provides the same API as Jetpack Room (familiar to Android developers) while supporting KMP. It handles schema migrations with compile-time verification.

### Why Platform-specific ViewModels?

ViewModels are intentionally **not** in the shared module. Each platform has its own lifecycle and state management patterns:
- Android: `androidx.lifecycle.ViewModel` with `viewModelScope`
- iOS: `ObservableObject` with `@Published` properties

The shared module exposes `UseCase` classes returning `Flow`, which each platform's ViewModel subscribes to.

### Why Navigation 3 (Android)?

AndroidX Navigation 3 is the latest navigation library with full type-safe route support and first-class Compose integration. It replaces string-based navigation with Kotlin data classes.

## Dependency Injection

Koin is used as the DI framework across all modules:

- **core-network** (`core/core-network/.../di/NetworkModule`): Ktor client, CivitAI and ComfyUI API services
- **core-database** (`core/core-database/.../di/DatabaseModule`): Room DB instance, all DAOs
- **core-domain** (`core/core-domain/.../di/DomainModule`): Repository bindings, use case factory
- **shared** (`shared/src/commonMain/di/`): Re-exports core modules; `ViewModelModule` for SettingsViewModel
- **Android** (`androidApp/CivitDeckApplication.kt`): Platform-specific ViewModel registrations, platform drivers
- **iOS** (`shared/src/iosMain/di/KoinHelper.kt`): Use case accessors for SwiftUI ViewModels via `KoinHelper.shared.getXxx()`

## CI/CD

GitHub Actions runs on every push to `master` and on pull requests:

1. **Android job**: Shared unit tests → Detekt lint → Debug APK build
2. **iOS job**: SwiftLint → Xcode build for iOS Simulator

See `.github/workflows/ci.yml` for the full configuration.
