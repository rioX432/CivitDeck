---
name: dev-investigate
description: "Investigate codebase for an issue in a forked context (context isolation)"
context: fork
allowed-tools:
  - Read
  - Glob
  - Grep
  - Bash(git log:*)
  - Bash(git diff:*)
  - Bash(git blame:*)
  - Bash(find:*)
  - Agent
  - ToolSearch
---

# /dev-investigate — Forked-Context Investigation

Investigate the codebase for a given issue in a forked context. This skill runs with `context: fork` so that large Read results do not pollute the caller's context window.

**Input:** $ARGUMENTS (issue details, keywords, affected areas from Phase 1)

## Why Fork?

Investigation reads many files to trace data flows and map dependencies. Running in a forked context keeps this token-heavy work isolated — the caller only receives the final `investigation-report.md`, not hundreds of file reads.

---

## Step 1: Parse Input

Extract from `$ARGUMENTS`:
- Issue summary and acceptance criteria
- Keywords and technical terms
- Known affected areas (from issue labels, description)
- Project architecture context (from CLAUDE.md)

Read `CLAUDE.md` for project architecture, conventions, and directory structure.

---

## Step 2: Find Entry Points

1. **Grep** for keywords from the issue (class names, function names, error messages)
2. **Glob** for likely file patterns (feature directories, module names)
3. **git log** for recent changes in related areas
4. Identify 3-6 entry points to start deep investigation

---

## Step 3: Deep Investigation (Explore Agents)

Launch parallel Explore agents for each investigation axis:

### Agent template

```
Agent(
  subagent_type: "Explore",
  prompt: "Investigate {axis} for {issue} in this codebase.

  Entry points: {identified files/symbols}

  You MUST:
  1. Read every relevant file — no guessing
  2. Trace calls and data flow through actual code paths
  3. Note public API surfaces and internal boundaries
  4. Check for tests — list what's tested and what's not
  5. List all files involved with their role

  Report: structured findings with file:line references."
)
```

### Investigation axes (select 2-4 based on issue type):

- **Code structure**: Where does the relevant code live? What layer/module?
- **Data flow**: How does data enter, transform, and exit?
- **Dependencies**: What depends on this? What does this depend on?
- **Test coverage**: What's tested? What's not? Where are the test files?
- **History**: Recent changes, who touched it, related PRs (`git log`, `git blame`)

### Investigation checklist

For each axis, the agent must cover:

- [ ] **Read the code**: Every involved file, not just entry points
- [ ] **Trace the flow**: Follow function calls, event handlers, data transformations
- [ ] **Map boundaries**: Module boundaries, public vs internal APIs
- [ ] **Check tests**: Existing test files, what's covered, what's missing
- [ ] **Check history**: `git log` / `git blame` for recent changes and context

---

## Step 4: Think Twice

After receiving agent reports, verify:
1. Did the agents actually read the code, or did they speculate?
2. Are there other possible causes or code paths not considered?
3. Is impact analysis complete — all callers, all downstream dependencies?
4. Do findings from different axes contradict each other?

If gaps remain, launch follow-up Explore agents for specific areas.

---

## Step 5: Write Investigation Report

Write `investigation-report.md` in the current working directory.

**Filename sanitization**: If the issue has a reference (e.g., `#42`, `PGR-1234`), the file is still named `investigation-report.md` (the caller handles issue-specific paths).

```markdown
# Investigation Report: {issue title}

## Summary
- 1-3 sentence overview of findings

## Architecture Overview
- Component/module structure relevant to the issue
- Layer boundaries and responsibilities

## Data Flow
- Entry point -> processing -> output (with file:line references)
- State management involved
- Side effects (DB writes, API calls, file I/O)

## Affected Files
| File | Role | Lines | Changes Needed |
|------|------|-------|----------------|
| `src/...` | Entry point | 45-120 | Modify X |
| `src/...` | Data layer | 10-80 | Add Y |

## Dependencies
- **Upstream** (what calls this): [list with file:line]
- **Downstream** (what this calls): [list with file:line]
- **External** (libraries, APIs, services): [list]

## Existing Patterns
- How similar features are implemented in this codebase
- Conventions observed (naming, error handling, testing)

## Test Coverage
- Existing tests: [list with file paths]
- Covered scenarios: [list]
- Missing coverage: [list]

## Risks / Concerns
- Potential issues discovered during investigation
- Complexity hotspots
- Missing error handling or edge cases

## Decision Points
- Ambiguities that need resolution in /dig phase
- Design choices with trade-offs

## Open Questions
- Things that could not be determined from code alone
```

**Rules:**
- Every claim must reference a specific `file:line`
- Distinguish facts (read from code) from inferences
- If something is unclear, put it in Open Questions — do not guess
- The report must be self-contained — the caller has no access to this fork's context

---

## Error Handling

| Situation | Action |
|-----------|--------|
| Entry points unclear | Grep broadly, expand search patterns |
| Agent returns shallow results | Re-launch with more specific prompts |
| Codebase too large for full trace | Scope down, report what was covered and what was skipped |
| CLAUDE.md missing | Infer architecture from directory structure |
