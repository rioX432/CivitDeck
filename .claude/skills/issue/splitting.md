# Splitting Oversized Issues

## Contents
- Choosing a split move
- Move 1: Spike / implementation
- Move 2: Vertical slice
- Move 3: Architecture layer
- Move 4: Case (happy path → edges → errors)
- Move 5: Contract first
- Move 6: Mechanical / judgment
- Move 7: Root cause (for scan findings)
- Worked examples
- Anti-patterns

## Choosing a split move

Pick by which sizing-gate check failed and what the work actually is.

| Situation | Move |
|---|---|
| Unresolved design decision | 1 — Spike / implementation |
| Change must be demoable at every step | 2 — Vertical slice |
| Change is structurally deep, not user-visible | 3 — Architecture layer |
| One behavior, many conditions | 4 — Case |
| Two components must agree on a shape | 5 — Contract first |
| Wide rename plus a few hard spots | 6 — Mechanical / judgment |
| A scan produced dozens of hits | 7 — Root cause |

Moves compose: a spike often precedes a contract-first split.

## Move 1: Spike / implementation

Split when the issue contains a decision nobody has made yet.

- **Spike issue**: deliverable is a written decision (an ADR, a comment on the epic, a section in `docs/`), never code. `Done when` = "the decision and its rationale are posted on #N". Timebox it in the body.
- **Implementation issues**: filed after the spike closes, referencing the decision.

Never let an agent decide architecture inside an implementation PR — the decision becomes invisible in the diff.

## Move 2: Vertical slice

Split so each issue delivers one user-visible path end-to-end through every layer.

Prefer this when the feature must be demoable or shippable behind a flag at each step. Slice by user intent: "user can create X" → "user can edit X" → "user can delete X", not "add the model" → "add the API" → "add the screen".

The first slice may include scaffolding the later ones reuse. Say so in `Scope: In`.

## Move 3: Architecture layer

Split inner-to-outer along the project's layers, matching `/decompose`:

```
core types → business logic → infrastructure → presentation → tests → cross-cutting
```

Use this when the change is structural (a new abstraction, a data model migration) and no intermediate step is user-visible. Each issue must still build and pass tests on its own — if the middle layer breaks the build alone, it belongs with its neighbor.

## Move 4: Case

Split one behavior by its conditions.

1. Happy path with the default input
2. Edge cases (empty, boundary, maximum)
3. Error and offline states
4. Concurrency and races

File #1 first; the rest reference it. This is the safest split for bug fixes with an unclear reproduction: #1 fixes the reported case, the follow-ups harden it.

## Move 5: Contract first

When a producer and a consumer must agree:

1. Define the type / schema / API contract, with tests against the contract alone
2. Producer implements it
3. Consumer adopts it

Issues 2 and 3 can then run in parallel and merge independently.

## Move 6: Mechanical / judgment

A wide change usually has a large boring part and a small hard part.

- **Mechanical issue**: the rename, the import rewrite, the codemod. Large diff, near-zero review risk. `Done when` = build and tests pass with no behavior change.
- **Judgment issues**: the two or three call sites where the change actually alters behavior. Small diffs, real review.

Do not let the hard part hide in a 2,000-line rename.

## Move 7: Root cause (for scan findings)

Scanning skills produce occurrences, not issues. Collapse them:

1. Group hits by the cause that would fix them all at once
2. One issue per cause; list occurrences as a table inside the body
3. Drop groups whose fix is smaller than the issue that describes it — those go in the report, not the tracker

A group spanning more than ~5 files is still oversized: split it further by module, and make the first one establish the pattern the rest follow.

## Worked examples

### Example 1 — category verb, no object

**Before** (fails check 1 and 2):

> **Improve app performance**
> The app feels slow. Optimize startup, list scrolling, and image loading.

**After** — spike first, because "slow" has no measurement yet:

| # | Title | Done when |
|---|---|---|
| 1 | Measure cold-start and scroll frame timings on a mid-tier device (`spike`) | Baseline numbers for the 3 suspect paths are posted on this issue |
| 2 | Cache decoded thumbnails in `ImageLoader` | `./gradlew :app:benchmark` shows P50 list frame time below 16ms |
| 3 | Defer analytics init off the startup critical path | Startup trace shows `AnalyticsInit` after first frame |

Issues 2 and 3 are written only after 1 closes — before that, their `Done when` would be guesses.

### Example 2 — "and" in the title

**Before** (fails check 1, 3 and 5):

> **Add CSV export and email delivery for reports**

**After** — vertical slice, then contract:

| # | Title | Scope: Out |
|---|---|---|
| 1 | Generate a CSV file from a report on demand | Delivery — see #2 |
| 2 | Email a generated CSV to the requesting user | Generation — see #1; scheduling — see #3 |
| 3 | Schedule recurring report emails | Ad-hoc export — #1, #2 |

Each merges alone: #1 ships a download, #2 ships a send button, #3 ships a schedule.

### Example 3 — a scan dump

**Before** (fails check 1 and 2): one issue titled "Fix 47 detekt violations" with a pasted report.

**After** — root cause, then module:

| # | Title | Body contains |
|---|---|---|
| 1 | Replace swallowed exceptions with `Result` in `sync/` | Table of the 9 occurrences with file:line |
| 2 | Replace swallowed exceptions with `Result` in `network/` | Table of the 6 occurrences; follows the pattern from #1 |
| 3 | Enable the `TooGenericExceptionCaught` detekt rule | Depends on #1, #2; `Done when` = detekt passes with the rule enabled |

The remaining 32 low-severity violations stay in the audit report and are never filed.

## Anti-patterns

| Pattern | Why it breaks agents |
|---|---|
| Epic used as an implementation issue | No single `Done when`; the agent stops at an arbitrary point and calls it done |
| Body is a checklist of 10 boxes | Each box is a separate outcome; the PR becomes unreviewable |
| "Refactor `FooManager`" | No observable change means no proof of completion |
| Splitting into steps that cannot build alone | CI is red between merges; `/dev-all` blocks |
| One issue per lint hit | Tracker noise; reviewers stop reading |
| Solution written as the goal | The agent implements the stated solution even when investigation finds a better one |
