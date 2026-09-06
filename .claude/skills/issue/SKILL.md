---
name: issue
description: "Write and size GitHub Issues so each one is a single agent-executable unit — one outcome, one PR, one proof. Use when filing an issue, when an audit/scan/research skill converts findings into issues, or when an existing issue looks too large to implement in one pass."
argument-hint: "[finding, feature description, or existing issue number to re-size]"
allowed-tools:
  - Bash(gh issue create:*)
  - Bash(gh issue list:*)
  - Bash(gh issue view:*)
  - Bash(gh issue edit:*)
  - Bash(gh label list:*)
  - Read
  - Grep
  - Glob
  - AskUserQuestion
---

# /issue — Right-Sized Issue Authoring

Turn a finding, request, or vague idea into issues an agent can finish in one pass and a human can review in one sitting.

**Input:** $ARGUMENTS

## Workflow

Copy this checklist and check items off as you go:

```
- [ ] 1. Collect the raw input
- [ ] 2. Check for duplicates
- [ ] 3. Run the sizing gate
- [ ] 4. Split until every unit passes
- [ ] 5. Draft with the template
- [ ] 6. Confirm with the user
- [ ] 7. Create and link
```

---

## Step 1: Collect

Gather, from `$ARGUMENTS` and the calling context:

- The observable problem or desired change (not the proposed solution)
- Evidence: `file:line`, log excerpt, screenshot, review finding, metric
- Which files the change most likely touches (Grep/Glob if not already known)

If the input is an existing issue number, `gh issue view <n>` and treat its body as the raw input to re-size.

## Step 2: Check for duplicates

```bash
gh issue list --state all --search "{2-4 distinctive keywords}" --limit 20
```

Also check `CLAUDE.md → ## Won't Do`. A match there means **do not file** — report the entry instead.

If an open issue already covers it, comment on that issue instead of creating a new one.

## Step 3: Sizing gate

Every issue must pass all five checks. Judge the issue as written, not as intended.

| # | Check | Fails when |
|---|-------|-----------|
| 1 | **Single outcome** | The title needs "and", a comma list, or is a category verb with no object ("improve X", "refactor Y", "clean up Z") |
| 2 | **Bounded change** | The expected edit exceeds ~5 files / ~300 lines, or you cannot name any of the files |
| 3 | **Single proof** | You cannot write one `Done when` naming a real command and its exact success output |
| 4 | **No open decisions** | The body still contains an unresolved design choice ("either A or B", "TBD", "decide during implementation") |
| 5 | **Independently mergeable** | Merging it alone leaves the repo broken, or leaves the change with no observable effect |

The ~5 files / ~300 lines threshold is the size of a diff a reviewer holds in their head in one sitting. If `CLAUDE.md` defines its own limit, that wins.

**On failure:**

| Failed check | Action |
|---|---|
| 1 or 2 | Split — see [splitting.md](splitting.md) |
| 3 | Acceptance criteria are unknown. Ask the user, or file a spike |
| 4 | File a **spike** whose deliverable is a written decision, not code. Implementation issues come after |
| 5 | Merge it back into its sibling, or restructure as a vertical slice |

## Step 4: Split

Read [splitting.md](splitting.md) for the split moves and worked examples. Re-run the sizing gate on every resulting unit — splitting can produce a piece that still fails.

When splitting yields 3 or more units, create a tracking **epic**:

- Epic issue: `epic` label, task list of child issue numbers, no implementation detail of its own
- Each child: `Part of #{epic}` in the body

Only the children are executable. Never assign an epic to `/dev`.

## Step 5: Draft

```markdown
## Goal
{One sentence stating the observable change.}

## Context
{Why this, why now. Link the evidence: file:line, audit run, review URL, user report, metric.}

## Scope
**In:**
- {bullet}

**Out:**
- {what this issue deliberately does not cover, with links to sibling issues}

## Done when
- [ ] `{command}` output shows `{exact success signal}`
- [ ] {observable behavior check}

## Files (expected)
- `path/to/file` — {what changes}

## Core Value Alignment
{Which Core Value from CLAUDE.md this strengthens, and the one-step reasoning.}

## Risks / Notes
{Known traps, edge cases, prior art.}
```

Rules for the body:

- **`Done when` is the contract with the agent.** Resolve the command from `CLAUDE.md → Commands` or CI config — never guess one. It is reused verbatim as the `/goal` completion condition (`rules/ai-ops.md → /goal for Autonomous Execution`), so an unverifiable `Done when` produces an unfinishable autonomous run.
- **`Scope: Out` is not optional.** It is what stops an agent from expanding the change.
- **Core Value Alignment is a gate**, per `rules/ai-ops.md → Core Value Guard`. If the change fails the one-step test, propose a `## Won't Do` entry instead of an issue.
- No solution design in the body beyond what Step 3 check 4 required. Implementation is `/dev`'s job.

## Step 6: Confirm

Present the drafted issues as a table (title, size, files, `Done when`) and use `AskUserQuestion`:

**Which issues should be created?** → All / Let me select / None (draft only)

Skip this step in autonomous mode (`/goal`); create everything that passed the gate and print the URLs.

## Step 7: Create

```bash
gh issue create \
  --title "{verb} {object}" \
  --body "$(cat <<'EOF'
{drafted body}
EOF
)" \
  --label "{labels}"
```

Check available labels first with `gh label list`; only pass labels that exist.

| Signal | Labels |
|---|---|
| Crash, data loss, security | `bug`, `priority: high` |
| Incorrect behavior | `bug` |
| Code health, duplication, dead code | `tech-debt` |
| UI/UX, accessibility | `ux` |
| Dependency, CVE | `dependencies`, `security` |
| Unresolved decision | `spike` |
| Tracking issue | `epic` |
| Estimated size | `size: S` (≤1 file) / `size: M` (≤5 files) |

`size: L` is not a valid implementation issue — it means Step 4 was skipped.

After creating, print the URLs and, for epics, edit the epic body to link the children.

---

## Batch mode (called from a scanning skill)

`/audit`, `/ux-audit`, `/competitive-audit`, and `/monitor` produce many findings at once. Additional rules:

1. **Group by root cause, not by occurrence.** Twelve hits of the same missing null check in one module is one issue, not twelve.
2. **Cap the run.** Default max 10 new issues; `/competitive-audit` keeps its own cap of 3. Above the cap, file the top-severity ones and report the rest in the summary only.
3. **Every issue keeps its source**: which skill, which run, which finding.
4. **Never file Low severity as an issue.** It goes in the report.

---

## Re-sizing an existing issue

When `/dev` or a user finds an issue that is too large:

1. Run the sizing gate against the existing body
2. Report which checks failed, with the quoted text that failed them
3. Propose the split as a table
4. On confirmation: create the children, convert the original into the epic (`gh issue edit` — retitle, add the task list, add the `epic` label), and do not close it

## Error handling

| Situation | Action |
|---|---|
| `gh` not authenticated | Report; output the drafted bodies as markdown so the user can paste them |
| Label does not exist | Create the issue without it; note the missing label |
| `CLAUDE.md` has no Core Values | Ask the user to define them before filing feature issues; bug and security issues proceed |
| Sizing gate cannot be judged (no codebase access) | State the assumption in `Risks / Notes`; do not silently pass the gate |
