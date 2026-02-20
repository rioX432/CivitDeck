---
description: Android-specific patterns and pitfalls for CivitDeck
globs: androidApp/**/*.kt, androidApp/**/*.kts
---

# Android Rules

## Coil 3.x Image Loading
- `ImageRequest.Builder` requires context from `LocalContext.current` — do not pass `applicationContext`
- `crossfade` needs explicit import: `coil3.request.crossfade`
- Use `SubcomposeAsyncImage` for shimmer/placeholder loading states

## Navigation 3
- `LocalNavAnimatedContentScope` is in `androidx.navigation3.ui`, NOT `androidx.navigation3.runtime`

## Detekt
- LongMethod limit is 60 lines — split composables into smaller functions early
- `autoCorrect = true` is configured in `build.gradle.kts` — do NOT pass `--auto-correct` flag
- Auto-correct may reorder imports, causing a second run to report new issues — run `./gradlew detekt` twice if needed

## Compose
- Design tokens live in `ui/theme/` — use them instead of hardcoded values
- Don't duplicate `Alignment` import when adding new imports to files that already have it

## Adaptive Layout
- `ui/adaptive/AdaptiveUtils.kt` — responsive grid columns and foldable posture detection using Material 3 Adaptive
