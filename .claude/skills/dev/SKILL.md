---
name: dev
description: "End-to-end: investigate → implement → test → review → PR"
argument-hint: "[GitHub issue #, e.g. #42, or Linear issue ID, e.g. PGR-1234]"
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
  - Bash(./gradlew *)
  - Bash(cd iosApp && swiftlint *)
  - Bash(gh pr create:*)
  - Bash(gh issue view:*)
  - Glob
  - Grep
  - Read
  - Edit
  - Write
  - Task
  - TaskCreate
  - TaskUpdate
  - TaskList
  - ToolSearch
  - AskUserQuestion
  - mcp__linear__get_issue
  - mcp__linear__list_comments
  - mcp__codex__codex
  - mcp__codex__codex-reply
  - mcp__figma-remote__get_design_context
  - mcp__figma-remote__get_screenshot
  - mcp__figma-remote__get_variable_defs
  - mcp__figma-remote__get_metadata
---

# /dev — End-to-End Development Workflow

Execute the full development cycle for an issue: investigate → implement → test → review → PR.

**Target:** "$ARGUMENTS"

## Setup: Create Task Tracker

Use `TaskCreate` to create a task for each phase. This provides:
- Progress visibility in the UI spinner
- Persistence across `/compact` and context compression
- Clear phase tracking for the user

Create these tasks at the start:
1. "Gather context from issue/Figma/docs"
2. "Investigate codebase"
3. "Cross-check with Codex"
4. "Implement changes"
5. "Run quality gate (test + detekt + swiftlint)"
6. "Commit changes"
7. "Review changes"
8. "Create PR"

Use `TaskUpdate` to mark each task `in_progress` when starting and `completed` when done.

---

## Phase 1: Context Gathering

### 1a. Issue Context

Detect the issue source from "$ARGUMENTS":

**GitHub Issue** (starts with `#`):
1. Run `gh issue view <number> --json number,title,body,labels`
2. Extract: title, description, acceptance criteria, labels

**Linear Issue** (matches `XXX-1234` pattern):
1. Use `ToolSearch` with `+linear` to load the Linear MCP
2. Call `mcp__linear__get_issue` with the issue ID
3. Extract: title, description, acceptance criteria, labels, priority

**Branch naming** (from labels or issue type):
- Bug → branch prefix `fix/`
- Feature/Improvement/anything else → branch prefix `feature/`
- Format: `{prefix}/{issue-ref}-{kebab-case-short-description}`

### 1b. Figma Design (if applicable)

Scan the issue description for Figma links (`figma.com/design/...` or `figma.com/file/...`).

If found:
1. Use `ToolSearch` with `+figma-remote` to load Figma Remote MCP
2. Extract `fileKey` and `nodeId` from the URL:
   - URL format: `https://www.figma.com/design/{fileKey}/...?node-id={nodeId}`
   - `nodeId` needs URL-decoding (`%3A` → `:` etc.)
3. Call `mcp__figma-remote__get_design_context` with `fileKey` and `nodeId` to get layout/styling info
4. Call `mcp__figma-remote__get_screenshot` to get visual reference
5. Store design context for use during implementation

### 1c. Project Context

Read the following files (use Read tool directly, not a subagent):
- `AGENTS.md` — architecture, commands, and conventions
- `docs/ARCHITECTURE.md` — module structure, data flow, design decisions

---

## Phase 2: Investigation

Use the `Task` tool with `subagent_type: "Explore"` and thoroughness `"very thorough"` to investigate the codebase. The prompt MUST include:

- The issue title, description, and acceptance criteria
- Keywords extracted from the issue
- Instruction: "Perform a very thorough investigation"

The subagent must:

1. **Find relevant code**: Use Grep/Glob to locate files matching the issue context
2. **Read the code**: Actually read every file involved — no speculation allowed
3. **Trace the flow**: Follow UI → ViewModel → UseCase → Repository
4. **Check existing tests**: Find related test files
5. **Impact analysis**: List files that need changes, callers, downstream dependencies
6. **Platform scope**: Determine if changes affect Android only, iOS only, or both (shared + platforms)

### No Speculation Principle
- Every finding must be backed by actual code reading
- If unsure about behavior, read callers and related files
- If still unclear, state "unverified" — never guess

### Think Twice
After completing analysis, re-evaluate:
1. Did I actually read the code confirming the root cause?
2. Are there other possible causes I haven't considered?
3. Is impact analysis complete?
4. Does this affect both platforms or just one?

### Unclear Points
If anything is ambiguous or uncertain after investigation, use `AskUserQuestion` to ask the user. Do NOT proceed with assumptions.

---

## Phase 3: Cross-Check (Codex)

Use `ToolSearch` with `+codex` to load Codex MCP, then call `mcp__codex__codex` with:

- `sandbox`: `read-only`
- `cwd`: project root
- `prompt`: Include the investigation results (root cause, impact, proposed approach) and ask Codex to verify:
  1. Is the root cause analysis correct?
  2. Is the impact analysis complete?
  3. Any alternative approaches not considered?

**Handling results:**
- Codex agrees → Proceed
- Codex disagrees → Re-investigate the specific disagreement, then present both perspectives
- Codex suggests alternatives → Evaluate and include if valid

**If Codex MCP is unavailable:** Skip this phase and note it was skipped. Proceed with extra scrutiny in self-review.

---

## ── AskUserQuestion: Approach Confirmation ──

Present to the user:

1. **Root cause** (with `file:line` references)
2. **Impact** (files affected, downstream effects)
3. **Proposed approach** (what changes, where)
4. **Codex verification** (agreed/disagreed, additional insights — or "skipped")

Ask the user to confirm the approach before proceeding to implementation.

If the user wants changes, adjust the plan accordingly.

---

## Phase 4: Branch & Implement

### 4a. Create Branch

```bash
git checkout -b {branch-name}
```

Use the branch name determined in Phase 1.

### 4b. Implement

Implement the changes using `Edit` and `Write` tools.

**Guidelines:**
- Follow existing code patterns (read surrounding code first)
- Follow AGENTS.md conventions (Clean Architecture + MVVM, Koin DI, UDF)
- If Figma design was fetched, reference the design context for UI implementation
- Keep changes minimal and focused — no unnecessary refactoring
- Android: use design tokens from `ui/theme/`, Coil 3.x for images
- iOS: use `DesignSystem/` tokens, `CachedAsyncImage` for images, feature-based structure

---

## Phase 5: Quality Gate

### 5a. Run Tests

```bash
./gradlew :shared:testDebugUnitTest
```

### 5b. Run Detekt (if Kotlin files changed)

```bash
./gradlew detekt
```

If detekt reports issues from import reordering, run it again (auto-correct may cause cascading fixes).

### 5c. Run SwiftLint (if Swift files changed)

```bash
cd iosApp && swiftlint --strict
```

### Failure Handling

If tests or linters fail:
1. Analyze the failure
2. Fix the issue
3. Re-run the failing check
4. **Maximum 3 fix attempts** — if still failing after 3 tries, report to user with:
   - What failed
   - What was tried
   - Remaining error details
   Then **stop and wait for user guidance**.

---

## ── AskUserQuestion: Commit Confirmation ──

Show the user:
1. Summary of all changes made (files modified/created)
2. Proposed commit message

Commit message format:
- One line, concise
- No `Co-Authored-By`, no AI stamps
- Example: `Fix recommended outfit not showing on gacha screen re-entry`

Ask the user to confirm before committing.

---

## Phase 6: Commit

Only after user confirmation:

```bash
git add {specific files}
git commit -m "{message}"
```

- Add files explicitly (no `git add .` or `git add -A`)
- No Co-Authored-By, no AI stamps

---

## Phase 7: Review

Run **parallel** review checks using `Task` tool with `subagent_type: "general-purpose"` and `model: "sonnet"`.

Launch these agents in parallel (all in a single message):

### 7a. Architecture Review
Prompt: Review the changes for MVVM compliance, layer boundaries (data/domain/UI), dependency direction, Koin DI usage. Reference AGENTS.md conventions.

### 7b. Android Review (only if Kotlin UI files changed)
Prompt: Review Compose changes for recomposition safety, state hoisting, side effect key correctness, modifier ordering. Check Coil usage patterns and Navigation 3 correctness.

### 7c. iOS Review (only if Swift files changed)
Prompt: Review SwiftUI changes for proper use of `CachedAsyncImage` (not third-party libs), design token usage from `DesignSystem/`, feature-based structure compliance. Check for iOS 16+ compatibility (no iOS 18+ APIs).

### 7d. Test Coverage Check
Prompt: Check if new/modified logic has corresponding tests. Flag untested paths.

### Review Result Handling

Collect all review results. Categorize findings:

- **Critical** (crashes, memory leaks, data loss, security): **STOP. Report to user immediately.** Do NOT proceed to PR.
- **Important** (performance, incorrect behavior, missing error handling): Fix these, then re-run Quality Gate (Phase 5)
- **Suggestions** (style, naming): Note but do not block

If Critical issues are found:
1. Present all critical findings to the user
2. Wait for user guidance
3. Do NOT proceed to PR creation

If Important issues are found:
1. Fix them
2. Re-run Quality Gate
3. Amend the commit or create a new fix commit (ask user preference)

---

## ── AskUserQuestion: PR Creation Confirmation ──

Show the user:
1. Review summary (all findings and resolutions)
2. Branch name
3. Proposed PR title
4. Target branch for the PR

Ask the user to confirm before creating the PR.

---

## Phase 8: PR Creation

Only after user confirmation:

### 8a. Push

```bash
git push -u origin {branch-name}
```

### 8b. Create PR

Use the project's pull_request_template.md. Only fill in Description and Related Issues.

```bash
gh pr create --title "{PR title}" --body "$(cat <<'EOF'
## Description

- {bullet point summary of changes}

## Related Issues

{issue reference, e.g. Closes #42}

## Screenshots / Video

<!-- If applicable, add screenshots or screen recordings -->

## Test Plan

- [x] {how it was tested, e.g. "Ran detekt — zero issues"}
- [x] {e.g. "Verified on Android emulator"}

## Review Checklist

- [x] Code follows project architecture (Clean Architecture + MVVM)
- [x] Shared logic is in `commonMain`, platform-specific code only when necessary
- [x] No unnecessary dependencies added
- [x] Tests added/updated as needed

## Breaking Changes

None
EOF
)"
```

### 8c. Output

Print the PR URL so the user can review it.

---

## Error Handling Summary

| Situation | Action |
|-----------|--------|
| Issue not found | Report error, stop |
| Figma link found but fetch fails | Warn, continue without design context |
| Investigation unclear | AskUserQuestion before proceeding |
| Codex unavailable | Skip cross-check, note it was skipped |
| Codex disagrees with analysis | Re-investigate, present both perspectives |
| Tests fail (≤3 attempts) | Fix and retry |
| Tests fail (>3 attempts) | Report to user, stop |
| Critical review finding | Report to user, stop |
| Important review finding | Fix, re-run quality gate |
| Git/PR creation fails | Report error, stop |
