---
name: orchestrate
description: "Coordinate several workers toward one goal that does not fit a single agent run: gate the fan-out decision, cut non-overlapping lanes, brief each worker with an objective, output format, tool guidance and boundaries, accept a return only on printed evidence, and checkpoint into resumable state files. Use for cross-module or cross-repository goals and for long-running PoCs and spikes that span sessions. Use /dev for one issue, /dev-all for a batch of issues, /review for a review of the current branch."
argument-hint: "[goal, or the path to an existing orchestration directory to resume]"
user-invocable: true
disable-model-invocation: true
allowed-tools:
  - Bash(git status)
  - Bash(git status:*)
  - Bash(git log:*)
  - Bash(git diff:*)
  - Bash(git branch:*)
  - Bash(git rev-parse:*)
  - Bash(git worktree:*)
  - Glob
  - Grep
  - Read
  - Write
  - Edit
  - Agent
  - Skill
  - AskUserQuestion
---

# /orchestrate — Lead and Workers

Run one goal that a single agent run cannot finish: hold the plan, hand bounded work to workers, judge what
comes back on evidence, and keep the whole thing resumable from files.

**Arguments:** $ARGUMENTS — a goal, or a path to an existing orchestration directory to resume.

The session running this skill is the **lead**. It owns the plan, the gates, the synthesis, and every
authorization decision. It does not do the workers' reading and editing itself; that is the point.

---

## Step 0: Do not fan out yet (GATE)

Fanning out is expensive and it is easy to make a run worse. Anthropic measures agents at roughly **4x** the
tokens of a chat turn and multi-agent runs at roughly **15x**; OpenAI's guidance is to start with one agent and
add specialists only when the contract really changes, because splitting early only adds prompts, traces and
approval surfaces. Both are recorded in [evidence.md](references/evidence.md).

Fan out only when **all three** hold:

1. The goal splits into parts with **distinct outputs** — not one output that several agents would each touch.
2. The parts have **non-overlapping write paths**, or are read-only.
3. A part can be finished from its brief **without conversing** with another part mid-flight.

Do not fan out when any of these is true:

- The parts need to share evolving context, or depend on each other densely. Workers cannot talk to each other
  and the lead cannot steer one mid-run; every coordination hop has to round-trip through the lead.
- Two parts would edit the same file, or redefine the same contract.
- The work is one issue (`/dev`), a batch of issues (`/dev-all`), one investigation (`/investigate`), or a review
  of the current branch (`/review`). Those skills already own their loop — do not wrap them in this one.
- The goal fits in one sentence and follows an existing pattern. Do it directly.

State which of the three conditions each lane satisfies before spawning anything. If only one lane survives the
gate, run it in this session and say so instead of building a one-worker orchestration.

---

## Step 1: Frame the goal

Before any worker exists, write down:

- **The outcome** — what is true when this is done, in observable terms.
- **Success criteria** — each with the evidence it requires. At least one must be a command whose output can be
  read. Resolve commands from project guidance; never invent one.
- **Stop conditions** — success, falsification, worker-turn budget, repeated failure with no new information, or a
  decision only the user can make. Do not invent a budget the user did not give.
- **Non-goals** — what this run will not touch.
- **Mode** — `research` (evidence and a recommendation, no product code), `experiment` (smallest useful
  implementation to test a hypothesis), or `build` (implement the agreed change). Mode decides what may be
  written, so record it and keep to it.
- **Permissions** — pushing, opening or merging PRs, deploying, posting, and migrating shared data happen only
  when this conversation already authorized them. Delegating does not widen permissions; a worker inherits the
  lead's boundary and no more.
- **Repository snapshot** — for each repository in scope: branch, `HEAD`, and `git status --short`. Keep
  pre-existing changes apart from this run's changes, and never reset, clean, or stash the user's work.

Treat every repository as independent: its own instructions (`CLAUDE.md` / `AGENTS.md`), its own tests, its own
failure boundary. Do not assume a parent directory is one repository.

---

## Step 2: Cut lanes

A lane is one worker's whole assignment. Give each lane an objective, an artifact, a write boundary, its
dependencies, and a stopping condition.

- **One writer per file, and per repository at a time.** Two workers editing one file is a merge conflict the
  lead has to untangle by hand, and it wastes both runs.
- **One owner per shared contract.** When lanes share an API, schema, or generated client, exactly one party owns
  it: either the upstream lane, or the lead when it settles the contract up front. Name the owner in the plan.
- **Dependencies are sequential** unless the contract is already settled. A downstream lane starts from a
  published artifact, never from an expected one. Independent investigation of downstream code can run in parallel
  with upstream work; integration verification against an unfinished contract cannot, and must not be counted as a
  pass.
- **To run dependent lanes in parallel, publish the contract first.** The lead may fix the interface before
  spawning — path, payload, signature, error cases — and write it verbatim into every brief. Then each brief must
  say: implement against this contract, and if it has to change, stop and report instead of changing it, because
  other lanes depend on it. Integration verification (Step 7) still compares the lanes' real code against each
  other, never the contract against itself.
- **Isolate writing lanes** with a git worktree (`isolation: worktree` on the subagent, or an explicit
  `git worktree add`) when two lanes must write in the same repository and the user wants them concurrent.

Name the **integration check that will open the final gate** in the plan, before any lane starts: the command
or comparison that proves the lanes fit together, and which criterion it settles. A gate invented after the
results are in gets fitted to them.

Scale the fan-out to the question, following Anthropic's published heuristic: simple fact-finding is one worker
with a handful of tool calls; a direct comparison is two to four workers; only genuinely broad work justifies
more. **Three to five parallel workers is the normal ceiling** — the failure mode on record is an agent spawning
fifty subagents for a simple query. Claude Code allows up to 20 concurrent subagents and a spawn depth of 3 by
default; those are limits, not targets.

---

## Step 3: Brief each worker

A vague brief is the documented cause of duplicated work — two workers researching the same angle while a third
gap goes uncovered. Every brief carries the four required elements, plus what a code change needs:

1. **Objective** — the outcome and its acceptance criteria.
2. **Output format** — exactly what to return, and where to write artifacts.
3. **Tools and sources** — which commands, which files, which sources count as primary; what not to touch.
4. **Boundaries** — what is out of scope, which paths are other lanes', what to do on a blocker instead of
   widening scope.

Read [delegation-brief.md](references/delegation-brief.md) and pass its items at the granularity the lane needs.
No fixed JSON is required, but the four elements above are not optional.

Also set per lane:

- **Model tier** — `haiku` for mechanical collection, `sonnet` for review and analysis, `opus` for long-horizon
  implementation and contract design, per `rules/ai-ops.md → Model Selection for Agents`. Treat provider tier
  names as adapter examples and keep the user's model
  for the lead. Record a model only when the host actually shows it; never infer it from output style.
- **A turn or tool budget** (`maxTurns`, or a stated cap in the brief) and what to do when it runs out: report the
  blocker, do not silently continue.
- **A compressed return.** A worker may read tens of thousands of tokens internally, but returns a distilled
  summary — Anthropic's published target is roughly 1,000–2,000 tokens — plus artifact paths. Only a subagent's
  final message reaches the lead, so what is not in that message is lost: require the evidence in it.
- **Read-only unless the lane needs to write.** Investigation, review, and verification lanes get no write tools.

---

## Step 4: Run the loop

1. Write the plan to the orchestration files **before** spawning (Step 8). A lead that keeps the plan only in
   context loses it to truncation; Anthropic's lead agent persists its plan for exactly this reason.
2. Spawn the surviving lanes, up to three to five at a time, each with its brief.
3. While workers run, do lead work only: reconcile returns, update state, prepare the next gate. Do not start
   reading the files a worker owns — that is the context this skill exists to keep out of the lead.
4. As each return arrives, put it through Step 5 before anything downstream depends on it.
5. A failed lane gets a **re-brief**, not a rerun. Repeating an identical prompt repeats the result. If the
   blocker is a missing permission, tool, or environment, it is not a worker problem: finish the independent
   lanes, then report it.

---

## Step 5: Accept or reject each return (GATE)

A worker's own verdict is not evidence. Judge the artifact.

- **Require printed output.** A claim that tests pass, with no command and no output, is unverified. `review.json`
  contents, a test summary line, a diff stat, a URL — those are evidence.
- **Spot-check load-bearing claims** against the code or the primary source yourself. Workers report success too
  generously.
- Use four verdicts: **pass** (threshold verified), **fail** (contradicted by evidence), **needs-evidence** (the
  claim may hold but nothing shows it), **disputed** (two sources conflict). Only `pass` opens a gate.
- Settle a dispute with deterministic evidence first — a test, a command, the file itself — and only then with one
  stronger adjudication.
- **Feedback names three things**: what does not match, which evidence shows it, and what would count as fixed.
  Then re-verify the changed artifact, not the worker's assurance that it is fixed.
- When a fix changes something downstream lanes consumed, **re-run the consumers' verification**. An upstream fix
  silently invalidates every gate that was opened on the old version.

---

## Step 6: Independent evaluation

For costly, subjective, cross-boundary, or borderline results, have something other than the builder judge them.

- **Fix the evaluation contract before building**: criteria, thresholds, method, and which checks are manual. Any
  missed threshold fails the unit.
- Give the evaluator the **contract and the observable artifacts**, not the builder's conclusion, and ask it to
  find failures and missing evidence.
- Reach for what this repo already has: `/review` for a branch, `security-reviewer` for vulnerability scans,
  `counter-argument` to stress-test a proposal, and Codex as a second model for design questions per
  `rules/behavior.md`. A fresh subagent reviewing only the diff is more independent than the session that wrote it.
- Prefer **pass/fail against a threshold, or pairwise comparison**, over a score out of ten. When comparing
  candidates, hold length and order constant: judges favor longer answers and whichever came first.
- Deterministic work is judged by tests and reproducible commands. Checks that need a device, an account, or an
  unavailable environment are **manual** — never report them as passed.

---

## Step 7: Integration verification

Run the project's real commands from `AGENTS.md` or a host-specific override at the boundaries the run changed.

- A passing check in one lane, module, or repository never covers another.
- Verify every contract that changed from both sides: the producer's output and a consumer actually using it.
- Separate **not run**, **failed**, and **blocked by environment**. Do not let a unit test stand in for an
  integration that was never executed.

---

## Step 8: Checkpoint and resume

Long runs need files, not memory. Read [state-contract.md](references/state-contract.md) before creating or
updating them, and put the directory where the user says, or `docs/orchestrate/<slug>/` in the repository that
owns the main question — then tell the user the path.

`BRIEF.md` (objective, mode, criteria, stop conditions, scope), `STATE.json` (authoritative operational state),
`EVIDENCE.md` (claims with sources and dates), `DECISIONS.md` (append-only), `RUNLOG.md` (one entry per
checkpoint).

- A goal that finishes in this session does not need the files. A goal that spans sessions, hosts, or
  repositories does.
- **Checkpoint** after each material finding, gate, decision, or repository change, and before the session ends:
  update `STATE.json` first, then evidence and decisions, then append one `RUNLOG.md` entry with commands,
  results, changed paths, blockers, and the next concrete action. Verify the JSON parses and that nothing is
  marked `running` that is not running.
- **Resume** by reading `BRIEF.md`, `STATE.json`, the last `RUNLOG.md` entry, and the open decisions; then
  recompute each repository's branch, `HEAD`, and changes. Git and tests override the recorded status. Re-run the
  last passing check before new work, and continue from the first unblocked next action — do not redo finished
  work to rebuild context.
- On failure, resume from the last checkpoint. Restarting a long run from zero throws away paid-for progress.
- Do not wrap up early because the context is long. Checkpoint and continue.

---

## Step 9: Disposition and stop

At each gate, choose one and record why: `continue`, `pivot`, `stop-success`, `stop-not-viable`, `blocked`,
`handoff-to-dev`.

- Stop when the last few iterations stopped improving the measured result. Remaining budget is not a reason to
  continue, and partial progress is not a reason to keep going.
- **Escalate to the user** when an action is irreversible or high-impact (merge, deploy, data migration, anything
  public), when a lane has failed repeatedly for the same reason, or when a decision needs authority this run does
  not have. Ask once, with the options and your recommendation.
- `handoff-to-dev` is the normal ending for a PoC that worked: write one handoff per independently reviewable
  change — problem, chosen approach, acceptance criteria, in and out of scope, cross-repository order, required
  tests and manual checks, rejected alternatives — then file it with `/ai-dev:issue` and implement it with `/dev`.
  Research does not authorize implementation; `rules/ai-ops.md → Research → Implementation Gate` still applies.

---

## Final report

State: the disposition; each success criterion with its evidence or its gap; what each lane produced; the
verification actually executed, with results, grouped by repository; changed paths; what is unverified, manual, or
blocked; the exact next action or handoff; and the path to the orchestration files.

Do not claim completion when a required verification was blocked — say "implemented, verification incomplete" and
name what is missing. Mention only the external operations this conversation authorized, with their identifiers.
