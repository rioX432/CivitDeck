---
name: audit
description: "Audit the codebase for tech debt, code quality, architecture, performance, visual, and dependency issues with parallel scanners, then file the findings as right-sized GitHub Issues. Use for a health check of a repo or module; scope it with tech-debt / quality / architecture / visual / deps."
argument-hint: "[all (default) | tech-debt | quality | architecture | visual | deps | <directory>]"
user-invocable: true
disable-model-invocation: true
allowed-tools:
  - Skill
  - Bash(gh issue list:*)
  - Bash(git log:*)
  - Glob
  - Grep
  - Read
  - Agent
  - TaskCreate
  - TaskUpdate
  - TaskList
  - AskUserQuestion
  - mcp__playwright__browser_navigate
  - mcp__playwright__browser_snapshot
  - mcp__playwright__browser_take_screenshot
  - mcp__playwright__browser_close
---

# /audit — Codebase Health Audit

Audit the codebase for code quality, tech debt, architecture, performance, visual, and dependency issues. Findings become GitHub Issues via the `issue` skill.

**Scope:** $ARGUMENTS (default: all)

## Scope Selection

`$ARGUMENTS` selects which scanners run. A directory path narrows the files every scanner looks at.

| Scope | Static analysis | Scanners |
|---|---|---|
| `all` (default) | yes | A, B, C, D, E |
| `tech-debt` | yes | A (debt) + C (architecture, performance) |
| `quality` | yes | B (quality, testing) |
| `architecture` | no | C |
| `visual` | no | D |
| `deps` | no | E |
| `<directory>` | yes, scoped | all, restricted to that path |

Skip the steps for scanners not selected, and say in the report which scanners were skipped.

---

## Step 1: Setup

Create task tracker:
1. "Run static analysis"
2. "Scan tech debt"
3. "Scan code quality"
4. "Scan architecture"
5. "Scan visual bugs"
6. "Scan dependencies"
7. "Aggregate findings"
8. "Create GitHub Issues"

---

## Step 2: Static Analysis

Mark task 1 `in_progress`.

Run the project's lint/static analysis commands from CLAUDE.md's Commands section.

If not specified, auto-detect:
- `build.gradle.kts` → `./gradlew detekt`, `./gradlew ktlintCheck`
- `package.json` → `npm run lint`
- `Cargo.toml` → `cargo clippy`
- `pyproject.toml` → `ruff check`

Mark task 1 `completed`.

---

## Step 3: Parallel Code Scans

Mark tasks 2–4 `in_progress`. Launch **3 Explore agents in parallel**.

### Agent A: Tech Debt Scanner

```
Scan for tech debt in the codebase.

## What to find:
1. TODO / FIXME / HACK / WORKAROUND comments
2. Deprecated API usage
3. Hardcoded values (magic numbers, config strings)
4. Dead code (unused functions, unreachable branches)
5. Commented-out code blocks

Return findings as structured list:
- category, severity (high/medium/low), file, line, description, snippet
```

### Agent B: Code Quality Scanner

```
Scan for code quality issues.

## What to find:
1. Long functions (50+ lines)
2. Large files (500+ lines)
3. Deep nesting (4+ levels)
4. Duplicated code (3+ similar blocks)
5. Missing error handling (empty catch, swallowed errors)
6. Public API without tests
7. Test files with no assertions
8. Missing edge case coverage (null, empty, boundary values)
9. God classes (too many unrelated responsibilities)
10. Inconsistent patterns across similar features

Return findings as structured list:
- category, severity, file, line, description, snippet
```

### Agent C: Architecture & Performance Scanner

```
Read CLAUDE.md to understand the project architecture, then scan for:

## What to find:
1. Layer violations (check architecture boundaries in CLAUDE.md)
2. Circular dependencies between modules
3. Incorrect dependency direction
4. Missing abstractions (concrete where interface should be)
5. Resource leak indicators (open without close), retain cycles, leaked references
6. Concurrency issues (shared mutable state without synchronization)
7. N+1 queries and missing indexes on frequently queried columns
8. Unbounded lists without pagination
9. Heavy computation on the main/UI thread

Return findings as structured list:
- category, severity, file, line, description, snippet
```

Mark tasks 2–4 `completed`.

### Agent D: Visual Bug Scanner

```
Scan for visual UI issues. Adapt method based on available tools.

## If Playwright MCP is available AND a web app (dev server URL in CLAUDE.md):
1. Start dev server (or use provided URL)
2. Navigate to key pages with Playwright
3. Take screenshots at default viewport
4. Analyze each screenshot for:
   - Layout overflow or element overlap
   - Text truncation or unreadable content
   - Missing images or broken assets
   - Blank/empty screens that should have content
5. Check responsive: resize to mobile (375px) and check again

## If Playwright is NOT available OR mobile native app:
1. Glob for UI files (*.kt Compose, *.swift SwiftUI, *.tsx, *.jsx, *.vue)
2. Scan for common visual bug patterns:
   - Hardcoded sizes (px instead of dp/sp/rem)
   - Missing error/loading/empty states
   - Missing contentDescription / accessibilityLabel
   - Clickable areas < 48dp (Android) / 44pt (iOS)
   - Unbounded text without maxLines or ellipsis

Return findings as structured list:
- category: visual, severity (high/medium/low), screen/file, description
```

Mark task for Agent D `completed`.

### Agent E: Dependency Scanner

```
Scan project dependencies for security and freshness issues.

## What to find:
1. Known security vulnerabilities (CVEs)
   - Gradle: `./gradlew dependencyCheckAnalyze` or check dependency versions against known CVEs
   - npm: `npm audit`
   - pip: `pip-audit` or check requirements
2. Outdated major versions (1+ major behind)
   - Gradle: check version catalogs or dependency declarations
   - npm: `npm outdated`
3. Deprecated dependencies (archived repos, no updates in 2+ years)
4. Dependency conflicts or duplicate versions
5. Unused dependencies (declared but not imported)

For each finding include:
- dependency name, current version, latest version, severity, CVE ID (if applicable), upgrade risk (low/medium/high)
```

Mark task for Agent E `completed`.

---

## Step 4: Aggregate Findings

Mark task 5 `in_progress`.

### Deduplication
Merge findings from static analysis and code scans that reference the same file+line.

### Severity Classification

| Severity | Criteria |
|----------|----------|
| **Critical** | Crash risk, data race, memory leak, security, **screen unusable** |
| **High** | Architecture violation, missing error handling, **layout broken/unreadable** |
| **Medium** | Code smell, hardcoded value, missing test, **minor visual issue** |
| **Low** | TODO comment, dead code, style issue, **pixel-level misalignment** |

Mark task 5 `completed`.

---

## Step 5: Present Findings

```
## Audit Report

Scope: {scope}
Found: {N} issues across {K} files

### Critical (N)
| # | File | Line | Description |
|---|------|------|-------------|

### High (N)
...

### Medium (N)
...

### Low (N)
...

Static Analysis: {pass / N violations}
```

---

## ── AskUserQuestion: Issue Creation ──

**Q1: Which findings should become GitHub Issues?**
- All Critical + High (recommended)
- All findings
- Let me select
- None (report only)

---

## Step 6: Create GitHub Issues

Mark task 6 `in_progress`.

Invoke the `issue` skill in batch mode — it owns duplicate checking, the sizing gate, splitting, the body template, and labels:

```
Skill("issue", args: "Batch: audit findings. Source: /audit run on {scope}.
{for each selected finding: severity, category, file:line, description, snippet, suggested fix}")
```

Audit-specific inputs to pass through:

- **Group before handing over**: findings sharing a root cause become one issue, not one per occurrence
- Only Critical and High findings are eligible; Medium and Low stay in the report
- Every finding carries its `file:line` so the issue's `Files (expected)` section is real

Do **not** call `gh issue create` directly from this skill.

Mark task 6 `completed`.

---

## Error Handling

| Situation | Action |
|-----------|--------|
| Static analysis tool not available | Skip, note in report |
| Agent returns no findings | Note "No issues found" |
| Issue filing fails | The `issue` skill outputs the drafted bodies as markdown for manual creation |
| 0 findings total | Report "Codebase looks healthy!" |
