---
name: dig
description: "Resolve design ambiguities before implementation: keep only the decisions that would change the result, auto-decide what codebase patterns already settle, ask the user at most 5 questions with a recommendation each, and output a decision matrix with acceptance criteria. Use after investigation, before decomposition, when the approach could go several valid ways. Skip when the change fits in one sentence and follows existing patterns."
user-invocable: true
---

# Dig — Structured Ambiguity Resolution

Find the decisions that would change what gets built, settle what evidence settles, and ask the user only the rest. This step does not edit product code.

## When to Use

- After investigation is complete but before decomposition
- When there are design choices, trade-offs, or unclear requirements
- Skip it when the change can be described in one sentence and every choice follows from the request or an existing pattern. State any assumption and continue

## Process

### Step 1: Extract Decision Points

From the investigation results, list each open point and what depends on it:

| # | Ambiguity | Category | Auto-decidable? |
|---|-----------|----------|-----------------|
| 1 | ... | architecture / api-design / data-flow / concurrency / error-handling / naming / testing | yes/no |

Keep a point only if a different answer would change user-visible behavior, a data model or API contract, architecture or dependency direction, what the tests assert, or something costly to reverse. Drop style preferences. Rank the rest by impact × uncertainty.

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

Follow the call pattern and fallback in [rules/behavior.md → Call pattern](../../rules/behavior.md).

**Give Codex**: the unresolved architecture/api-design/concurrency ambiguities with their options, plus the codebase patterns found in Step 2.

**Ask Codex for**, per ambiguity:
1. An evaluation of the proposed options
2. Alternatives not yet considered
3. A recommendation with reasoning
4. Risks and trade-offs

Fold the recommendations into the options presented to the user in Step 4 — they are options, not decisions.

### Step 4: Ask Only What Blocks

- At most 5 questions per run, highest impact first, unless the user asks for a fuller interview. `AskUserQuestion` takes up to 4 at a time; hold back a question whose options depend on another answer
- One decision per question: why it matters in one sentence, 2-4 mutually exclusive options with trade-offs, and a recommendation with its evidence — usually the existing codebase pattern
- If an answer is ambiguous, clarify it before moving on. If an answer changes the direction, re-read the affected code and repeat Steps 1-4 for the points it opens
- **No user reachable** (autonomous `/dev`, or running as a sub-agent without `AskUserQuestion`): do not ask. Take the recommended option, record it under Assumptions with source `default`, and list the open questions for the caller
- If nothing blocks, say so and move on

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
| # | Assumption | Source | Risk | How it will be checked |
|---|-----------|--------|------|------------------------|

### Acceptance Criteria
- {observable outcome, including one end-to-end check — reuse the issue's `Done when` when it exists}

### Out of Scope
- {what this change will not do — reuse the issue's `Scope: Out` when it exists}
```

Pass the matrix to `/decompose`.
