---
name: audit
description: "Audit codebase for tech debt, code quality, and architecture issues — then create GitHub Issues"
argument-hint: "[scope: all (default), or specific directory/module]"
user-invocable: true
disable-model-invocation: true
allowed-tools:
  - Bash(gh issue create:*)
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
---

# /audit — Codebase Health Audit

Audit the codebase for code quality, tech debt, architecture, and security issues. Findings become GitHub Issues.

**Scope:** $ARGUMENTS (default: all)

---

## Step 1: Setup

Create task tracker:
1. "Run static analysis"
2. "Scan tech debt"
3. "Scan code quality"
4. "Scan architecture"
5. "Aggregate findings"
6. "Create GitHub Issues"

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
7. Inconsistent patterns across similar features

Return findings as structured list:
- category, severity, file, line, description, snippet
```

### Agent C: Architecture Scanner

```
Read CLAUDE.md to understand the project architecture, then scan for:

## What to find:
1. Layer violations (check architecture boundaries in CLAUDE.md)
2. Circular dependencies between modules
3. Incorrect dependency direction
4. Missing abstractions (concrete where interface should be)
5. Resource leak indicators (open without close)
6. Concurrency issues (shared mutable state without synchronization)

Return findings as structured list:
- category, severity, file, line, description, snippet
```

Mark tasks 2–4 `completed`.

---

## Step 4: Aggregate Findings

Mark task 5 `in_progress`.

### Deduplication
Merge findings from static analysis and code scans that reference the same file+line.

### Severity Classification

| Severity | Criteria |
|----------|----------|
| **Critical** | Crash risk, data race, memory leak, security |
| **High** | Architecture violation, missing error handling |
| **Medium** | Code smell, hardcoded value, missing test |
| **Low** | TODO comment, dead code, style issue |

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

For each selected finding:

```bash
gh issue create \
  --title "{Category}: {description}" \
  --body "$(cat <<'EOF'
## Summary
{Description}

## Location
`{file}:{line}`

## Details
{snippet}

## Suggested Fix
{suggestion}
EOF
)" \
  --label "{auto-detected labels}"
```

### Label Mapping
| Category | Labels |
|----------|--------|
| Critical | `bug`, `priority: high` |
| High | `bug` |
| Code Quality | `tech-debt` |
| Architecture | `tech-debt` |

Mark task 6 `completed`.

---

## Error Handling

| Situation | Action |
|-----------|--------|
| Static analysis tool not available | Skip, note in report |
| Agent returns no findings | Note "No issues found" |
| `gh issue create` fails | Report, user can create manually |
| 0 findings total | Report "Codebase looks healthy!" |
