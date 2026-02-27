---
name: dev-all
description: "Parallel multi-issue development with worktree isolation and conflict prevention"
argument-hint: "[issue numbers, e.g. #42 #43 #44, or empty for all open issues]"
user-invocable: true
disable-model-invocation: true
allowed-tools:
  - Bash(git *)
  - Bash(./gradlew *)
  - Bash(cd iosApp && swiftlint *)
  - Bash(gh *)
  - Bash(plutil *)
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
  - AskUserQuestion
---

# /dev-all — Parallel Multi-Issue Development

Develop multiple GitHub issues in parallel using worktree-isolated subagents with dependency awareness and **conflict prevention**.

**Target issues:** "$ARGUMENTS"

---

## Phase 0: Resolve Target Issues

**If arguments provided:** Extract issue numbers (e.g. `#42 #43 #44` or `42 43 44`).

**If empty:** Fetch all open issues:

```bash
gh issue list --state open --json number,title,labels --limit 100
```

Present issues grouped by phase/label and ask user which to develop.

---

## Phase 1: Context Collection (Parallel)

### 1a. Fetch All Issues in Parallel

Launch parallel `Task` subagents (one per issue, `subagent_type: "Bash"`, `model: "haiku"`) to fetch:

```
gh issue view <number> --json number,title,body,labels
```

### 1b. Read Project Context

Read directly (not via subagent):
- `AGENTS.md`
- `docs/ARCHITECTURE.md`

---

## Phase 2: Dependency & Conflict Analysis

### 2a. Detect Dependencies

1. **Explicit dependencies** — "depends on #X", "blocked by #X"
2. **Code-level dependencies** — use `Task(subagent_type: "Explore")` to scan which files each issue likely touches

### 2b. Build Conflict Map

**CRITICAL STEP** — Identify shared "accumulation point" files that multiple issues will modify:

| Accumulation Point | Typical Conflicts |
|---|---|
| `CivitDeckDatabase.kt` | DB migration version numbers |
| `UserPreferencesEntity.kt` | New preference columns |
| `UserPreferencesRepository.kt` | New interface methods |
| `DomainModule.kt` | Use case registrations |
| `KoinHelper.kt` (iOS) | Getter methods |
| `CivitDeckApplication.kt` | ViewModel/DI registrations |
| `SettingsScreen` (both) | New settings sections |
| `ModelSearchScreen` (both) | UI modifications |
| `project.pbxproj` | New file entries |
| Test fakes | Interface implementation stubs |

For each accumulation point, list which issues will touch it.

### 2c. Pre-Assign Shared Resources

**DB Migration Versions:**
1. Check current DB version: `grep "version =" shared/src/commonMain/.../CivitDeckDatabase.kt`
2. Assign sequential versions to each issue that adds DB columns:
   - Issue A: N → N+1
   - Issue B: N+1 → N+2
   - Issue C: N+2 → N+3

**Accumulation Point Insertion Order:**
For files like DomainModule.kt, KoinHelper.kt, CivitDeckApplication.kt:
- Assign each issue a **comment-delimited section** to add its code
- Example: `// region #122 Offline` ... `// endregion #122`
- This prevents git merge conflicts by ensuring non-overlapping line ranges

### 2d. Build Execution Graph

- **Independent issues**: No dependencies → parallel
- **Dependent issues**: Must wait
- **Soft conflicts**: Issues touching same screens → prefer sequential or provide explicit coordination

### 2e. Create Task Tracker

`TaskCreate` for each issue with dependencies via `addBlockedBy`.

---

## Phase 3: Parallel Execution

### Execution Strategy

1. Launch independent issues as worktree-isolated subagents (max 5 parallel)
2. Monitor completion via `TaskOutput`
3. Launch dependent issues as their dependencies complete

### Subagent Prompt Template

Each subagent receives:

```markdown
You are developing a single GitHub issue for the CivitDeck project (KMP: Android + iOS).

## Issue
- Number: #{number}
- Title: {title}
- Description: {description}
- Labels: {labels}

## Project Context
{AGENTS.md summary and ARCHITECTURE.md summary}

## Key Rules
- Read CLAUDE.md, AGENTS.md, and .claude/rules/ for conventions
- Follow Clean Architecture + MVVM, Koin DI, UDF
- No guessing — read code before modifying
- Design tokens: Android `ui/theme/`, iOS `DesignSystem/`
- Coil 3.x for Android images, `CachedAsyncImage` for iOS
- iOS deployment target 16.0 — no iOS 18+ APIs
- Detekt LongMethod limit is 60 lines
- All code in English, no AI stamps

## Conflict Prevention — IMPORTANT
{conflict_instructions}

## Your Workflow

### Step 1: Investigate
- Read CLAUDE.md, AGENTS.md, .claude/rules/
- Glob/Grep/Read relevant code
- Trace: UI → ViewModel → UseCase → Repository
- Determine platform scope

### Step 2: Implement
- Create branch: `{branch-name}`
- Follow conflict prevention instructions exactly
- Keep changes minimal and focused

### Step 3: Quality Gate
- `./gradlew :shared:testDebugUnitTest`
- `./gradlew detekt` (twice if import reorder)
- `cd iosApp && swiftlint --strict`
- Max 3 fix attempts

### Step 4: Commit & Push & PR
- `git add {specific files}` (no `git add .`)
- `git commit -m "{message}"`
- `git push -u origin {branch-name}`
- `gh pr create` with Description, Related Issues, Test Plan, Review Checklist

Report: branch name, PR URL, changes summary, issues encountered.
```

### Conflict Prevention Instructions (per issue)

The `{conflict_instructions}` section is **unique per issue** and contains:

```markdown
### DB Migration
- Use migration version: {N} → {N+1}
- Schema version: {N+1}
- Migration name: MIGRATION_{N}_{N+1}
- Add columns: {list of columns for this issue only}

### DomainModule.kt
- Add your use case registrations AFTER the comment `// {previous-issue-marker}`
- Add a comment: `// region #{number} {title}` before your registrations

### KoinHelper.kt (iOS)
- Add your getter methods AFTER existing getters
- Use comment: `// #{number} {title}` above your additions

### CivitDeckApplication.kt
- When modifying ViewModel registrations, add parameters at the END of existing parameter lists
- Do NOT reorder existing parameters

### SettingsScreen (both platforms)
- Add your settings section AFTER the "{previous_section}" section
- Use a clear section header/label

### ModelSearchScreen (if applicable)
- Only modify the specific area described below:
  {specific_modification_instructions}

### pbxproj
- For new files, always add all 4 entries (PBXBuildFile, PBXFileReference, group children, PBXSourcesBuildPhase)
- Use UUIDs that start with a unique prefix: CD{issue_short_id}
```

---

## Phase 4: Collect Results & Report

### 4a. Update Tasks
Mark all tasks completed or note failures.

### 4b. CI Monitoring
Check CI status for each PR:
```bash
gh pr list --state open --json number,title,statusCheckRollup
```
Fix any CI failures by resuming the relevant subagent or launching a fix agent.

### 4c. Summary Report

```
## Development Summary

| Issue | Branch | PR | CI | Status |
|-------|--------|----|-----|--------|
| #42 Title | fix/42-xxx | #PR_URL | Pass | Done |
```

### 4d. Merge Order Recommendation

Output recommended merge order based on:
1. Independence (least conflicts first)
2. DB migration version sequence
3. Dependency chain

---

## Error Handling

| Situation | Action |
|-----------|--------|
| Issue not found | Skip, warn in summary |
| Circular dependency | Report, stop |
| Subagent fails after 3 retries | Mark failed, continue others |
| DB migration conflict detected | Pre-assigned versions prevent this |
| Accumulation point conflict | Section markers prevent this |
| CI failure | Launch fix agent targeting specific error |
| Worktree conflict | Report in summary, suggest resolution |

---

## Constraints

- Maximum **5 parallel subagents**
- Each subagent gets its own worktree
- Dependent issues wait for dependencies to complete
- Conflict prevention instructions are mandatory for every subagent
