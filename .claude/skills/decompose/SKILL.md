---
name: decompose
description: "Break a task into ordered subtasks with dependencies"
user-invocable: true
allowed-tools:
  - ToolSearch
---

# Task Decomposition

Break down a development task into small, executable subtasks with clear dependencies.

## When to Use

- After investigation and dig phases are complete
- When the approach is confirmed and decisions are made
- Before starting the implementation loop

## Input Requirements

You need these before decomposing:
1. Investigation results (affected files, data flow, impact)
2. Decision matrix from dig phase (resolved ambiguities)
3. Confirmed approach

## Decomposition Rules

### Size
- Each task should take **5-30 minutes** to implement
- If a task feels larger, split it further

### Structure per Task
Each task must include:
- **What**: What to implement/change
- **Where**: Target file(s) with paths
- **How**: Specific implementation approach
- **Why**: Reason this change is needed
- **Verify**: How to confirm it works (test command, manual check, etc.)

### Architecture-Based Ordering

Read CLAUDE.md to understand the project's architecture layers, then split by layer from inner to outer.

**General pattern (adapt to project):**
```
1. Core types      — data classes, interfaces, enums
2. Business logic  — use cases, services, domain rules
3. Infrastructure  — repositories, API clients, DB
4. Presentation    — UI components, view models, controllers
5. Tests           — unit tests, integration tests
6. Cross-cutting   — lint, formatting, CI checks
```

**For KMP projects:**
```
1. commonMain API  — shared types, interfaces
2. expect/actual   — platform abstractions
3. androidMain     — Android-specific implementation
4. iosMain         — iOS-specific implementation
5. Tests           — commonTest, platform tests
6. Sample apps     — androidApp, iosApp
7. Cross-cutting   — detekt, explicitApi
```

**For web projects:**
```
1. Types/schemas   — shared types, API contracts
2. Backend logic   — API handlers, services
3. Frontend logic  — state management, hooks
4. UI components   — views, pages
5. Tests           — unit, E2E
6. Cross-cutting   — lint, build
```

### Pairing Rules
- **Type + mapper = 1 task**: Data type and its transformation go together
- **Implementation + test = 1 task**: Never separate implementation from its test
- **Cross-cutting checks = last task**: After all implementations are done

### Dependency Order
Set dependencies on tasks that require earlier ones:
- Core type tasks block implementation tasks
- Implementation tasks block test tasks
- All tasks block cross-cutting checks

## TaskCreate Format

Use `TaskCreate` for each subtask:

```
subject: "Implement {What} in {Where}"
description: |
  **What**: {description}
  **Where**: {file path(s)}
  **How**: {implementation approach}
  **Why**: {reason}
  **Verify**: {verification step}
activeForm: "Implementing {What}"
```

## Output

After creating all tasks, show the full task list with dependencies:

```
| # | Layer | Description | Blocked By |
|---|-------|-------------|------------|
| 1 | Core | Define FooData data class | — |
| 2 | Logic | Implement FooService | #1 |
| 3 | Infra | Add FooRepository | #2 |
| 4 | UI | Add FooScreen component | #2, #3 |
| 5 | Test | Unit tests for FooService | #2 |
| 6 | Cross | Lint + format check | #3, #4 |
```

Ask user to confirm before starting implementation.

---

## Codex Architecture Validation (Optional)

After generating the full task list, use Codex to validate the decomposition.

### Load Codex

```
ToolSearch("select:mcp__codex__codex")
```

### Call Codex (if available)

```
mcp__codex__codex(
  prompt: "Validate this task decomposition for correctness and completeness:

  ## Task List
  {full task table with dependencies}

  ## Project Architecture
  {architecture layers from CLAUDE.md}

  ## Validation Checklist
  1. **Layer ordering**: Are tasks ordered inner-to-outer (core → logic → infra → presentation → tests → cross-cutting)?
  2. **Missing tasks**: Are there any steps that should exist between tasks? Any gaps?
  3. **Dependency correctness**: Are all dependency edges correct? Any missing or unnecessary dependencies?
  4. **Parallelization**: Which tasks could safely run in parallel (no shared state, no dependency)?
  5. **Risk assessment**: Which tasks are highest-risk and might need extra verification?

  Output: validated task list with any corrections, plus a list of parallelizable task groups."
)
```

If Codex suggests corrections:
1. Apply the corrections to the task list
2. Note what was changed and why

### Fallback (Codex unavailable)

If `ToolSearch` fails to find Codex or the call errors:
- Skip validation — output the task list as-is (traditional flow)
