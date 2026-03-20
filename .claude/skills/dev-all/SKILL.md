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

## Step 1: Resolve Target Issues

**If `$ARGUMENTS` is provided:** Extract issue numbers.
**If empty:** Fetch all open issues:
```bash
gh issue list --state open --json number,title,labels,body --limit 100
```

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
3. Skipped issues (with reasons)
4. Estimated scope per issue

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

#### 4b. Run /dev in isolated sub-agent
```
Agent(
  prompt: "/dev #{issue_number}",
  model: "opus",
  isolation: "worktree"
)
```

The sub-agent:
- Gets a fresh context (no pollution from previous issues)
- Works in an isolated git worktree (no file conflicts)
- Runs the full /dev workflow autonomously
- Returns: PR URL (or error report)

#### 4c. Enable auto-merge
```bash
gh pr merge {PR_URL} --auto --squash --delete-branch
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
