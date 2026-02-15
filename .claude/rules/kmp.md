---
description: Kotlin Multiplatform shared module patterns
globs: shared/**/*.kt
---

# KMP Shared Module Rules

## Code Organization
- All shared logic goes in `commonMain` — platform-specific code only when necessary
- DTOs in `data/api/` are separate from domain entities in `domain/model/`
- Use cases: single-responsibility, one public function, returns `Flow`
- ViewModels live in platform modules (`androidApp/`, `iosApp/`), NOT in shared

## Dependencies
- Ktor for HTTP, Kotlinx Serialization for JSON, Room KMP for local DB, Koin for DI
- Platform-specific Koin modules in `androidApp/di/` and `iosApp/`

## API Responses
- All API responses should be cached locally for offline support
- Room KMP for offline favorites and response caching with TTL
