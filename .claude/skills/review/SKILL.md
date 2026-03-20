---
name: review
description: "Multi-agent parallel code review against the base branch"
user-invocable: true
allowed-tools:
  - Read
  - Glob
  - Grep
  - Agent
  - Bash(git diff:*)
  - Bash(git log:*)
  - Bash(git status)
  - Bash(gh pr view:*)
---

# /review — Multi-Agent Code Review

Review the current branch's changes against the base branch using parallel review agents.

## Step 0: Prepare

1. Identify base branch (`main` or `master`) and current branch
2. `git log` — commits on this branch
3. `git diff {base}...HEAD` — full changeset
4. `gh pr view --json body` — PR description (if available)
5. Run each command individually — do NOT chain with `&&`

## Step 1: Build Change Context

1. Understand the **intent** from commit messages and PR description
2. Categorize changed files: core logic, UI, infrastructure, tests
3. Identify **risk areas**: complex changes, new integrations, security-sensitive code

## Step 2: Multi-Agent Parallel Review

Launch **two review agents in parallel** (model: sonnet):

### Agent A: Bug & Logic + Security
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

### Agent B: Architecture & Quality
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

### Project-Specific Reviewers (if available)

Check `.claude/agents/` for project-specific reviewer agents (e.g., `kmp-reviewer`, `ui-reviewer`). If found, launch them in parallel with Agents A and B.

## Step 3: Merge Findings

1. Collect findings from all agents
2. **Deduplicate**: remove findings reported by multiple agents
3. Assign final severity:
   - **Critical**: crash, data loss, security vulnerability, incorrect behavior
   - **Warning**: potential bug, performance issue, architecture violation
   - **Suggestion**: improvement opportunity, non-blocking
   - **Nit**: style/preference, optional

## Step 4: Present Report

```
## Review Summary

**Branch:** {current} → {base}
**Files changed:** N
**Risk level:** Low / Medium / High
**Reviewed by:** Agent A (Bug/Security) + Agent B (Arch/Quality) + {project-specific}

### Critical (must fix)
- [file:line] description

### Warning (should fix)
- [file:line] description

### Suggestion (nice to have)
- [file:line] description

### Nit (optional)
- [file:line] description
```
