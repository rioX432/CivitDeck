# Mobile Conventions

## Android CLI (agent-first)

When available, use Android CLI for environment and project setup:
- `android sdk install` — SDK setup
- `android create` — project scaffolding from official templates
- `android emulator` — virtual device management
- `android run` — build/deploy
- `android docs <topic>` — query Android Knowledge Base for up-to-date guidance
- `android skills list` / `android skills add --skill <name>` — install Android Skills (edge-to-edge, Navigation 3, etc.)
- `android init` — install android-cli skill for agent integration

Use Android CLI commands before falling back to raw gradle/adb. It reduces token usage and follows current best practices.

## KMP/CMP Shared Code
- Maximize shared code in `commonMain`
- Platform-specific code via `expect`/`actual` only when necessary
- No Android/iOS SDK imports in `commonMain`
- Shared UI uses Compose Multiplatform; platform-specific UI via `expect`/`actual` composables
- K2 compiler is the default — no need for opt-in flags
- Swift Export is enabled by default — Kotlin code is translated to pure Swift for iOS interop

## Compose Multiplatform
- Stable for Android, iOS, Desktop (JVM). Web (Kotlin/Wasm) is Beta
- Hot Reload is enabled by default — no restart needed for UI changes
- Use unified `@Preview` annotation in common code (works across all platforms)
- Pausable composition is default — eliminates frame drops
- Use intelligent prefetching for lazy layouts

## Android
- Compose-first UI (no XML layouts in new code)
- No `GlobalScope` — use `viewModelScope` or `lifecycleScope`
- No force unwrap (`!!`) — use safe calls or `requireNotNull` with message
- Follow Material Design 3 theming
- Min touch target: 48dp
- Edge-to-edge display is the default — use `android skills add --skill edge-to-edge` for guidance

## iOS
- SwiftUI preferred for new screens
- No force unwrapping (`!`) — use `guard let` or `if let`
- Dynamic Type support required for all text
- Safe area handling required
- Min touch target: 44pt

## Testing
- Shared unit tests in `commonTest`
- Roborazzi screenshot tests for visual regression (`@Preview` → auto-snapshot)
- Maestro for E2E smoke tests (YAML-based, launch + core flow only, keep minimal)
- Update golden images (`./gradlew recordRoborazziDebug`) when UI intentionally changes
