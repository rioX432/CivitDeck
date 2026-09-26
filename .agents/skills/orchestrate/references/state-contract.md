# Orchestration State Contract

Keep these files small enough that a fresh session regains context quickly and can continue from them alone.

## Directory

```text
<orchestration directory>/
|-- BRIEF.md
|-- STATE.json
|-- EVIDENCE.md
|-- DECISIONS.md
`-- RUNLOG.md
```

Use a stable lowercase kebab-case slug. Reuse the existing directory when the objective matches; never create a
second one to resume the same run.

## BRIEF.md

Stable. Change it only when the objective, scope, criteria, or constraints change, and record the change in
`DECISIONS.md`.

```markdown
# <title>

## Outcome
<what is true when this is done>

## Mode
research | experiment | build

## Hypotheses
- H1: <falsifiable statement>   (omit for build mode)

## Success criteria
- SC1: <observable outcome, and the evidence it requires>

## Stop conditions
- <success, falsification, budget, repeated failure, or a decision only the user can make>

## Scope
- Repositories: <name and role each>
- Lanes: <id, objective, owner paths>
- Non-goals: <explicit exclusions>

## Constraints
- <time, cost, security, environment, or authorization limits the user gave>
```

## STATE.json

The authoritative operational state. It must stay valid JSON. Use stable IDs, point at the other files instead of
copying their text, and keep command logs and long reports out of it.

```json
{
  "schemaVersion": 1,
  "slug": "example-run",
  "mode": "experiment",
  "status": "active",
  "disposition": "continue",
  "updatedAt": "<ISO-8601 timestamp>",
  "outcome": "<what is true when this is done>",
  "successCriteria": [
    {"id": "SC1", "description": "<observable outcome>", "status": "unmet", "evidence": []}
  ],
  "stopConditions": ["<condition>"],
  "repositories": [
    {
      "name": "<repository>",
      "role": "<role in this run>",
      "branch": "<branch or detached>",
      "headAtStart": "<commit>",
      "lastSeenHead": "<commit>",
      "dirtyPathsAtStart": [],
      "ownedPaths": [],
      "verification": "<focused command, or manual flow>"
    }
  ],
  "lanes": [
    {
      "id": "L1",
      "title": "<bounded output>",
      "mode": "investigate",
      "status": "pending",
      "dependsOn": [],
      "writePaths": [],
      "gate": "unopened",
      "nextAction": "<one concrete action>",
      "artifacts": []
    }
  ],
  "contracts": [
    {"id": "C1", "name": "<API, schema, or generated client>", "owner": "L1", "version": "<published version>", "consumers": ["L2"]}
  ],
  "blockers": [],
  "openQuestions": [],
  "nextActions": ["<highest-priority unblocked action>"]
}
```

Allowed values:

- `status`: `active`, `blocked`, `complete`, `stopped`
- `disposition`: `continue`, `pivot`, `stop-success`, `stop-not-viable`, `blocked`, `handoff-to-dev`
- success criterion `status`: `unmet`, `met`, `failed`, `manual-qa`, `out-of-scope`
- lane `status`: `pending`, `running`, `returned`, `blocked`, `complete`, `failed`, `skipped`
- lane `gate`: `unopened`, `pass`, `fail`, `needs-evidence`, `disputed`

`complete` means the lane's gate is `pass`. A lane whose worker reported success but whose evidence has not been
checked is `returned`, not `complete`.

Record a worker's model and effort in its lane only when the host shows them; never infer them from output style.

## EVIDENCE.md

One row per material claim, precise enough for someone else to recheck.

```markdown
| ID | Claim | Source | Checked | Confidence | Implication |
| --- | --- | --- | --- | --- | --- |
| E1 | <fact, or inference labeled as such> | <URL, or repository:path> | <date> | high/medium/low | <effect on the decision> |
```

Link the page itself, not a search result. For code, give the repository and path, and the commit when line
numbers may move.

## DECISIONS.md

Append; do not rewrite history.

```markdown
## D1: <decision title>

- Date: <date>
- Status: accepted | rejected | superseded
- Decision: <what was chosen>
- Reason: <evidence and trade-off>
- Evidence: E1, E2
- Supersedes: <decision ID, if any>
```

Record pivots, scope and criteria changes, contract ownership, chosen approaches, and important rejected
alternatives. Routine implementation choices need no entry.

## RUNLOG.md

Append one concise entry per checkpoint. Do not paste raw build output; keep a full log as a separate file only
when it is needed to reproduce a result.

```markdown
## <ISO-8601 timestamp>

- Disposition: <disposition>
- Gates opened: <lane IDs and verdicts>
- Findings: <new material information>
- Repository state: <branch and HEAD per repository>
- Changed paths: <paths this run owns>
- Verification: <commands or manual checks, and results>
- Blockers: <specific prerequisite, or none>
- Next: <one concrete, highest-priority action>
```

## Reconciliation

At every resume and checkpoint:

1. Parse `STATE.json` before trusting it.
2. Compare the recorded branches, HEADs, and changed paths with Git.
3. Let code, tests, Git, and generated output decide what is observably done; `EVIDENCE.md` and `DECISIONS.md`
   hold the history the repositories cannot show.
4. Correct stale `nextActions`, leaving old reasoning in the append-only logs.
5. Check that every `met` criterion references evidence, every `complete` lane has a `pass` gate, and only real,
   active work is `running`.
6. Check that the first `nextActions` item is concrete, unblocked, and consistent with the disposition.
