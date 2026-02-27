---
name: audit
description: "Audit codebase for tech debt, bugs, and mobile UI/UX issues — then create GitHub Issues"
argument-hint: "[scope: android|ios|shared|all (default: all)]"
user-invocable: true
disable-model-invocation: true
allowed-tools:
  - Bash(./gradlew detekt)
  - Bash(cd iosApp && swiftlint --strict)
  - Bash(gh issue create:*)
  - Bash(gh issue list:*)
  - Bash(git log:*)
  - Bash(git diff:*)
  - Bash(grep -r * *)
  - Glob
  - Grep
  - Read
  - Task
  - TaskCreate
  - TaskUpdate
  - TaskList
  - AskUserQuestion
  - mcp__codex__codex
  - mcp__codex__codex-reply
---

# /audit — Codebase Health Audit & Issue Creation

Audit CivitDeck for tech debt, bugs, and mobile UI/UX problems. Findings become GitHub Issues.

**Scope:** "$ARGUMENTS" (android | ios | shared | all — default: all)

---

## Step 1: Setup

Create task tracker:

1. "Run static analysis (detekt + swiftlint)"
2. "Scan tech debt (TODOs, deprecated APIs, code smells)"
3. "Scan Android UI/UX issues"
4. "Scan iOS UI/UX issues"
5. "Scan architecture issues (KMP shared module)"
6. "Aggregate findings"
7. "Create GitHub Issues"

Parse `$ARGUMENTS`:
- `android` → skip iOS scan and swiftlint
- `ios` → skip Android scan and detekt
- `shared` → skip both platform scans, only architecture + tech debt
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

### 2b. SwiftLint (skip if scope = android or shared)

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

Mark tasks 2–5 `in_progress`. Launch **4 Task subagents in parallel** (`subagent_type: "Explore"`, `model: "haiku"`).

---

### Subagent A: Tech Debt Scanner

```
Scan for tech debt in the CivitDeck KMP project.

Root: /Users/rio/workspace/projects/CivitDeck/

## What to find:

### 1. TODO / FIXME / HACK comments
Use Grep to find: `TODO|FIXME|HACK|XXX|WORKAROUND`
- In: shared/, androidApp/, iosApp/
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

### Subagent B: Android UI/UX Scanner (skip if scope = ios or shared)

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
- State reads inside lambdas that should be deferred for perf (snapshot reads)
- Composables with >1 `@Preview` that have duplicate names (causes preview conflicts)

### 3. Missing UI states
- Screen Composables (files ending in Screen.kt) that have no empty state / error state handling
- `LazyColumn` that shows content but no `if (items.isEmpty())` branch

### 4. Design token violations
- Hardcoded colors: `Color(0x`, `Color.Red`, `Color.Blue`, `Color.Green`, `Color.White`, `Color.Black`, `Color.Gray`
- Hardcoded text sizes: `fontSize = NN.sp` (not from tokens)
- Hardcoded padding/spacing: `padding(NN.dp)` where NN is not from token system

### 5. Navigation 3 correctness
- `LocalNavAnimatedContentScope` imported from `androidx.navigation3.runtime` (should be `androidx.navigation3.ui`)
- `navigate(...)` called with hardcoded String routes instead of typed routes

### 6. Image loading
- `AsyncImage` used without `SubcomposeAsyncImage` for loading state
- `ImageRequest.Builder(applicationContext)` — should use `LocalContext.current`
- Missing `crossfade` import: coil3.request.crossfade

### 7. Performance
- Composables with `Modifier.offset(x=..., y=...)` using dp values — should use `Modifier.offset { }` with pixel conversion via `LocalDensity.current`
- `LazyColumn` with very large item content that could cause janky scroll

Return the same JSON format as Subagent A.
```

---

### Subagent C: iOS UI/UX Scanner (skip if scope = android or shared)

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
- `AsyncImage` / `CachedAsyncImage` without accessibility label
- Tappable views (`.onTapGesture`) without `.accessibilityElement(children:)` or `.accessibilityAction`
- Text that might be truncated without `.accessibilityLabel` providing full text

### 2. iOS version compatibility (deployment target 16.0)
Grep for iOS 18+ only APIs in Swift files:
- `onScrollGeometryChange` — iOS 18+ only
- `scrollPosition(id:)` — iOS 17+ only
- `@Observable` macro — iOS 17+ only
- `.contentMargins` — iOS 17+ only
- `NavigationStack(path:)` if not guarded by `@available(iOS 17, *)`
- `safeAreaPadding` — iOS 17+ only

### 3. Gesture conflicts in ScrollView
- `DragGesture(minimumDistance: 0)` inside `ScrollView` — blocks scrolling
- `.gesture(DragGesture())` (not `.simultaneousGesture`) for swipe actions in lists
- Any `UIScrollView` delegate manipulation without `coordinateSpace` checks

### 4. Design token violations
- Hardcoded colors: `Color(red:`, `Color(.sRGB`, `Color(hue:`, `Color(white:`, `Color(hex:`, `#colorLiteral`
- Hardcoded font sizes: `.font(.system(size: NN))` instead of `CivitDeckFonts`
- Hardcoded spacing: `.padding(NN)` or `.frame(width: NN)` where NN is a magic number

### 5. Image loading
- `AsyncImage` used directly (should be `CachedAsyncImage`)
- Third-party image libraries: `import Kingfisher`, `import SDWebImage`

### 6. Missing UI states
- View files (ending in View.swift) that load data but have no empty state or error state
- `List { ForEach(...) }` without empty state handling

### 7. SwiftUI best practices
- `@State` variables that should be `@StateObject` (reference types stored as `@State`)
- `.onAppear` used for async data loading (should be `.task`)
- Missing `id:` parameter on `ForEach` for identifiable items using raw array indices
- `NavigationLink` with `isActive` binding (deprecated in iOS 16+, use NavigationStack)

### 8. Memory / performance
- `Timer` created in `View` body without cancellation on `onDisappear`
- `NotificationCenter.addObserver` without removal in `onDisappear`

Return the same JSON format as Subagent A.
```

---

### Subagent D: Architecture Scanner (KMP Shared Module)

```
Audit the KMP shared module architecture in CivitDeck.

Root: /Users/rio/workspace/projects/CivitDeck/shared/

Architecture rules:
- Clean Architecture: data/ → domain/ → UI (platform)
- Use cases: single-responsibility, return Flow
- Repository interfaces in domain/repository/, implementations in data/repository/
- DTOs in data/api/ are separate from domain entities in domain/model/
- Room KMP for caching with TTL
- Koin DI: common modules in commonMain/di/

## Checks to perform:

### 1. Layer boundary violations
- Use cases importing from `data/` package (bypasses domain layer)
- Repository implementations directly calling other repositories (should go through use cases)
- Domain models importing from `data/api/` DTOs (domain should not depend on data layer DTOs)

### 2. Use case design
- Use cases with more than one public method (should be single-responsibility)
- Use cases that don't return Flow/StateFlow (should be reactive)
- Use cases with direct DB access (should go through repository)

### 3. Missing error handling
- `suspend fun` in repositories that don't wrap exceptions in `Result<T>` or `Flow<Result<T>>`
- `try/catch` blocks that swallow exceptions with empty catch (just `catch (e: Exception) {}`)
- API calls without timeout or retry logic in Ktor client

### 4. Room KMP issues
- `@Entity` classes with foreign key constraints — check if seed data is handled in `onOpen` callback
- `@Query` returning nullable without null check at call site
- Migrations: check latest migration number matches DB version in `CivitDeckDatabase.kt`

### 5. DI correctness
- Classes instantiated with `=` instead of Koin `get()` inside Koin modules
- `single<>` used where `factory<>` is more appropriate (stateless services)
- Missing bindings: use cases that exist but are not registered in DomainModule

### 6. Coroutines / Flow misuse
- `GlobalScope` usage (should use structured concurrency)
- `flow { }` that collects another flow inside (use `flatMapLatest` etc.)
- `collect {}` instead of `collectLatest {}` for UI-bound flows

Return the same JSON format as Subagent A.
```

---

## Step 4: Aggregate Findings

Mark tasks 2–5 `completed`. Mark task 6 `in_progress`.

Collect results from:
- Static analysis (detekt + swiftlint output)
- All 4 subagents

### Deduplication

If the same file+line appears in both static analysis and code scan, merge into one finding.

### Severity Classification

| Severity | Criteria |
|----------|----------|
| **Critical** | Crash risk, data loss, security issue, accessibility blocker |
| **High** | iOS version incompatibility (app crashes on 16/17), gesture conflict (scrolling broken), layer boundary violation |
| **Medium** | Missing error/empty state, design token violation, Compose correctness issue |
| **Low** | TODO comment, dead code, code style, naming |

### Group by Category

1. **Bugs** (Critical + High severity functional issues)
2. **Tech Debt** (TODO/FIXME, deprecated APIs, dead code)
3. **UI/UX — Android**
4. **UI/UX — iOS**
5. **Architecture**

Mark task 6 `completed`.

---

## Step 5: Present Findings to User

Output a structured report:

```
## Audit Report — CivitDeck

Scope: {android|ios|shared|all}
Found: {N} issues across {K} files

### Bugs (N)
| # | Severity | File | Line | Description |
|---|----------|------|------|-------------|
| 1 | Critical | path/to/File.kt | 42 | Description |
...

### Tech Debt (N)
...

### UI/UX — Android (N)
...

### UI/UX — iOS (N)
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

Mark task 7 `in_progress`.

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
| UI/UX Android | `ui-ux`, `android` |
| UI/UX iOS | `ui-ux`, `ios` |
| Architecture | `tech-debt`, `architecture` |

If a label doesn't exist in the repo, skip it (do not fail).

### Batch confirmation

If more than 5 issues will be created, show the full list first and ask:
```
About to create {N} GitHub Issues. Proceed?
```

Mark task 7 `completed`.

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
