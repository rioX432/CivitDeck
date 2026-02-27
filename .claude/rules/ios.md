---
description: iOS-specific patterns and pitfalls for CivitDeck
globs: iosApp/**/*.swift
---

# iOS Rules

## Scroll Tracking
- `GeometryReader + PreferenceKey` does NOT work for scroll offset — `onPreferenceChange` doesn't fire during active scrolling on iOS 16/17
- Use `simultaneousGesture(DragGesture)` instead — fires every frame alongside ScrollView
- Accumulate delta (20pt+) to avoid jitter; reset on direction change
- `onScrollGeometryChange` is iOS 18+ only — deployment target is 16.0

## Gestures in ScrollView
- `DragGesture(minimumDistance: 0)` blocks ScrollView vertical scrolling — never use inside scrollable views
- For press effects (e.g. `springPress`), use `onLongPressGesture(minimumDuration: .infinity, pressing:, perform:)` instead
- Swipe-to-reveal in scrollable lists: use `.simultaneousGesture()` (not `.gesture()`) with horizontal direction check (`abs(width) > abs(height)`)

## Image Loading
- Uses custom `CachedAsyncImage` in `DesignSystem/` — no third-party library
- Do NOT add Kingfisher, SDWebImage, or other image loading dependencies

## Xcode Project
- pbxproj needs 4 entries for new files: PBXBuildFile, PBXFileReference, group children, PBXSourcesBuildPhase
- `No such module 'Shared'` in SourceKit is expected — KMP framework needs to be built first

## Architecture
- Feature-based structure under `Features/` — each feature has its own View + ViewModel
- Design tokens in `DesignSystem/` — use `CivitDeckColors`, `CivitDeckFonts`, `CivitDeckSpacing`
- Full DesignSystem components: `CivitDeckColors`, `CivitDeckFonts`, `CivitDeckSpacing`, `CivitDeckMotion`, `CivitDeckShapes`
- `AdaptiveGrid.swift` — screen-size adaptive grid columns
- `ShimmerModifier.swift` — loading shimmer animation
- `CachedAsyncImage.swift` — custom image loader (no third-party lib)
