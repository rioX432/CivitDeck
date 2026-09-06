---
name: investigate
description: "Investigate the codebase and report: trace data flows, map dependencies, assess impact. Use for standalone questions like \"how does X work\" or \"what breaks if we change Y\". Reports only — no implementation, no proposals. For investigation inside a /dev run, use dev-investigate instead."
argument-hint: "<topic, feature, or issue>"
user-invocable: true
allowed-tools:
  - Read
  - Glob
  - Grep
  - Agent
  - Bash(git log:*)
  - Bash(git diff:*)
  - Bash(git blame:*)
  - Bash(gh issue view:*)
  - TaskCreate
  - TaskUpdate
  - TaskList
  - TaskGet
  - AskUserQuestion
---

# /investigate — Codebase Investigation

Answer a question about the codebase with evidence. Report findings only.

**Use when:** "How does X work?", "What would be affected if we change Y?", "Trace the auth flow end-to-end", "What's the current state of module Z?"

**Not for:** web research (`/think`), pre-implementation ambiguity resolution (`/dig`), finding bugs (`/audit`), investigation inside a `/dev` run (`/dev-investigate`)

The method and report format live in [report-format.md](report-format.md) — read it before Phase 2.

---

## Phase 1: Scoping

1. Parse `$ARGUMENTS`:
   - **Issue reference** (`#42`, `PGR-1234`): fetch the issue, extract keywords and affected areas
   - **Feature/module name**: find entry points via Grep/Glob
   - **Free-form question**: extract key terms, identify likely code areas
2. Choose 2-4 investigation axes (table in `report-format.md`)
3. Identify entry points per axis — files, classes, functions
4. `TaskCreate` one task per axis

## Phase 2: Deep Investigation

Follow [report-format.md](report-format.md): launch the Explore agents in parallel, apply the investigation checklist, then run the Think Twice pass before writing anything.

## Phase 3: Report

Present the report skeleton from [report-format.md](report-format.md) directly to the user, filled in. Apply the evidence rules — every claim carries a `file:line`, and anything unclear goes in Open Questions rather than being guessed.

Stop here. No implementation, no proposals. If the investigation surfaced work worth doing, say so in one line and let the user decide whether to run `/issue`.

---

## Error Handling

| Situation | Action |
|-----------|--------|
| Issue not found | Report error, ask for clarification |
| Entry points unclear | Grep broadly, ask user if still ambiguous |
| Agent returns shallow results | Re-launch with more specific prompts |
| Codebase too large for full trace | Scope down; report what was covered **and what was skipped** |
