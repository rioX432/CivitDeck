---
name: decompose
description: "Turn a confirmed change into a short, ordered plan of verifiable subtasks: behavior slices with the test in the same step, prerequisites and spikes first, each with a Verify command and explicit dependencies. Use after investigation and ambiguity resolution, before the implementation loop. Not for splitting GitHub Issues — that is the issue skill."
user-invocable: true
---

# Task Decomposition

Break a confirmed change into a short, ordered plan whose every step can be checked on its own. Planning does not edit product code.

## When to Use

- After investigation and dig phases are complete
- When the approach is confirmed and decisions are made
- Before starting the implementation loop
- When the change fits in one sentence, return a one-task plan instead of a breakdown

## Input Requirements

You need these before decomposing:
1. Investigation results (affected files, data flow, impact)
2. Decision matrix from dig (decisions, assumptions, acceptance criteria, out of scope)
3. Confirmed approach

If an open decision would change the plan, go back to `/dig` instead of planning around it.

## Decomposition Rules

### Cut by behavior, not by layer

Each subtask delivers one observable behavior through the layers it needs, so a wrong assumption shows up after the first subtask rather than after every layer is built. Inside a subtask, follow the project's layer order from CLAUDE.md (inner to outer, e.g. types → logic → data → UI; for KMP: commonMain → expect/actual → androidMain / iosMain).

- **Prerequisites first**: a contract, schema, shared type, generated code, or migration that every slice needs is its own first subtask
- **Spike the unknowns**: when a subtask's approach is uncertain, add a small spike whose output is a decision with evidence, and order the dependent subtasks after it
- **Test in the same subtask** as the code it verifies. `/dev` TDD mode writes it first inside that subtask
- **Cross-cutting checks** (lint, format, full test suite) are the last subtask

### Size by verifiability

A subtask is small enough when its Verify step can be written before starting and one reviewer can check it. Split further only when you cannot say how to verify it, it mixes independent behaviors, or it fails in practice. Aim for 2-5 subtasks for an issue that passed the `issue` sizing gate; leave out operational steps such as "open the file" or "run the tests".

### Structure per Task

Each task must include:
- **What**: The behavior this subtask adds
- **Where**: Target file(s) with paths
- **How**: Specific implementation approach
- **Why**: Reason this change is needed
- **Verify**: The command or manual check that proves it, from CLAUDE.md → Commands. Name checks that cannot run locally (device, account, environment) instead of implying they pass

### Dependencies and parallelism

Set `Blocked By` only on real dependencies. Mark subtasks parallel only when their file scopes are disjoint and neither depends on the other; everything else runs in order, one writer per file.

## Task Format

Create each subtask with `TaskCreate` when this session has it (Claude 5 models and background sub-agents do not); otherwise list the subtasks in this format in your reply and keep their status there:

```
subject: "Implement {What} in {Where}"
description: |
  **What**: {behavior}
  **Where**: {file path(s)}
  **How**: {implementation approach}
  **Why**: {reason}
  **Verify**: {verification step}
activeForm: "Implementing {What}"
```

## Output

After creating all tasks, show the full task list with dependencies:

```
| # | Behavior | Scope | Verify | Blocked By |
|---|----------|-------|--------|------------|
| 1 | Favorite model + repository (prerequisite) | shared/model, data/FavoriteRepository + tests | `./gradlew :shared:test` | — |
| 2 | Toggle a favorite from the detail screen | ToggleFavoriteUseCase, DetailScreen + tests | `./gradlew :feature:detail:test` | #1 |
| 3 | List favorites on a new tab | GetFavoritesUseCase, FavoritesScreen + tests | `./gradlew :feature:favorites:test` | #1 |
| 4 | Lint + full test suite | — | `./gradlew detekt test` | #2, #3 |
```

End with the end-to-end check from the acceptance criteria. #2 and #3 above can run in parallel: disjoint files, shared prerequisite only.

Ask the user to confirm before starting implementation — in autonomous `/dev`, skip the confirmation and proceed.

When a subtask fails or grows during implementation, re-plan it and the subtasks that depend on it, not the whole plan.

---

## Codex Architecture Validation (Optional)

After generating the full task list, use Codex to validate the decomposition.

Follow the call pattern and fallback in [rules/behavior.md → Call pattern](../../rules/behavior.md).

**Give Codex**: the full task table with dependencies, and the architecture layers from CLAUDE.md.

**Ask Codex to check**:
1. **Slicing** — does each subtask deliver an observable behavior, with its test inside it?
2. **Missing tasks** — gaps between steps, or a prerequisite that is not its own subtask?
3. **Dependency correctness** — wrong, missing, or unnecessary edges?
4. **Parallelization** — which tasks share no files and no dependency?
5. **Risk** — which tasks need a spike or extra verification?

Apply any corrections and note what changed and why.
