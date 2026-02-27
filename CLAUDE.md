# CLAUDE.md

Strictly follow the rules in [AGENTS.md](./AGENTS.md).

## Think Twice

Before acting, always pause and reconsider. Re-read the requirements, re-check your assumptions, and verify your approach is correct before writing any code.

## Research-First Development (No Guessing)

**Guessing is prohibited.** Never design or implement based on assumptions. Always follow this order:

1. **Investigate first** — Read official docs, inspect source code, or web-search to confirm API signatures, behavior, and best practices. If a library API is unfamiliar, look it up before using it.
2. **Self-review** — After designing or implementing, verify:
   - Consistency with existing patterns in the codebase
   - Edge cases are handled
   - No unverified assumptions crept in
3. **Cross-review with Codex** — If Codex MCP (`mcp__codex__codex`) is available, use it to cross-check:
   - New module or architecture designs
   - Implementations that deviate from existing patterns
   - Complex decisions where you're not fully confident
   - Code review requests (always cross-review with Codex)
4. **Proceed only with confirmed information** — If the source of truth is unclear, investigate further or ask the user before writing code.

This applies to:
- Choosing library APIs and their correct usage
- Diagnosing bugs and identifying root causes
- Designing architecture and component structure
- Any decision where you are not 100% certain

## Language

- All code (comments, variable names, documentation) must be written in English
- All PR titles, descriptions, and commit messages must be written in English

## Key Gotchas

- Detekt auto-correct reorders imports — run `./gradlew detekt` twice if the first run changes import order
- Detekt LongMethod limit is 60 lines — split composables early
- `No such module 'Shared'` in SourceKit is expected — KMP framework needs to be built first
- Coil 3.x: `ImageRequest.Builder` needs context from `LocalContext.current`
- Coil `crossfade` needs explicit import: `coil3.request.crossfade`
- Navigation 3: `LocalNavAnimatedContentScope` is in `androidx.navigation3.ui`, NOT `androidx.navigation3.runtime`
- iOS deployment target is 16.0 — `onScrollGeometryChange` is iOS 18+ only
- iOS scroll tracking: `GeometryReader + PreferenceKey` doesn't fire during active scrolling — use `simultaneousGesture(DragGesture)` instead
- iOS gestures in ScrollView: `DragGesture(minimumDistance: 0)` blocks scrolling — use `onLongPressGesture(minimumDuration: .infinity, pressing:)` for press effects
- Android `Modifier.offset {}` uses pixels — convert dp via `LocalDensity.current` to avoid cards not fully animating off-screen
- Room KMP: migrations don't run on fresh installs — use `onOpen` callback with `INSERT OR IGNORE` for seed data (e.g. default collections)
- pbxproj needs 4 entries for new files: PBXBuildFile, PBXFileReference, group children, PBXSourcesBuildPhase
- SKIE suspend functions: K/N adds `do` prefix to `init`-prefixed functions (e.g. `initKoin` → `doInitKoin`), but SKIE generates its own Swift async extension without the prefix. Call `KoinKt.initializeAuth()`, NOT `KoinKt.doInitializeAuth()`
