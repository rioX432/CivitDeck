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
- `CachedAsyncImage` downsamples to `maxPixelSize` (default 400px) — set `maxPixelSize: 1200` for detail/fullscreen views

## Fullscreen Image Viewer Pattern
- **Reference implementation: `ImageViewerScreen.swift`** — always follow this pattern
- Use `if let index = selectedIndex { ZStack { ... } }` for conditional rendering inside fullScreenCover
- Pass ALL callbacks to `ZoomableImageView`: `onFocusModeChanged`, `onDismiss`, `onDragYChanged`
- Track `@State private var controlsVisible = true` and `@State private var dragOffset: CGFloat = 0`
- Show controls only when `controlsVisible && dragOffset == 0`
- Dismiss via `selectedIndex = nil` — do NOT use `@Environment(\.dismiss)`
- Background opacity must fade with drag: `1.0 - abs(dragOffset) / 100 / 4.0`
- Button foreground: `.white` (NOT `.civitInverseOnSurface` which is dark gray in dark mode)

## Tap Handling in TabView(.page)
- NEVER use `onTapGesture` inside `TabView(.page)` — conflicts with swipe gesture
- Use `.simultaneousGesture(TapGesture().onEnded { })` + `.contentShape(Rectangle())` — works alongside TabView's swipe
- `Button` inside `TabView(.page)` can also be unreliable — prefer `simultaneousGesture`

## ZoomableImageView Gesture Coordination
- `shouldRecognizeSimultaneouslyWith` must return `true` ONLY for `panRecognizer` (dismiss drag), NOT for tap recognizers
- `shouldReceive touch:` must ignore taps in edge areas (top/bottom 80pt) where SwiftUI overlay buttons live
- Without this, UIKit tap recognizers consume taps before SwiftUI buttons can receive them

## Toggle/Picker Bindings with KMP ViewModels
- Always update `@Published` property immediately in the Binding setter before calling KMP VM
- Pattern: `set: { viewModel.property = $0; viewModel.onPropertyChanged($0) }`
- Without this, the control visually reverts because the KMP flow hasn't emitted yet

## NavigationLink in List
- Use `NavigationLink(value:)` — provides automatic chevron and full row tap area
- Do NOT add manual `chevron.right` icons alongside NavigationLink
- Do NOT use `Button { path.append() }` in standard List rows — tap area is unreliable

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
