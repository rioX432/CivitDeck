---
name: dev-all
description: "Process issues sequentially: /dev per issue in isolated sub-agent → CI wait → merge → next"
argument-hint: "[issue numbers, e.g. #42 #43 #44, or empty for all open issues]"
user-invocable: true
disable-model-invocation: true
allowed-tools:
  - Bash(git checkout:*)
  - Bash(git pull:*)
  - Bash(git log:*)
  - Bash(git status)
  - Bash(git branch:*)
  - Bash(gh pr create:*)
  - Bash(gh pr merge:*)
  - Bash(gh pr view:*)
  - Bash(gh pr checks:*)
  - Bash(gh issue view:*)
  - Bash(gh issue list:*)
  - Glob
  - Grep
  - Read
  - Agent
  - Skill
  - TaskCreate
  - TaskUpdate
  - TaskList
  - TaskGet
  - AskUserQuestion
---

# /dev-all — Sequential Issue Processing

Process multiple GitHub Issues sequentially. Each issue runs `/dev` in an isolated sub-agent, then waits for CI and merges before proceeding to the next.

**Arguments:** $ARGUMENTS

## Why Per-Issue (not Single Branch)?

Each issue gets its own branch, PR, and merge cycle:
- **Clean git history**: each PR is atomic and reviewable
- **CI validates each change independently**
- **Merge conflicts are impossible**: each issue starts from latest main
- **Rollback is easy**: revert a single PR, not a batch

---

## Step 0: Core Value Check (GATE)

1. Read the project's `CLAUDE.md` and look for `## Core Values` section
2. **If missing**: Warn the user that Core Values are undefined. Ask if they want to:
   - Define Core Values now (recommended)
   - Proceed without the filter (not recommended — risk of feature bloat)
3. If user chooses to proceed without, log a warning in the final report

---

## Step 1: Resolve Target Issues

**If `$ARGUMENTS` is provided:** Extract issue numbers.
**If empty:** Fetch all open issues:
```bash
gh issue list --state open --json number,title,labels,body --limit 100
```

### 1a. Filter Issues

- **Skip issues labeled `won't`** — these are explicitly decided not to implement
- **Skip issues listed in CLAUDE.md `## Won't Do`** — cross-reference issue titles

---

## Step 2: Parallel Investigation (Read-Only)

Launch **parallel Explore agents** (one per issue) to quickly understand scope:

Each agent:
1. `gh issue view {NUMBER} --json title,body,labels,comments`
2. Grep/Glob to find related code
3. Return: summary, affected files, estimated scope, dependencies

---

## Step 3: Dependency Analysis & Order

### 3a. Detect Dependencies
Check issue bodies for: `blocked by #N`, `depends on #N`, `after #N`

### 3b. Execution Order
Topological sort:
1. Independent issues first (ascending by number)
2. Dependent issues after their dependencies
3. Circular dependencies → skip, report

---

## ── AskUserQuestion: Execution Plan ──

Present:
1. Ordered list of issues
2. Dependencies detected
3. Skipped issues (with reasons — including `won't` label and Won't Do matches)
4. Estimated scope per issue
5. **Core Value alignment per issue** (if Core Values are defined)

Ask user to confirm before proceeding.

---

## Step 4: Sequential Issue Loop

Create a master task tracker:
```
TaskCreate for each issue: "#{number}: {title}"
```

### For each issue (in order):

#### 4a. Pull latest main
```bash
git checkout main && git pull origin main
```

#### 4b. Run /dev in isolated sub-agent (autonomous)
```
Agent(
  prompt: "/dev #{issue_number} — run in autonomous mode (no user confirmations).
    Definition of done, all evidence required in your final message:
    (1) the project's test command output (from CLAUDE.md Commands) showing its
        success signal, re-run after your last change;
    (2) review.json counts printed as text — \"critical\": 0 is required;
    (3) the PR URL.
    Constraints: do not modify or delete test files except those the issue
    explicitly requires — include `git diff --stat` in the final message to prove
    it. If the same failure recurs 3 times, stop and report the blocker instead.
    Finish by printing the Structured Return Value JSON.",
  model: "opus",
  isolation: "worktree"
)
```

> **Why no `/goal` inside the Agent() prompt?** `/goal` is a session-scoped
> Stop-hook wrapper. There is no official support for slash commands taking
> effect inside a sub-agent prompt, so a `/goal` there is likely inert text.
> Instead, the completion condition is stated as explicit instructions with
> evidence requirements, and Step 4b-result verifies the evidence rather than
> trusting the sub-agent's self-report. If a true evaluator loop per issue is
> needed, run the issue headlessly — `claude -p "/goal <condition>"` is
> officially supported.

The sub-agent:
- Gets a fresh context (no pollution from previous issues)
- Works in an isolated git worktree (no file conflicts)
- Runs the full /dev workflow autonomously
- Skips AskUserQuestion confirmations (proceeds with best judgment)
- Returns: structured result with PR URL, review status, and counts

#### 4b-result. Review Validation

After the sub-agent completes, validate the result before proceeding to merge. **Never trust the sub-agent's narrated success** — a claim of "tests pass, review clean" without evidence is the most common failure mode of long autonomous loops (proxy-signal collapse):

1. Read `workspace/{issue}/review.json` yourself to get the structured review output
2. Parse the sub-agent's return value for review status — and cross-check it against review.json; on mismatch, treat the issue as failed

**Decision logic:**

| Review Status | Action |
|---------------|--------|
| `critical` (critical_count > 0) | **Skip this issue.** Report to user: "#{issue} has {N} critical findings — skipping." Mark task as failed. Proceed to next issue. |
| `warnings` (warning_count > 0) | **Report to user.** `AskUserQuestion`: "#{issue} PR has {N} unresolved warnings. Merge anyway?" If yes → proceed. If no → skip. |
| `clean` | **Proceed to auto-merge.** |
| Sub-agent failed (`status: "failed"`) | **Skip this issue.** Report failure reason. Proceed to next issue. |

#### 4c. Enable auto-merge
```bash
gh pr merge {PR_URL} --auto --merge --delete-branch
```

#### 4d. Wait for merge
Poll until merged (check every 30 seconds, timeout 15 minutes):
```bash
STATE=$(gh pr view {PR_URL} --json state -q '.state')
```

If CI fails:
1. Report the failure to user
2. Ask: skip this issue and continue, or stop?

#### 4e. Mark task completed and proceed

---

## Step 5: Final Report

```
## Batch Development Summary

| # | Issue | PR | Status |
|---|-------|----|--------|
| 1 | #{42} Title | PR_URL | Merged |
| 2 | #{43} Title | PR_URL | Merged |
| 3 | #{44} Title | — | Skipped (CI failed) |

Completed: N / M issues
```

Mark all tasks `completed`.

---

## Autonomous Mode (/goal)

To run the entire batch under `/goal`, **derive the condition from the resolved issue list (Step 1)** — never wrap the raw request. The evaluator only reads transcript text, so the condition must reference output this skill actually prints (the final report table, `gh pr view` output):

```
/goal Every issue in {resolved issue list} is resolved or explicitly skipped: the
final report table, printed in the most recent turn, shows for each issue either a
merged PR (verified by `gh pr view --json state` output showing MERGED) or a skip
reason — or stop after {5 × issue count} turns or after 3 consecutive issue
failures, then summarize what is blocking. Constraints: do not close an issue
without a merged fix, and do not drop issues from the list to finish early.
```

Derivation rules:
- **End state ← Step 1's resolved issue list, locked at plan confirmation.** Do not remove issues mid-run to make the condition easier to satisfy (criteria laundering)
- **Proof ← the final report table + `gh pr view --json state` output**, re-printed in the most recent turn
- **Turn cap ← ~5 turns per issue**, joined as an OR-branch of the condition

In autonomous mode:
- Skip `AskUserQuestion` confirmations — proceed with best judgment
- On CI failure: skip the issue and continue (don't stop); record the skip reason in the report
- On 3 consecutive failures or at the turn cap: **stop on that turn** and print the blocking summary (the stop branch only completes the goal if the summary actually appears)

## Error Handling

| Situation | Action |
|-----------|--------|
| Issue not found | Skip, warn in report |
| Circular dependency | Skip affected issues, report |
| Sub-agent /dev fails | Ask user: skip or stop |
| CI fails | Ask user: skip or stop |
| Merge conflict | Ask user: skip or stop |
| 3 consecutive failures | Stop, report to user |
| Auto-merge timeout (15min) | Report, ask user |
