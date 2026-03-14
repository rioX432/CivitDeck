# /audit — Codebase Health Audit & Issue Creation

Audit CivitDeck for tech debt, bugs, and mobile/desktop UI/UX problems. Findings become GitHub Issues.

**Scope:** "" (android | ios | desktop | shared | all — default: all)

---

## Step 1: Setup

Create task tracker:

1. "Run static analysis (detekt + swiftlint)"
2. "Scan tech debt (TODOs, deprecated APIs, code smells)"
3. "Scan Android UI/UX issues"
4. "Scan iOS UI/UX issues"
5. "Scan Desktop UI/UX issues"
6. "Scan architecture issues (KMP shared module)"
7. "Aggregate findings"
8. "Create GitHub Issues"

Parse ``:
- `android` → skip iOS/Desktop scan and swiftlint
- `ios` → skip Android/Desktop scan and detekt
- `desktop` → skip Android/iOS scans
- `shared` → skip all platform scans, only architecture + tech debt
- `all` or empty → run everything

---

## Step 2: Static Analysis (Parallel)

Mark task 1 `in_progress`. Run tools **in parallel** (single message, separate Bash calls).

### 2a. Detekt (skip if scope = ios)

```bash
./gradlew detekt 2>&1 | tail -60
```

Parse output for:
- `LongMethod` violations (>60 lines)
- `TooManyFunctions`, `ComplexMethod`
- `MagicNumber`, `UnusedImports`
- `ForbiddenComment` (TODO/FIXME in production code)

### 2b. SwiftLint (skip if scope = android or shared or desktop)

```bash
cd iosApp && swiftlint --strict 2>&1 | head -100
```

Parse output for:
- `line_length` violations
- `function_body_length` violations
- `todo` rule matches
- `force_cast`, `force_try` violations

Mark task 1 `completed`.

---

## Step 3: Parallel Code Scans

Mark tasks 2–6 `in_progress`. Launch **5 Task subagents in parallel** (`subagent_type: "Explore"`, `model: "haiku"`).

---

### Subagent A: Tech Debt Scanner

```
Scan for tech debt in the CivitDeck KMP project.

Root: /Users/rio/workspace/projects/CivitDeck/

## What to find:

### 1. TODO / FIXME / HACK comments
Use Grep to find: `TODO|FIXME|HACK|XXX|WORKAROUND`
- In: shared/, androidApp/, iosApp/, desktopApp/
- Exclude: build/, .gradle/, DerivedData/, *.generated.*
- For each: record file, line, comment text, surrounding context (±3 lines)

### 2. Deprecated / removed API usage
- Kotlin: `@Deprecated` annotation usage in non-deprecated code
- Android: any `.apply` on deprecated lifecycle APIs
- iOS: `UIKit` imports in SwiftUI-only files
- Ktor: outdated client patterns (HttpClient engine setup)

### 3. Hardcoded values that should be constants/tokens
- Android Kotlin: hardcoded hex colors (`0xFF`, `Color(0x`, `#[0-9A-Fa-f]{6}`)
- Android Kotlin: hardcoded dp values as raw Int/Float (e.g. `16.dp` outside theme)
- iOS Swift: hardcoded hex colors (`Color(red:`, `UIColor(red:`)
- iOS Swift: hardcoded String font names, hardcoded spacing numbers
- Desktop Kotlin: hardcoded Color.Black, Color.White, Color.Red etc.

### 4. Dead code indicators
- `@Suppress("UNUSED_PARAMETER")` annotations
- Functions/classes with `// unused` or `// not used` comments
- Private functions that are never called (check with Grep for function name)

### 5. Commented-out code blocks
- Lines/blocks that look like commented code (not doc comments), e.g. `// val foo = ...`, `// someFunction(...)`

Return format (JSON-like list):
[
  {
    "category": "TODO|DEPRECATED|HARDCODED|DEAD_CODE|COMMENTED_CODE",
    "severity": "high|medium|low",
    "file": "relative/path/to/file.kt",
    "line": 42,
    "description": "What the issue is",
    "snippet": "the problematic code line(s)"
  }
]
```

---

### Subagent B: Android UI/UX Scanner (skip if scope = ios or shared or desktop)

```
Audit Android UI/UX code quality in the CivitDeck Android app.

Root: /Users/rio/workspace/projects/CivitDeck/androidApp/

Architecture context:
- Jetpack Compose + Navigation 3
- Design tokens in ui/theme/ (CivitDeckColors, CivitDeckTypography, CivitDeckSpacing)
- Coil 3.x with SubcomposeAsyncImage for image loading
- Detekt LongMethod limit: 60 lines

## Checks to perform:

### 1. Accessibility
- `Image(...)` or `AsyncImage(...)` without `contentDescription` parameter
- `Icon(...)` without `contentDescription`
- `Modifier.clickable {}` without `onClickLabel` or `semantics { role = ... }`
- `Box/Row/Column` acting as button (has `clickable`) but no semantic role

### 2. Compose correctness
- `LazyColumn/LazyRow` items WITHOUT `key(...)` — search for `items(` not followed by `key =`
- `rememberCoroutineScope` used for side-effect logic that should be `LaunchedEffect`
- `remember {}` without keys for values that depend on state/props (missing key param)

### 3. Missing UI states
- Screen Composables (files ending in Screen.kt) that have no empty state / error state handling
- `LazyColumn` that shows content but no `if (items.isEmpty())` branch

### 4. Design token violations
- Hardcoded colors: `Color(0x`, `Color.Red`, `Color.Blue`, `Color.Green`, `Color.White`, `Color.Black`, `Color.Gray`
- Hardcoded text sizes: `fontSize = NN.sp` (not from tokens)
- Hardcoded padding/spacing: `padding(NN.dp)` where NN is not from token system

### 5. Image loading
- `AsyncImage` used without `SubcomposeAsyncImage` for loading state
- `ImageRequest.Builder(applicationContext)` — should use `LocalContext.current`

Return the same JSON format as Subagent A.
```

---

### Subagent C: iOS UI/UX Scanner (skip if scope = android or shared or desktop)

```
Audit iOS UI/UX code quality in the CivitDeck iOS app.

Root: /Users/rio/workspace/projects/CivitDeck/iosApp/

Architecture context:
- SwiftUI + iOS 16.0 deployment target
- DesignSystem/: CivitDeckColors, CivitDeckFonts, CivitDeckSpacing, CivitDeckMotion, CivitDeckShapes
- CachedAsyncImage for image loading (NO third-party image libraries)
- Feature-based structure under Features/

## Checks to perform:

### 1. Accessibility
- `Image(...)` without `.accessibilityLabel(...)` or `.accessibilityHidden(true)`
- Tappable views (`.onTapGesture`) without `.accessibilityElement(children:)` or `.accessibilityAction`

### 2. iOS version compatibility (deployment target 16.0)
Grep for iOS 17+/18+ only APIs:
- `onScrollGeometryChange`, `scrollPosition(id:)`, `@Observable`, `.contentMargins`, `safeAreaPadding`

### 3. Design token violations
- Hardcoded colors: `Color(red:`, `Color(.sRGB`, `.white`, `.red`, `.orange`, `.black`, `.gray`
- Hardcoded font sizes: `.font(.system(size: NN))` instead of CivitDeckFonts
- Hardcoded spacing/corner radius values

### 4. Image loading
- `AsyncImage` used directly (should be `CachedAsyncImage`)

### 5. Missing UI states
- Views that load data but have no empty/error state

Return the same JSON format as Subagent A.
```

---

### Subagent D: Desktop UI/UX Scanner (skip if scope = android or ios or shared)

```
Audit Desktop (Compose Desktop) UI/UX code quality in CivitDeck.

Root: /Users/rio/workspace/projects/CivitDeck/desktopApp/

Architecture context:
- Compose Desktop (JVM) with Material 3
- Design tokens from core-ui (Spacing, CornerRadius, CivitDeckColors)
- Coil 3.x for image loading (SubcomposeAsyncImage)
- State-based navigation (backstack, not Navigation 3)
- Koin for DI (koinViewModel)

## Checks to perform:

### 1. Missing UI states
- Screens without error/empty/loading state handling
- SubcomposeAsyncImage without error lambda

### 2. Accessibility
- Icon without contentDescription
- clickable without semantic labels

### 3. Design token violations
- Hardcoded colors (Color.Black, Color.White, Color.Red, Color(0x...))
- Hardcoded dp not from Spacing/CornerRadius tokens

### 4. Image loading
- AsyncImage used instead of SubcomposeAsyncImage (no loading/error states)

### 5. Navigation
- Routes pushed but not handled in when blocks
- Missing back navigation on overlay screens

### 6. Desktop-specific
- JFileChooser/Swing calls on main thread (should be Dispatchers.IO/Swing)
- Keyboard event handling gaps
- Background color missing on overlay screens (previous screen shows through)

### 7. Settings integration
- User settings (gridColumns, themeMode, NSFW filter, powerUserMode) not applied
- Settings changes not reactive

Return the same JSON format as Subagent A.
```

---

### Subagent E: Architecture Scanner (KMP Shared Module)

```
Audit the KMP shared module architecture in CivitDeck.

Root: /Users/rio/workspace/projects/CivitDeck/shared/

## Checks to perform:

### 1. Layer boundary violations
- Use cases importing from `data/` package
- Domain models importing from `data/api/` DTOs

### 2. Use case design
- Use cases with more than one public method
- Use cases that don't return Flow/StateFlow

### 3. Error handling
- Empty catch blocks
- API calls without error handling

### 4. Room KMP
- DB version matches latest migration number

### 5. DI correctness
- Missing Koin bindings for use cases/repositories

### 6. Coroutines
- GlobalScope usage
- flow{} collecting another flow inside

Return the same JSON format as Subagent A.
```

---

## Step 4: Aggregate Findings

Mark tasks 2–6 `completed`. Mark task 7 `in_progress`.

Collect results from:
- Static analysis (detekt + swiftlint output)
- All 5 subagents

### Deduplication

If the same file+line appears in both static analysis and code scan, merge into one finding.

### Severity Classification

| Severity | Criteria |
|----------|----------|
| **Critical** | Crash risk, data loss, security issue, accessibility blocker |
| **High** | iOS version incompatibility, gesture conflict, layer boundary violation, thread safety |
| **Medium** | Missing error/empty state, design token violation, Compose correctness issue |
| **Low** | TODO comment, dead code, code style, naming |

### Group by Category

1. **Bugs** (Critical + High severity functional issues)
2. **Tech Debt** (TODO/FIXME, deprecated APIs, dead code)
3. **UI/UX — Android**
4. **UI/UX — iOS**
5. **UI/UX — Desktop**
6. **Architecture**

Mark task 7 `completed`.

---

## Step 5: Present Findings to User

Output a structured report:

```
## Audit Report — CivitDeck

Scope: {android|ios|desktop|shared|all}
Found: {N} issues across {K} files

### Bugs (N)
| # | Severity | Platform | File | Line | Description |
|---|----------|----------|------|------|-------------|
| 1 | Critical | Desktop | path/to/File.kt | 42 | Description |
...

### Tech Debt (N)
...

### UI/UX — Android (N)
...

### UI/UX — iOS (N)
...

### UI/UX — Desktop (N)
...

### Architecture (N)
...
```

---

## ── AskUserQuestion: Issue Creation ──

Ask the user:

**Q1: Which findings should become GitHub Issues?**
- All Critical + High findings (recommended)
- All findings
- Let me select by category
- None (report only)

**Q2: Which GitHub labels to apply?**
- bug, tech-debt, ui-ux (auto-detect per finding)
- bug only
- No labels

If user selects "Let me select by category", ask a follow-up listing the categories found.

---

## Step 6: Create GitHub Issues

Mark task 8 `in_progress`.

For each selected finding, create a GitHub Issue:

```bash
gh issue create \
  --title "{Category}: {concise description} ({filename}:{line})" \
  --body "$(cat <<'EOF'
## Summary

{Description of the issue}

## Location

`{relative/file/path}:{line}`

## Details

{snippet of problematic code}

## Impact

{Why this matters — UX impact, crash risk, user-facing effect}

## Suggested Fix

{Concrete suggestion for how to fix}
EOF
)" \
  --label "{auto-detected labels}"
```

### Label Mapping

| Category | Labels |
|----------|--------|
| Bugs (Critical) | `bug`, `priority: high` |
| Bugs (High) | `bug` |
| Tech Debt | `tech-debt` |
| UI/UX Android | `ui`, `android` |
| UI/UX iOS | `ui`, `ios` |
| UI/UX Desktop | `ui` |
| Architecture | `architecture` |

If a label doesn't exist in the repo, skip it (do not fail).

### Batch confirmation

If more than 5 issues will be created, show the full list first and ask:
```
About to create {N} GitHub Issues. Proceed?
```

Mark task 8 `completed`.

---

## Step 7: Final Report

```
## Audit Complete

Scope: {scope}
Issues found: {total}
GitHub Issues created: {N} / {selected}

| Category | Found | Created |
|----------|-------|---------|
| Bugs | N | N |
| Tech Debt | N | N |
| UI/UX Android | N | N |
| UI/UX iOS | N | N |
| UI/UX Desktop | N | N |
| Architecture | N | N |

Skipped: {reasons — e.g. "user selected report-only"}

Static Analysis:
- Detekt: {pass / N violations}
- SwiftLint: {pass / N violations}
```

---

## Error Handling

| Situation | Action |
|-----------|--------|
| detekt not available | Skip, note in report |
| swiftlint not installed | Run `brew install swiftlint`, retry once, else skip |
| Subagent returns no findings | Note "No issues found in this category" |
| Label doesn't exist in repo | Skip label silently |
| `gh issue create` fails | Report error with finding details so user can create manually |
| 0 findings total | Report "Codebase looks healthy!" and stop |
