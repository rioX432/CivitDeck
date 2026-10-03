---
name: review
description: "Review the current branch against its base with reviewer count and specialists chosen from semantic risk (fast: coordinator only; highRisk: independent reviewers), producing verified, deduplicated, severity-ranked findings. Use before committing or opening a PR."
---

> **Host adaptation:** Treat product-specific tool names as capabilities. Use the
> equivalent tools available in the current host, ask through its interaction mechanism,
> and skip optional integrations that are unavailable. Resolve repository guidance from
> `AGENTS.md`, falling back to `CLAUDE.md` only when needed. `$ARGUMENTS` means the current
> user request; `${CLAUDE_SKILL_DIR}` means the directory containing this skill.

# /review — Risk-Profiled Code Review

Review the current branch's changes against the base branch, spending independent reviewers only where the risk warrants them.

## Step 0: Prepare

1. Resolve the base branch: the open PR's base (`gh pr view --json baseRefName`), otherwise the remote default branch (`git symbolic-ref refs/remotes/origin/HEAD`). Do not assume `main`. The session context's "Main branch" falls back to `main` when `origin/HEAD` is unset, so it is not a resolved base. Never write a branch name the lookup has not returned — not `main`, not the context's "Main branch" — into a later command; until the lookup returns, write `{base}` there
2. `git log {base}..HEAD` — commits on this branch
3. Build the changeset from both sources — `/dev` reviews before it commits, so the working tree usually holds most of the change:
   - `git diff {base}...HEAD` — committed changes
   - `git diff HEAD` — staged and unstaged changes
   - `git status --short` — untracked files; read the new source files in full
4. `gh pr view --json body` — PR description (if available)
5. Run each command individually — do NOT chain with `&&`

If both diffs are empty and there are no untracked source files, report "nothing to review" and stop.

## Step 1: Build Change Context and Choose the Review Profile

1. Understand the **intent** from commit messages and PR description
2. Categorize changed files: core logic, UI, infrastructure, tests
3. Classify the change with the signals and profiles of [rules/verification.md → Classify the change](../../rules/verification.md)
   — `fast`, `standard`, or `highRisk`. Use the caller's profile when it already classified this change for
   verification; raise it if the diff shows a signal the caller missed. Judge semantics and affected boundaries:
   a one-line change to token validation is `highRisk`, forty files of documentation are `fast`, and a change to
   what users see or can interact with (layout, sizing, styles, visible copy) is `behavior-change`, never `fast`
4. List the **surfaces** the change touches — security (auth, crypto, secrets, input handling, permissions), UI
   (components, screens, styles, accessibility), performance (hot paths, rendering, lists, I/O in loops), or a
   surface a project reviewer declares

## Step 2: Review by Profile

Reviewer count follows risk, not file count. Every profile uses the two checklists below; they differ in who
applies them.

| Profile | Independent reviewers | Specialists |
|---|---|---|
| `fast` | 0 — the coordinator (this session) reviews the diff itself | none |
| `standard` | 0 or 1 — the matching specialist when the change touches a specialist's surface; otherwise Agent A only for a concrete signal: `crosses-module-boundary`, `new-feature`, or a candidate Critical/Warning the coordinator cannot confirm alone | the one whose surface the change touches, as that single reviewer |
| `highRisk` | at least 1 — Agent A always; add Agent B for `public-contract`, `data-migration`, `cross-repository`, or `build-or-ci-config` | every matching surface |

Launch all independent reviewers and specialists for one review in the same turn so they run concurrently. A
reviewer that is not needed is not launched: its cost is tokens and wall-clock time with no expected finding.
A `highRisk` review always launches its reviewers, even when the diff is the only evidence available; a coordinator
pass never substitutes for them.

### Checklist A: Bug & Logic + Security
```
Review the changed files for bugs, logic errors, and security issues.
Read REVIEW.md (if exists) or .claude/rules/ for review criteria.

Focus areas:
- Null safety violations
- Concurrency issues
- Error handling gaps
- Security: hardcoded secrets, input sanitization, data exposure
- Platform compatibility
- Performance: memory leaks, main thread blocking, redundant calls

For each finding: [file:line] severity — description
Severity: Critical / Warning / Suggestion / Nit
```

### Checklist B: Architecture & Quality
```
Review the changed files for architecture and code quality.
Read REVIEW.md (if exists) or .claude/rules/ for review criteria.

Focus areas:
- Architecture: SRP, layer boundaries, dependency direction
- Design patterns: consistency with existing codebase
- Testing: changed code has corresponding test updates
- Naming and readability
- Unnecessary complexity or over-engineering

For each finding: [file:line] severity — description
Severity: Critical / Warning / Suggestion / Nit
```

As the coordinator, apply both checklists yourself for `fast`, and for `standard` without an added reviewer. An
independent reviewer ("Agent A" / "Agent B") receives the checklist as its brief, plus the changed paths and the
base, and returns findings only.

### Specialists (on matching surface only)

- `security-reviewer` — the security surface, or any `security` / `auth` signal
- `ui-reviewer` — the UI surface, when the project provides one
- `perf-reviewer` — the performance surface, when the project provides one

Check `.claude/agents/` for project-specific reviewers (e.g., `kmp-reviewer`, `ui-reviewer`) and read each one's
description; launch it only when the changed files fall in the surface it declares. If its agent type is not
registered in this session, launch a general-purpose agent with that file's body as the brief. A specialist counts
as an independent reviewer for the `highRisk` minimum.

## Step 3: Merge Findings

1. Collect findings from the coordinator pass and every reviewer launched
2. **Deduplicate**: merge findings that describe the same defect into one, keeping the file:line and a single final severity
3. **Verify every Critical and Warning** before it is reported: re-read the cited code and its callers and confirm the failure scenario actually occurs (concrete input or state → wrong result). Drop a finding the code refutes; downgrade one you cannot confirm to Suggestion and say so. A Critical blocks the change for whatever workflow called this review, so an unverified Critical costs a whole run
4. Assign final severity:
   - **Critical**: crash, data loss, security vulnerability, incorrect behavior
   - **Warning**: potential bug, performance issue, architecture violation
   - **Suggestion**: improvement opportunity, non-blocking
   - **Nit**: style/preference, optional

## Step 4: Present Report

```
## Review Summary

**Branch:** {current} → {base}
**Files changed:** N
**Review profile:** fast / standard / highRisk — signals: {signals}
**Reviewed by:** {coordinator | Agent A (Bug/Security) | Agent B (Arch/Quality) | specialists}
**Independent reviewers:** {N} — {why each was needed, or "none: fast profile"}
**Counts:** Critical {n} · Warning {n} · Suggestion {n} · Nit {n}

### Critical (must fix)
- [file:line] description

### Warning (should fix)
- [file:line] description

### Suggestion (nice to have)
- [file:line] description

### Nit (optional)
- [file:line] description
```
