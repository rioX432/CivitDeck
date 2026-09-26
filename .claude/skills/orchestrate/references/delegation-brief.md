# Delegation Brief

What the lead hands a worker, and what the worker hands back. Pass these items at the granularity the lane needs.
No fixed JSON is required, but the four elements marked **required** are not optional — a brief missing them is
the documented cause of workers duplicating each other's work while a gap goes uncovered.

## Brief

- **Objective (required)** — the outcome, its acceptance criteria, and the mode: `investigate`, `review`,
  `experiment`, or `implement`. Investigation does not authorize edits.
- **Output format (required)** — exactly what to return in the final message, and the path of every artifact to
  write. Only the final message reaches the lead; intermediate tool results stay inside the worker.
- **Tools and sources (required)** — the commands to run, the files to read, what counts as a primary source, and
  what not to touch. Name the leaf skill or rules file to apply when one exists.
- **Boundaries (required)** — out-of-scope areas, the paths other lanes own, and what to do on a blocker: report
  it, do not widen scope.
- **Ownership** — the repository, the writable paths, the read-only dependencies. One writer per file.
- **Current evidence** — the symbols, call sites, reproduction steps, and pre-existing diff already known.
  Separate confirmed facts from assumptions so the worker does not re-derive or trust the wrong one.
- **Contract** — the upstream artifact and version this lane starts from, what it publishes, and which gate the
  next lane waits on. Say explicitly when an input is still unsettled.
- **Permissions** — the edits and external operations already authorized, and the boundary. Delegating does not
  widen permissions, and the same boundary applies to anything the worker spawns.
- **Budget** — the turn or tool-call cap, and what to do when it is reached.
- **Verification** — the minimal relevant command, resolved from `CLAUDE.md` rather than guessed, plus any
  cross-boundary check and which criterion each one proves.

## Return

Ask for, and require:

- **Changed paths** and a one-line summary of each change.
- **Commands run, with their output or exit status** — the evidence, not a narrated "it works".
- **What was not run, failed, or was blocked by the environment**, kept distinct from what passed.
- **Findings** as a distilled summary of roughly 1,000–2,000 tokens, with artifact paths for the detail. Bulk
  reading stays in the worker's context; that is why the lane exists.
- **Remaining uncertainty** and anything downstream lanes must know.
- A `complete` claim scoped to this lane only. A worker never reports the goal as done.

## Feedback

When a return fails a gate, say three things: what does not match, which evidence shows it, and what would count
as fixed. Then verify the changed artifact yourself and re-run the verification of any lane that consumed the old
version. Do not repeat an identical prompt — an unchanged brief produces an unchanged result.
