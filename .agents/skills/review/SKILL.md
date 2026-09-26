---
name: review
description: "Review the current branch against its base with parallel agents for bugs/security and architecture/quality, producing severity-ranked findings. Use before committing or opening a PR."
---

> **Host adaptation:** Treat product-specific tool names as capabilities. Use the
> equivalent tools available in the current host, ask through its interaction mechanism,
> and skip optional integrations that are unavailable. Resolve repository guidance from
> `AGENTS.md`, falling back to `CLAUDE.md` only when needed. `$ARGUMENTS` means the current
> user request; `${CLAUDE_SKILL_DIR}` means the directory containing this skill.

# /review — Multi-Agent Code Review

Review the current branch's changes against the base branch using parallel review agents.

## Step 0: Prepare

1. Resolve the base branch: the open PR's base (`gh pr view --json baseRefName`), otherwise the remote default branch (`git symbolic-ref refs/remotes/origin/HEAD`). Do not assume `main`
2. `git log {base}..HEAD` — commits on this branch
3. Build the changeset from both sources — `/dev` reviews before it commits, so the working tree usually holds most of the change:
   - `git diff {base}...HEAD` — committed changes
   - `git diff HEAD` — staged and unstaged changes
   - `git status --short` — untracked files; read the new source files in full
4. `gh pr view --json body` — PR description (if available)
5. Run each command individually — do NOT chain with `&&`

If both diffs are empty and there are no untracked source files, report "nothing to review" and stop.

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
3. **Verify every Critical and Warning** before it is reported: re-read the cited code and its callers and confirm the failure scenario actually occurs (concrete input or state → wrong result). Drop a finding the code refutes; downgrade one you cannot confirm to Suggestion and say so. A Critical stops `/dev` and makes `/dev-all` skip the issue, so an unverified Critical costs a whole run
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
