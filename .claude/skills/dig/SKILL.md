---
name: dig
description: "Resolve design ambiguities before implementation: auto-decide what codebase patterns already settle, ask the user about the rest, and output a decision matrix. Use after investigation, before decomposition, when the approach could go several valid ways."
user-invocable: true
allowed-tools:
  - ToolSearch
---

# Dig — Structured Ambiguity Resolution

Resolve ambiguities before implementation by generating structured questions with options. Auto-decides choices that follow established project patterns.

## When to Use

- After investigation is complete but before decomposition
- When there are design choices, trade-offs, or unclear requirements
- When the approach could go multiple valid directions

## Process

### Step 1: Extract Ambiguities

From the investigation results, identify every decision point:

| # | Ambiguity | Category | Auto-decidable? |
|---|-----------|----------|-----------------|
| 1 | ... | architecture / api-design / data-flow / concurrency / error-handling / naming / testing | yes/no |

### Step 2: Apply Auto-Decide Rules

For auto-decidable ambiguities, check the project's CLAUDE.md, rules/*.md, and existing codebase patterns to determine the answer.

**How to auto-decide:**
1. Check if CLAUDE.md or rules/ explicitly defines a convention for this decision
2. Check if the codebase has an established pattern (Grep for similar implementations)
3. If a clear, consistent pattern exists → auto-decide and record the rule

**Common decision categories:**
- **Architecture**: Where does this code belong? (Check CLAUDE.md architecture section)
- **API Design**: Public or internal? What return type? (Check existing public API patterns)
- **Data Flow**: Sync or async? Callback or stream? (Check established patterns)
- **Concurrency**: Thread safety approach? (Check existing locking/synchronization patterns)
- **Error Handling**: Exception, Result type, or null? (Check existing error patterns)
- **Naming**: Follow project naming conventions (Check CLAUDE.md or rules/)
- **Testing**: Unit, integration, or both? (Check existing test patterns)

### Step 3: Investigate Remaining Unknowns

For non-auto-decidable ambiguities:
1. Use Explore agent to find existing patterns in the codebase
2. Check if similar features already exist and how they handle the same decision
3. Read relevant documentation or ADRs

### Step 3.5: Codex Design Review (Optional)

For non-auto-decidable ambiguities in the **architecture**, **api-design**, or **concurrency** categories, consult Codex for alternative approaches before asking the user.

**Skip this step for**: naming, testing, error-handling, data-flow categories (these are resolved by codebase patterns or user preference).

Follow the call pattern and fallback in `rules/behavior.md → Call pattern`.

**Give Codex**: the unresolved architecture/api-design/concurrency ambiguities with their options, plus the codebase patterns found in Step 2.

**Ask Codex for**, per ambiguity:
1. An evaluation of the proposed options
2. Alternatives not yet considered
3. A recommendation with reasoning
4. Risks and trade-offs

Fold the recommendations into the options presented to the user in Step 4 — they are options, not decisions.

### Step 4: Ask User (max 3 rounds, max 4 questions per round)

Present remaining unknowns with `AskUserQuestion`:
- Context from investigation
- 2-4 concrete options with trade-offs
- Recommend the option matching existing codebase patterns

### Step 5: Output Decision Matrix

```markdown
## Dig Results: {requirement}

### Auto-Decided
| # | Decision | Rule | Result |
|---|----------|------|--------|
| 1 | Where to place X | CLAUDE.md: "feature code in src/features/" | src/features/x/ |

### Investigated
| # | Decision | Finding | Result |
|---|----------|---------|--------|
| 2 | Error handling approach | Existing pattern in src/api/client.kt uses Result<T> | Result<T> |

### User-Decided
| # | Decision | Choice | Reason |
|---|----------|--------|--------|
| 3 | Public API surface | Option A: minimal | user preference |

### Assumptions (if any)
| # | Assumption | Risk |
|---|-----------|------|
```
