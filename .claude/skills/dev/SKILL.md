---
name: dev
description: "End-to-end development for one issue: investigate → design → resolve ambiguities → decompose → implement → test → review → PR. Reads GitHub or Linear issues; writes branches and pull requests to GitHub via the gh CLI."
argument-hint: "[issue number or ID, e.g. #42 or PGR-1234]"
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
  - Bash(gh pr create:*)
  - Bash(gh issue view:*)
  - Glob
  - Grep
  - Read
  - Edit
  - Write
  - Agent
  - Skill
  - TaskCreate
  - TaskUpdate
  - TaskList
  - TaskGet
  - ToolSearch
  - AskUserQuestion
  - mcp__codex__codex
---

# /dev — E2E Development Workflow

Resolve Issue $ARGUMENTS from investigation to PR creation.

**Target:** $ARGUMENTS

## Setup: Create Task Tracker

Use `TaskCreate` to create a task for each phase. This provides progress visibility and persistence across `/compact`.

1. "Gather context from issue"
2. "Investigate codebase (/dev-investigate)"
3. "Technical design (Codex)"
4. "Resolve ambiguities (/dig)"
5. "Decompose into subtasks (/decompose)"
6. "Implement changes"
7. "Run quality gate"
8. "Review changes"
9. "Commit & create PR"

Use `TaskUpdate` to mark each task `in_progress` when starting and `completed` when done.

## Workflow

```
Phase 1: Issue Understanding
    ↓
Phase 2: Investigation (← /dev-investigate, context: fork)
    ↓
Phase 2.5: Technical Design (← Codex, optional)
    ↓
Phase 3: Ambiguity Resolution (/dig)
    ↓
Phase 4: Task Decomposition (/decompose)
    ↓
── AskUserQuestion: confirm approach + task list ──
    ↓
Phase 5: Branch & Implement
    ↓
Phase 6: Quality Gate (build + test + lint from CLAUDE.md)
    ↓
Phase 7: Review (/review)
    ↓
── AskUserQuestion: commit + PR confirmation ──
    ↓
Phase 8: Commit & PR Creation
```

---

## Phase 1: Issue Understanding

Mark task 1 `in_progress`.

Detect the issue source from "$ARGUMENTS":

**GitHub Issue** (starts with `#` or is a number):
1. `gh issue view <number> --json number,title,body,labels,assignees,comments`
2. Extract: title, description, acceptance criteria, labels

**Linear Issue** (matches `XXX-1234` pattern):
1. Use `ToolSearch` with `+linear` to load the Linear MCP
2. Call `mcp__linear__get_issue` with the issue ID

**Figma links** in the issue description (`figma.com/design/...`):
1. Use `ToolSearch` with `+figma-remote` to load Figma MCP
2. Fetch design context and screenshot

### Sizing Gate

Before investigating, check that the issue is one executable unit. Run the sizing gate from the
`issue` skill against the issue body (`skills/issue/SKILL.md → Step 3`).

If the issue fails a check — an `and` in the title, an unresolved design choice, a task list of
ten boxes, an `epic` label — **stop and split before implementing**:

```
Skill("issue", args: "Re-size #{issue}: {quoted text that failed the gate}")
```

Then run `/dev` against one of the resulting children. An oversized issue is the single largest
cause of an unreviewable PR and of `/goal` runs that never terminate.

In autonomous mode, a failed gate is a **stop condition**, not something to work around.

**Branch naming** (from labels or issue type):
- Bug → `fix/{issue-ref}-{kebab-case-short-desc}`
- Otherwise → `feat/{issue-ref}-{kebab-case-short-desc}`

Read `CLAUDE.md` for project architecture and conventions.

### Migration Detection

If the issue involves library/framework updates, version bumps, or API deprecations:
1. Check labels for `migration`, `upgrade`, `dependency`, or `breaking-change`
2. If Android/KMP project and `android` CLI is available: run `android docs search "{migration topic}"` to get latest official migration guidance
3. Note migration-specific risks (breaking changes, deprecated APIs) for Phase 3 (/dig)

Mark task 1 `completed`.

---

## Phase 2: Investigation (/dev-investigate)

Mark task 2 `in_progress`.

Invoke the `/dev-investigate` skill to run investigation in a forked context. This keeps large Read results isolated from this context.

```
Skill("dev-investigate", args: "{issue title}. {issue description}. Keywords: {keywords}. Affected areas: {areas from labels/description}")
```

After the skill completes, read the investigation report:

```
Read("investigation-report.md")
```

### Think Twice

After reading the report:
1. Is the investigation thorough? All affected files identified?
2. Are there other possible causes not considered?
3. Is impact analysis complete?

If anything is ambiguous, use `AskUserQuestion`. **Never assume.**

Mark task 2 `completed`.

---

## Phase 2.5: Technical Design (Codex)

Mark task 3 `in_progress`.

Use Codex to generate a technical design before ambiguity resolution and decomposition.
Follow the call pattern and fallback in `rules/behavior.md → Call pattern`.

**Give Codex**: the issue title and description, the investigation summary, the affected-files table.

**Ask Codex for**:
1. The implementation approach (architecture, data flow changes)
2. Interface changes and new abstractions needed
3. Edge cases and error handling strategy
4. Risks and trade-offs

Save the output for Phase 3 (`/dig`) and Phase 4 (`/decompose`).

Mark task 3 `completed`.

---

## Phase 3: Ambiguity Resolution

Mark task 4 `in_progress`.

Use the `/dig` skill with investigation results (and Codex design if available) to resolve decision points.

Mark task 4 `completed`.

---

## Phase 4: Task Decomposition

Mark task 5 `in_progress`.

Use the `/decompose` skill to break the work into ordered subtasks.

Mark task 5 `completed`.

---

## ── AskUserQuestion: Approach Confirmation ──

Present to the user:
1. **Decision Matrix** (from /dig)
2. **Task List** (from /decompose, with dependencies)
3. **Investigation summary** (key findings)

Ask the user to confirm before implementation.

---

## Phase 5: Branch & Implement

Mark task 6 `in_progress`.

### 5a. Create Branch

```bash
git checkout -b {branch-name}
```

### 5b. Implement

**TDD mode** (when issue has `tdd` label, or test changes are the primary goal):
```
LOOP for each subtask:
  1. TaskUpdate → in_progress
  2. Read target code
  3. Write/update tests FIRST (use test-writer agent if needed)
  4. Run tests — confirm they FAIL (red)
  5. Implement the minimal code to pass
  6. Run tests — confirm they PASS (green)
  7. Refactor if needed (keep tests passing)
  8. TaskUpdate → completed
```

**Standard mode** (default):
```
LOOP for each subtask (in dependency order):
  1. TaskUpdate → in_progress
  2. Read target code (MUST read before editing)
  3. Implement changes (Edit/Write)
  4. Self-verify (run Verify step from task description)
  5. TaskUpdate → completed

INTERRUPT conditions:
  - Unexpected problem → AskUserQuestion
  - 3 consecutive failures → STOP and report
```

Guidelines:
- Follow existing code patterns (read surrounding code first)
- Follow CLAUDE.md conventions
- Keep changes minimal and focused

Mark task 6 `completed`.

---

## Phase 6: Quality Gate

Mark task 7 `in_progress`.

Run the project's build, test, and lint commands as defined in CLAUDE.md's Commands section.

If CLAUDE.md doesn't specify commands, detect from project files:
- `build.gradle.kts` / `gradlew` → `./gradlew build`, `./gradlew test`, `./gradlew detekt`
- `package.json` → `npm test`, `npm run lint`
- `Cargo.toml` → `cargo build`, `cargo test`, `cargo clippy`
- `pyproject.toml` / `setup.py` → `pytest`, `ruff check`
- `pubspec.yaml` → `flutter test` (or `dart test`), `dart analyze`

### Failure Handling
1. Analyze the failure
2. Fix the issue
3. Re-run the failing check
4. **Maximum 3 fix attempts** — if still failing, report to user and stop

Mark task 7 `completed`.

---

## Phase 7: Review

Mark task 8 `in_progress`.

Use the `/review` skill to run multi-agent parallel review.

### Structured Review Output

After the review completes, write `workspace/{issue}/review.json`:

```json
{
  "issue": "{issue reference}",
  "branch": "{branch name}",
  "timestamp": "{ISO 8601}",
  "status": "clean | warnings | critical",
  "counts": {
    "critical": 0,
    "warning": 0,
    "suggestion": 0,
    "nit": 0
  },
  "findings": [
    {
      "severity": "critical | warning | suggestion | nit",
      "file": "path/to/file",
      "line": 42,
      "description": "description of finding",
      "resolved": false
    }
  ]
}
```

### Review Result Handling
- **Critical**: STOP. Report to user. Do NOT proceed.
- **Warning**: Fix, re-run Quality Gate (Phase 6). Update `review.json` findings as `"resolved": true`.
- **Suggestion**: Note but don't block

Mark task 8 `completed`.

---

## ── AskUserQuestion: Commit + PR Confirmation ──

Show the user:
1. Summary of all changes
2. Quality gate results
3. Review findings and resolutions
4. Proposed commit message (single line, no AI stamps)

---

## Phase 8: Commit & PR Creation

Mark task 9 `in_progress`.

### 8a. Commit
```bash
git add {specific files}
git commit -m "{concise message}"
```
- Explicit file staging (no `git add .`)
- No Co-Authored-By, no AI stamps

### 8b. Push & PR
```bash
git push -u origin {branch-name}
```

Use the project's `pull_request_template.md` if available. Only fill in Description and Related Issues.

```bash
gh pr create --title "#{issue} {description}" --body "$(cat <<'EOF'
## Description
- {bullet point summary}

## Related Issues
Closes #{issue}
EOF
)"
```

Report PR URL to the user.

Mark task 9 `completed`.

---

## Autonomous Mode (/goal)

When the user invokes `/dev` under a `/goal`, the workflow runs autonomously. **Do not wrap the raw request in `/goal`** — build the condition from the repo per `rules/ai-ops.md → /goal for Autonomous Execution`. The `/goal` evaluator cannot run tools; it only reads what is printed in the transcript, so the condition must name real commands and their exact success output.

Condition template (resolve `{test command}` and `{success signal}` from CLAUDE.md's Commands section — never guess):

```
/goal Issue $ARGUMENTS is resolved: in the most recent turn, {test command} was run
and its output shows {success signal, e.g. "0 failed" / "BUILD SUCCESSFUL"}, the
review step printed review.json counts showing "critical": 0, and the PR URL was
printed — or stop after 25 turns or if the same failure recurs 3 times, then
summarize the blocker. Constraints: do not modify or delete test files except those
the issue explicitly requires — show `git diff --stat` each turn.
```

In autonomous mode:
- Skip `AskUserQuestion` confirmations — proceed with best judgment
- Stop on Critical review findings or 3 consecutive failures (these still require human input)
- **Surface fresh evidence every turn**: after any code change, re-run the failing check and show its output — the evaluator discounts evidence that predates the last change
- **Print `review.json` counts as text** after Phase 7 — the evaluator cannot read files
- **If the turn cap is reached, stop on that turn** and print the blocker summary; do not keep working past the cap

### Structured Return Value

On completion, output a structured result for callers (e.g., `/dev-all`):

```json
{
  "issue": "{issue reference}",
  "status": "success | failed | blocked",
  "pr_url": "https://github.com/owner/repo/pull/N",
  "review": {
    "status": "clean | warnings | critical",
    "critical_count": 0,
    "warning_count": 0
  },
  "failure_reason": null
}
```

### AskUserQuestion Skip Rules

In autonomous mode, `AskUserQuestion` is skipped when:
1. **Approach confirmation** (Phase 4): Proceed with the decomposed task list
2. **Commit + PR confirmation** (Phase 8): Proceed if review status is `clean` or `warnings` (no unresolved criticals)

`AskUserQuestion` is NOT skipped when:
1. **Critical review findings**: Always stop and report
2. **3 consecutive failures**: Always stop and report
3. **Unexpected errors**: Always stop and report

## Error Handling

| Situation | Action |
|-----------|--------|
| Issue not found | Report error, stop |
| Figma fetch fails | Warn, continue without design |
| Investigation unclear | AskUserQuestion before proceeding |
| Tests fail (≤3 attempts) | Fix and retry |
| Tests fail (>3 attempts) | Report to user, stop |
| Critical review finding | Report to user, stop |
| Warning review finding | Fix, re-run quality gate |
| Git/PR creation fails | Report error, stop |
