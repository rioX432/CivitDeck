---
name: dev
description: "E2E development: investigate → dig → decompose → implement → test → review → PR"
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
---

# /dev — E2E Development Workflow

Resolve Issue $ARGUMENTS from investigation to PR creation.

**Target:** $ARGUMENTS

## Setup: Create Task Tracker

Use `TaskCreate` to create a task for each phase. This provides progress visibility and persistence across `/compact`.

1. "Gather context from issue"
2. "Investigate codebase"
3. "Resolve ambiguities (/dig)"
4. "Decompose into subtasks (/decompose)"
5. "Implement changes"
6. "Run quality gate"
7. "Review changes"
8. "Commit & create PR"

Use `TaskUpdate` to mark each task `in_progress` when starting and `completed` when done.

## Workflow

```
Phase 1: Issue Understanding
    ↓
Phase 2: Investigation (← Explore subagent)
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

## Phase 2: Investigation (Subagent)

Mark task 2 `in_progress`.

Delegate to Explore agent:

```
Agent(
  subagent_type: "Explore",
  prompt: <include issue details, keywords, ask for "very thorough" investigation>
)
```

The investigator must:
1. Find relevant code with Grep/Glob
2. Actually read every involved file — no speculation
3. Trace the data flow end-to-end
4. Check existing tests
5. List files needing changes, callers, downstream dependencies

### Think Twice

After receiving the report:
1. Did the investigator actually read the code?
2. Are there other possible causes not considered?
3. Is impact analysis complete?

If anything is ambiguous, use `AskUserQuestion`. **Never assume.**

Mark task 2 `completed`.

---

## Phase 3: Ambiguity Resolution

Mark task 3 `in_progress`.

Use the `/dig` skill with investigation results to resolve decision points.

Mark task 3 `completed`.

---

## Phase 4: Task Decomposition

Mark task 4 `in_progress`.

Use the `/decompose` skill to break the work into ordered subtasks.

Mark task 4 `completed`.

---

## ── AskUserQuestion: Approach Confirmation ──

Present to the user:
1. **Decision Matrix** (from /dig)
2. **Task List** (from /decompose, with dependencies)
3. **Investigation summary** (key findings)

Ask the user to confirm before implementation.

---

## Phase 5: Branch & Implement

Mark task 5 `in_progress`.

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

Mark task 5 `completed`.

---

## Phase 6: Quality Gate

Mark task 6 `in_progress`.

Run the project's build, test, and lint commands as defined in CLAUDE.md's Commands section.

If CLAUDE.md doesn't specify commands, detect from project files:
- `build.gradle.kts` / `gradlew` → `./gradlew build`, `./gradlew test`, `./gradlew detekt`
- `package.json` → `npm test`, `npm run lint`
- `Cargo.toml` → `cargo build`, `cargo test`, `cargo clippy`
- `pyproject.toml` / `setup.py` → `pytest`, `ruff check`

### Failure Handling
1. Analyze the failure
2. Fix the issue
3. Re-run the failing check
4. **Maximum 3 fix attempts** — if still failing, report to user and stop

Mark task 6 `completed`.

---

## Phase 7: Review

Mark task 7 `in_progress`.

Use the `/review` skill to run multi-agent parallel review.

### Review Result Handling
- **Critical**: STOP. Report to user. Do NOT proceed.
- **Warning**: Fix, re-run Quality Gate (Phase 6)
- **Suggestion**: Note but don't block

Mark task 7 `completed`.

---

## ── AskUserQuestion: Commit + PR Confirmation ──

Show the user:
1. Summary of all changes
2. Quality gate results
3. Review findings and resolutions
4. Proposed commit message (single line, no AI stamps)

---

## Phase 8: Commit & PR Creation

Mark task 8 `in_progress`.

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

Mark task 8 `completed`.

---

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
