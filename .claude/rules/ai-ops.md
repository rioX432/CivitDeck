# AI-Driven Development & Operations

## Core Value Guard

**Before any feature work, check the project's `CLAUDE.md` → `## Core Values` section.**

- If Core Values are not defined, ask the user to define them first
- Every feature must directly strengthen a Core Value (one-step test: no indirect reasoning)
- If a feature doesn't pass the one-step test, add it to `## Won't Do` with reasoning

## Research → Implementation Gate

**Research documents (docs/research/, RESEARCH.md, etc.) must NOT be directly implemented.**

Research flow:
1. Research findings → file as GitHub Issue (with Core Value alignment and complexity cost)
2. Issue passes weekly review by the user
3. Only then can it enter the development flow below

**No shortcut from "interesting research" to "let's build it".**

## Development Flow (Issue-Driven)

**All work is driven by GitHub Issues.** Pick the lowest-numbered unblocked issue and work through it.

**WIP limit: 1 issue at a time.** Finish (merge or close) the current issue before starting the next.

1. `gh issue list` to find unstarted issues
2. **Skip issues labeled `won't`**
3. Read the issue, understand requirements, plan implementation
4. **Verify Core Value alignment** — if the issue lacks a "Core Value Alignment" section, ask the user before proceeding
5. Confirm design/plan with **Codex MCP** (see rules/behavior.md for usage):
   - **Required**: architecture changes, new patterns, migrations, security-sensitive design
   - **Optional**: complex trade-offs where existing patterns don't clearly apply
   - **Skip**: existing-pattern implementations, small bug fixes, naming, test strategy (auto-decide from codebase)
   - **Codex unavailable?** Use WebSearch to verify against official docs, document rationale in PR
6. Implement according to plan
7. Verify build and lint pass
8. Run `/ai-dev:review` for self-review
9. Fix any review findings; extract reusable insights into `docs/claude/review_points.md`
10. Create PR (`Closes #N` in body)

## review_points.md Workflow
- Don't copy review comments verbatim — **extract reusable prevention insights**
- Reference `review_points.md` during design and implementation to avoid repeat mistakes

## Automated Operations (post-release)

| Pipeline | Method | Frequency |
|---|---|---|
| Crash monitoring | Firebase Crashlytics → Claude analysis → auto-fix PR or Issue | Real-time |
| User feedback | App Store / Google Play reviews → sentiment analysis → Issue | Daily |
| In-app feedback | Feedback form → GitHub Issues API | On submission |
| Metrics | Store API data collection → trend analysis → report | Daily |

## Feature Prioritization: 2-Axis Evaluation

Next features are decided by **two axes: "User Requests" and "Metrics"**. No features based on gut feeling.

**Axis 1: User Requests (Qualitative)**
- Request volume (vote count from feedback, reviews, social)
- Sentiment intensity (star rating, emotional analysis)
- User segment (free/paid, engagement level)

**Axis 2: Metrics (Quantitative)**
- Retention rate (D1/D7/D30)
- Feature usage rate
- Conversion rate (free → paid)
- Crash-free rate
- Task completion rate

**Rule:** Features where both axes don't align are not implemented. Exception: crash/security fixes act on metrics alone.

**Additional filter:** Even if both axes align, the feature must pass the Core Value one-step test. A popular request outside Core Value scope goes to `## Won't Do`, not the backlog.

## Effort Level Selection

Match effort level to task complexity:

| Level | Use When |
|---|---|
| `high` (default) | Standard development, bug fixes, small features |
| `xhigh` | Complex refactoring, cross-module changes, architecture decisions |
| `max` | Critical debugging, security-sensitive code, unfamiliar large codebase |

Set via `/effort xhigh` or per-agent with model selection.

## Agent Teams (Parallel Development)

Use Claude Code Agent Teams for parallel development when tasks are independent:

| Teammate | Scope | File Access |
|---|---|---|
| <!-- fill per project --> | | |

- **No file conflicts**: each teammate edits only their assigned directories
- Shared API changes require Lead coordination
- Cost: 4-15x token consumption — use for high-value tasks only

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

Anti-patterns (rewrite before use): `make the code better` (no proof), `when the tests pass` (no command named, no fresh-proof directive), `fix all the bugs` (unbounded, subjective).

## Model Selection for Agents

Match the model tier to the sub-task instead of defaulting everything to one tier. Use aliases (`haiku`/`sonnet`/`opus`) so agents track the latest generation automatically.

| Tier | Cost (per MTok, in/out) | Use for |
|---|---|---|
| `haiku` | $1 / $5 | Mechanical collection: URL existence checks, web/SNS scans, data gathering with no analysis |
| `sonnet` | $3 / $15 | Review, analysis, test writing — near-Opus coding quality since Sonnet 5 |
| `opus` | $5 / $25 | Long-horizon autonomous implementation, architecture decisions |

For long autonomous runs (Opus tier): state the full task specification up front in one well-specified prompt and run at high effort — clear goals up front produce more efficient and more accurate output than progressively revealed instructions.
