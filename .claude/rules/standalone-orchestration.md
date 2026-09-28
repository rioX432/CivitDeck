# Standalone Orchestration Policy

This policy applies only when no Control Plane assigned the work: a person running `/dev`, `/dev-all`, or
`/orchestrate` directly in Claude Code or Codex. When a Control Plane such as Buddy assigns a task, it owns every
decision below — admission, order, WIP, concurrency, model and effort, retry, worktrees, budgets, resume, and
approvals — and this file is not part of its context. Work assigned by a Control Plane never starts `/dev`,
`/dev-all`, `/orchestrate`, or `/goal` as a nested loop.

In the plugin this file is `standalone/orchestration.md`. Synced projects receive it as
`.claude/rules/standalone-orchestration.md`, so standalone sessions there keep loading it automatically.

## Admission

Work enters a standalone loop only through an explicit admission:

- **Explicit issue IDs** named by the user (`/dev #42`, `/dev-all #42 #43`), or
- **The ready label**: open issues labeled `ready`, or the label named under `## Ready label` in the repository's
  `AGENTS.md` / `CLAUDE.md`. A label that does not exist, or has no open issues, admits nothing.

Never admit every open issue. An argument-free batch run with nothing admitted stops and asks for issue IDs or the
ready label. Admission still skips issues labeled `won't` and `epic` (work an epic's children instead).

## Order and WIP

- **WIP limit: 1 issue at a time.** Finish (merge or close) the current issue before starting the next.
- Order admitted issues by dependency (`blocked by #N`, `depends on #N`, `after #N`), then by ascending number.
  Circular dependencies are skipped and reported.
- Each issue gets its own branch, PR, and merge cycle, starting from the latest default branch.

## Retry, turn caps, and resume

- Stop and report after 3 consecutive failures of the same check; a repeated identical attempt repeats the result.
- Every autonomous loop has a turn cap, and it stops on the turn it reaches the cap with a blocker summary.
- A run that spans sessions resumes from its checkpoint files and recomputes git and PR state before continuing;
  recorded status never overrides what git, tests, and GitHub show now.

## Effort Level Selection

Match effort level to task complexity:

| Level | Use When |
|---|---|
| `high` (default) | Standard development, bug fixes, small features |
| `xhigh` | Complex refactoring, cross-module changes, architecture decisions |
| `max` | Critical debugging, security-sensitive code, unfamiliar large codebase |

Set via `/effort xhigh` or per-agent with model selection.

## Agent Teams (Parallel Development)

Start with one agent. Use `/orchestrate` or Claude Code Agent Teams only when evaluation or the task structure
shows that independent lanes will outperform one context. Every lane must have a distinct output, a non-overlapping
write boundary (or be read-only), and no need for mid-flight coordination.

| Teammate | Scope | File Access |
|---|---|---|
| <!-- fill per project --> | | |

- **No file conflicts**: each teammate edits only its assigned paths; isolate concurrent writers in git worktrees
- Shared API changes require one named contract owner and an integration check
- Set concurrency, turn, tool, and retry limits; parallelism is a cost/latency trade-off, not a default
- The lead verifies evidence and integrates results; worker self-reports do not open a completion gate

## /goal for Autonomous Execution

Use `/goal` to set a completion condition for unattended task execution. After each turn, a small fast evaluator model (Haiku by default) reads the condition plus the conversation and returns yes/no. **The evaluator cannot run tools — it judges only what appears as text in the transcript** `[official]`. A condition is only as good as the evidence Claude prints.

Build every condition from 5 elements:

| # | Element | Example fragment |
|---|---------|------------------|
| 1 | End state (measurable, true/false) | "every test under tests/auth passes" |
| 2 | Proof command — must actually exist; resolve from CLAUDE.md Commands / CI config, never guess | "run `./gradlew test`" |
| 3 | Evidence signal (exact success output) | "the summary line shows `0 failed`" |
| 4 | Guardrail + its proof | "do not modify test files — show `git diff --stat` each turn" |
| 5 | Stop clause **as an OR-branch of the condition** | "— or stop after 20 turns, then summarize the blocker" |

Copy-paste template:

```
/goal <end state>. Prove it by, in the most recent turn, running <command> and
showing its output contains <exact signal> — or stop after <N> turns or if
<no-progress signal>, then summarize the blocker. Constraints: <what must not change>.
```

Rules (verified against the official docs and small evaluator probes):

- **Stop clause must be OR-joined into the condition** ("… shows `0 failed` — or stop after N turns"). Written as a free-standing sentence it is never treated as a completion path and the loop outlives its cap `[tested]`
- **Stop at the cap, on that turn, and print the blocker summary.** Overshooting the cap and stopping later can prevent the goal from ever completing `[tested]`
- **Re-run the proof command in the most recent turn after any change.** The evaluator tracks recency on its own — an unverified change stalls the loop with "no" forever `[tested]`
- **Evidence = actual command output in the transcript.** A narrated "tests pass" without output is not accepted; a printed test summary, `review.json` contents, or a PR URL is `[tested]`
- Subcommands: `/goal` (status), `/goal clear` (aliases: `stop`/`off`/`reset`/`none`/`cancel`). **There is no `--tokens` flag and no `pause` subcommand**
- Conditions can be up to 4,000 characters. Headless: `claude -p "/goal <condition>"` runs the loop to completion in one invocation `[official]`
- `/goal` is a session-scoped Stop-hook wrapper — it is unavailable when `disableAllHooks` is set, and there is no official support for it taking effect inside an `Agent()` sub-agent prompt

An issue's `Done when` is reused verbatim as the proof in the condition, which is why the issue sizing gate
requires a real command and its exact success output.

Anti-patterns (rewrite before use): `make the code better` (no proof), `when the tests pass` (no command named, no fresh-proof directive), `fix all the bugs` (unbounded, subjective).

## Model Selection for Agents

Match model capability to the sub-task instead of defaulting everything to one tier. Use supported aliases where
the host guarantees them, but record the resolved model and date in eval results. Model behavior, availability,
latency, and pricing are volatile; check current provider documentation instead of encoding them here.

| Capability | Use for |
|---|---|
| Fast / low-cost | Mechanical extraction, URL checks, and bounded collection with deterministic verification |
| Balanced | Review, analysis, test generation, and most implementation work |
| Frontier | Long-horizon implementation, ambiguous architecture, security-sensitive reasoning, and adjudication |

Prototype with the strongest justified model to establish a baseline, then use evals to determine whether a smaller
model still meets the accuracy target. For long autonomous runs, state the outcome, boundaries, tools, evidence,
and stop conditions up front; keep volatile facts and large references retrievable on demand.
