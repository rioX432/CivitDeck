---
name: decompose
description: "Break a task into ordered subtasks with dependencies"
user-invocable: true
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
