---
name: dev-all
description: "Process multiple GitHub Issues on a single branch. Investigates in parallel, implements sequentially, creates one PR."
argument-hint: "[issue numbers, e.g. #42 #43 #44, or empty for all open issues]"
user-invocable: true
disable-model-invocation: true
allowed-tools:
  - Bash(git checkout:*)
  - Bash(git add:*)
  - Bash(git commit:*)
  - Bash(git push:*)
  - Bash(git diff:*)
  - Bash(git log:*)
  - Bash(git status)
  - Bash(git branch:*)
  - Bash(git pull:*)
  - Bash(./gradlew *)
  - Bash(cd iosApp && swiftlint *)
  - Bash(gh pr create:*)
  - Bash(gh pr merge:*)
  - Bash(gh issue view:*)
  - Bash(gh issue list:*)
  - Glob
  - Grep
  - Read
  - Edit
  - Write
  - Task
  - TaskCreate
  - TaskUpdate
  - TaskList
  - TaskGet
  - ToolSearch
  - AskUserQuestion
  - mcp__codex__codex
  - mcp__codex__codex-reply
  - mcp__figma-remote__get_design_context
  - mcp__figma-remote__get_screenshot
---

# /dev-all — Batch Development on Single Branch

Process multiple GitHub Issues on **one branch** with **one PR**. Avoids merge conflicts from parallel branches.

**Arguments:** "$ARGUMENTS"

## Why Single Branch?

Parallel branches cause massive conflicts in shared files:
- **pbxproj**: every new iOS file modifies the same Xcode project sections
- **DB migrations**: sequential version numbering conflicts
- **Test fakes**: every interface change requires updating all fakes
- **DI registrations**: same Koin module files modified by multiple issues

Single branch with sequential implementation eliminates all of these.

---

## Step 1: Resolve Target Issues

**If `$ARGUMENTS` is provided:** Extract issue numbers (e.g. `#42 #43 #44` or `42 43 44`).

**If `$ARGUMENTS` is empty:** Fetch all open issues:
```bash
gh issue list --state open --json number,title,labels,body --limit 100
```

---

## Step 2: Context Gathering (Parallel)

### 2a. Read Project Context

Read directly (not via subagent):
- `AGENTS.md`
- `docs/ARCHITECTURE.md`

### 2b. Parallel Issue Investigation

Launch **parallel Task subagents** (`subagent_type: "Explore"`, `model: "haiku"`) — one per issue.

Each subagent:
1. `gh issue view {NUMBER} --json title,body,labels,comments`
2. Use Grep/Glob to find related code
3. Identify files that will need changes
4. Check for Figma links in the issue body
5. Return: issue summary, affected files, estimated scope, dependencies mentioned

### 2c. Create Task Tracker

Use `TaskCreate` for each issue:
- Subject: `#{number}: {title}`
- Description: scope, affected files, dependencies

---

## Step 3: Dependency Analysis & Execution Order

### 3a. Detect Dependencies

From issue bodies, detect blockers:

**Body text patterns (case-insensitive):**
- `blocked by #NNN`, `depends on #NNN`, `after #NNN`, `waiting for #NNN`

**Labels:** `blocked`, `blocked-by`, `on-hold`

For each blocker `#NNN`, check if open:
```bash
gh issue view NNN --json state
```

### 3b. Determine Execution Order

Topological sort:
1. Independent issues first (ascending by number)
2. Dependent issues after their dependencies
3. Circular dependencies → skip, report to user

### 3c. File Conflict Detection

From investigation results, identify issues that touch the **same files**. Order these sequentially to avoid edit conflicts, with the simpler change first.

Key conflict-prone files in CivitDeck:
- `iosApp/iosApp.xcodeproj/project.pbxproj` — any iOS file addition
- `shared/.../CivitDeckDatabase.kt` — migration version numbers
- `shared/.../UserPreferencesEntity.kt` — new preference fields
- `shared/.../UserPreferencesRepository.kt` — new preference methods
- `shared/.../DomainModule.kt` — new use case DI bindings
- `shared/.../KoinHelper.kt` — iOS use case accessors
- `androidApp/.../CivitDeckApplication.kt` — Android ViewModel DI
- `shared/src/commonTest/...` — test fakes implementing repository interfaces

---

## ── AskUserQuestion: Execution Plan ──

Present:
1. Ordered list of issues to implement
2. Dependencies detected
3. Any skipped issues (circular deps, external blockers)
4. Estimated scope per issue

Ask user to confirm before proceeding.

---

## Step 4: Branch & Implement

### 4a. Create Branch

```bash
git checkout master && git pull
git checkout -b feature/batch-{first-issue}-{last-issue}
```

### 4b. Sequential Implementation

For each issue in execution order:

1. **Mark task `in_progress`** via `TaskUpdate`

2. **Launch Task subagent** (`subagent_type: "general-purpose"`):

```
Implement GitHub Issue #{NUMBER}: "{TITLE}" on the CURRENT branch.
Do NOT create a new branch — you are already on the correct feature branch.

## Issue Details
{issue body}

## Files Likely Affected
{from investigation}

## Figma Design Context
{if Figma link was found, include design context here}

## Previously Implemented Issues in This Batch
{list of completed issues with: commit message, files changed}
→ If a file was already modified, BUILD ON those changes.

## Project Conventions
- Read CLAUDE.md and AGENTS.md for architecture rules
- Clean Architecture + MVVM, Koin DI, UDF
- Android: design tokens in `ui/theme/`, Coil 3.x
- iOS: `DesignSystem/` tokens, `CachedAsyncImage`, feature-based structure
- iOS deployment target 16.0 — no iOS 18+ APIs
- Detekt LongMethod limit: 60 lines

## Your Workflow

### Investigate
- Read ALL affected files before making changes
- Check if previous batch issues already modified these files
- Trace: UI → ViewModel → UseCase → Repository

### Implement
- Follow existing patterns exactly
- Keep changes minimal and focused
- When adding new iOS files: update pbxproj with 4 entries (PBXBuildFile, PBXFileReference, group children, PBXSourcesBuildPhase)
- When adding DB columns: create migration with next version number, update entity, update DB version

### Commit
- `git add {specific files}` (NO `git add .`)
- `git commit -m "{concise message} (#{NUMBER})"`
- No Co-Authored-By, no AI stamps

Report back: files changed, commit hash, any issues encountered.
```

3. **After subagent completes:**
   - Verify commit: `git log --oneline -1`
   - Mark task `completed`
   - Record changes for next subagent's context

4. **If subagent fails:**
   - Note the failure
   - Ask user: skip and continue, or stop?

---

## Step 5: Quality Gate

After ALL issues are implemented, run ALL checks on the **feature branch** (not master):

### 5a. Android Build (always run)
```bash
./gradlew :androidApp:assembleDebug
```

### 5b. Tests
```bash
./gradlew :shared:testDebugUnitTest
```

### 5c. Detekt (if Kotlin changed)
```bash
./gradlew detekt
```
Run twice if first run reports import reordering issues.

### 5d. SwiftLint (if Swift changed)
```bash
# Run from project root with explicit config — NOT `cd iosApp && swiftlint`
swiftlint --strict --config iosApp/.swiftlint.yml
```

### Failure Handling
- Fix and retry (max 3 attempts per check)
- Lint/test fixes go in a separate commit: `Fix lint/test issues`
- If still failing after 3 attempts → report to user, stop

---

## Step 6: PR Creation

### 6a. Push
```bash
git push -u origin feature/batch-{first-issue}-{last-issue}
```

### 6b. Create PR
```bash
gh pr create --title "Implement #{first}–#{last}: {brief summary}" --body "$(cat <<'EOF'
## Description

Batch implementation of the following issues:

{for each issue:}
- **#{NUMBER}**: {title} — {one-line summary}

## Related Issues

{for each issue:}
Closes #{NUMBER}

## Screenshots / Video

<!-- If applicable -->

## Test Plan

- [x] Unit tests pass (`./gradlew :shared:testDebugUnitTest`)
- [x] Detekt pass (`./gradlew detekt`)
- [x] SwiftLint pass (if Swift changed)

## Review Checklist

- [x] Each issue implemented as a separate commit
- [x] Code follows Clean Architecture + MVVM
- [x] Shared logic in `commonMain`, platform-specific only when necessary
- [x] No unnecessary dependencies added
- [x] Tests added/updated as needed

## Breaking Changes

None
EOF
)"
```

### 6c. Auto-Merge (if quality gate passed)
```bash
gh pr merge {PR_NUMBER} --merge --delete-branch
```

---

## Step 7: Final Report

```
## Batch Development Summary

Branch: feature/batch-{first}-{last}
PR: {URL}
Status: {Merged / Open for review}

| # | Issue | Commit | Status |
|---|-------|--------|--------|
| 1 | #{42} Title | abc1234 | Done |
| 2 | #{43} Title | def5678 | Done |
| 3 | #{44} Title | — | Skipped (reason) |

Quality Gate: Android Build ✓ | Tests ✓ | Detekt ✓ | SwiftLint ✓
```

Mark all tasks as `completed`.

---

## Error Handling

| Situation | Action |
|-----------|--------|
| Issue not found | Skip, warn in report |
| Circular dependency | Skip affected issues, report |
| Subagent implementation fails | Ask user: skip or stop |
| Tests fail after 3 attempts | Report to user, stop |
| Lint fails after 3 attempts | Report to user, stop |
| Figma link but fetch fails | Warn, continue without design |
| All issues blocked | Report, stop |
