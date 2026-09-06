# Investigation Method & Report Format

Shared by `/investigate` (standalone) and `/dev-investigate` (forked, issue-driven).

## Contents
- Explore agent template
- Investigation checklist
- Think Twice pass
- Report skeleton
- Issue-driven additions (dev-investigate only)
- Evidence rules

## Explore agent template

Launch one agent per axis, in parallel:

```
Agent(
  subagent_type: "Explore",
  prompt: "Investigate {axis} for {topic} in this codebase.

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

Pick 2-4 axes:

| Axis | Question it answers |
|---|---|
| Code structure | Where does the relevant code live? What layer/module? |
| Data flow | How does data enter, transform, and exit? |
| Dependencies | What depends on this? What does this depend on? |
| Test coverage | What is tested? What is not? Where are the test files? |
| History | Recent changes, who touched it, related PRs (`git log`, `git blame`) |

## Investigation checklist

Each agent must cover, for its axis:

- [ ] **Read the code**: every involved file, not just entry points
- [ ] **Trace the flow**: function calls, event handlers, data transformations
- [ ] **Map boundaries**: module boundaries, public vs internal APIs
- [ ] **Check tests**: existing test files, what is covered, what is missing
- [ ] **Check history**: `git log` / `git blame` for recent changes and context

## Think Twice pass

After the agent reports come back, before writing anything:

1. Did the agents actually read the code, or did they speculate?
2. Are there code paths or edge cases not covered?
3. Is impact analysis complete — all callers, all downstream dependencies?
4. Do findings from different axes contradict each other?

If gaps remain, launch follow-up Explore agents scoped to the specific gap. Do not paper over a gap in the report.

## Report skeleton

```markdown
## Investigation: {topic}

### Summary
1-3 sentences.

### Architecture Overview
- Component/module structure relevant to the topic
- Layer boundaries and responsibilities
- Key abstractions and interfaces

### Data Flow
- Entry point → processing → output (with file:line references)
- State management involved
- Side effects (DB writes, API calls, file I/O)

### Affected Files
| File | Role | Lines | Notes |
|------|------|-------|-------|
| `src/...` | Entry point | 45-120 | Handles X |

### Dependencies
- **Upstream** (what calls this): [list with file:line]
- **Downstream** (what this calls): [list with file:line]
- **External** (libraries, APIs, services): [list]

### Existing Patterns
- How similar features are implemented in this codebase
- Conventions observed (naming, error handling, testing)

### Test Coverage
- Existing tests: [list with file paths]
- Covered scenarios: [list]
- Missing coverage: [list]

### Risks / Concerns
- Potential issues discovered during investigation
- Complexity hotspots
- Missing error handling or edge cases

### Open Questions
- Things that could not be determined from code alone
```

## Issue-driven additions (dev-investigate only)

When investigating for a specific issue, the report is a handoff to `/dig` and `/decompose`. Add:

- **Affected Files** gains a `Changes Needed` column replacing `Notes`
- A **Decision Points** section listing ambiguities `/dig` must resolve, each with the options seen in the codebase

```markdown
### Decision Points
- {ambiguity}: options observed are {A at file:line} and {B at file:line}
```

## Evidence rules

- Every claim references a specific `file:line`
- Facts (read from code) and inferences are labelled differently
- Anything unclear goes in Open Questions — never a guess
- A forked report must be **self-contained**: the caller has no access to the fork's context
