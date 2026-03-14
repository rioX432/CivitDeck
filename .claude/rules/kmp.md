---
description: Kotlin Multiplatform shared module patterns
globs: shared/**/*.kt
---

# KMP Shared Module Rules

## Code Organization
- All shared logic goes in `commonMain` — platform-specific code only when necessary
- DTOs in `data/api/` are separate from domain entities in `domain/model/`
- Use cases: single-responsibility, one public function, returns `Flow`
- ViewModels live in platform modules (`androidApp/`, `desktopApp/`, `iosApp/`), NOT in shared
- Convention plugin includes `jvm()` target — `expect/actual` may be needed for JVM alongside Android/iOS

## Dependencies
- Ktor for HTTP, Kotlinx Serialization for JSON, Room KMP for local DB, Koin for DI
- Common Koin modules in `shared/src/commonMain/di/` (DataModule, DomainModule)
- Platform-specific Koin modules via `expect/actual` in `shared/src/androidMain/di/`, `shared/src/jvmMain/di/`, and `shared/src/iosMain/di/`
- Android ViewModels registered in `androidModule` inside `CivitDeckApplication.kt`
- Desktop ViewModels registered in `desktopApp/` (plain classes with CoroutineScope)
- iOS accesses dependencies via `KoinHelper.shared.getXxx()` (in `shared/src/iosMain/di/KoinHelper.kt`)

## API Responses
- All API responses should be cached locally for offline support
- Room KMP for offline favorites and response caching with TTL

## Database
- Tables with FOREIGN KEY constraints need seed data on fresh installs — Room migrations only run on upgrades, not new databases
- Use `RoomDatabase.Callback.onOpen` with `INSERT OR IGNORE` to ensure required rows (e.g. default Favorites collection) always exist
