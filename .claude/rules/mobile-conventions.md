# Mobile Conventions

## KMP/CMP Shared Code
- Maximize shared code in `commonMain`
- Platform-specific code via `expect`/`actual` only when necessary
- No Android/iOS SDK imports in `commonMain`
- Shared UI uses Compose Multiplatform; platform-specific UI via `expect`/`actual` composables

## Android
- Compose-first UI (no XML layouts in new code)
- No `GlobalScope` — use `viewModelScope` or `lifecycleScope`
- No force unwrap (`!!`) — use safe calls or `requireNotNull` with message
- Follow Material Design 3 theming
- Min touch target: 48dp

## iOS
- SwiftUI preferred for new screens
- No force unwrapping (`!`) — use `guard let` or `if let`
- Dynamic Type support required for all text
- Safe area handling required
- Min touch target: 44pt

## Testing
- Shared unit tests in `commonTest`
- Roborazzi screenshot tests for visual regression (`@Preview` → auto-snapshot)
- Maestro for E2E smoke tests (launch + core flow only, keep minimal)
- Update golden images (`./gradlew recordRoborazziDebug`) when UI intentionally changes
