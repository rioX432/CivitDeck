# AI-Driven Development & Operations

## Core Value Guard

**Before any feature work, check the project's `CLAUDE.md` → `## Core Values` section.**

- If Core Values are not defined, ask the user to define them first
- Every feature must directly strengthen a Core Value (one-step test: no indirect reasoning)
- If a feature doesn't pass the one-step test, add it to `## Won't Do` with reasoning

## Research → Implementation Gate

**Research documents (docs/research/, RESEARCH.md, etc.) must NOT be directly implemented.**

Research flow:
1. Research findings → file as GitHub Issue **via `/ai-dev:issue`** (with Core Value alignment and complexity cost)
2. Issue passes weekly review by the user
3. Only then can it enter the development flow below

**No shortcut from "interesting research" to "let's build it".**

## Development Flow (Issue-Driven)

**All work is driven by GitHub Issues.** Work the issue that was admitted to you — by the user, a standalone
wrapper, or a Control Plane. Choosing which issue comes next, and how many run at once, belongs to whoever admitted
the work, not to this rule.

1. Read the issue, understand requirements, plan implementation
2. **Run the sizing gate** (`/ai-dev:issue → Step 3`). An issue that fails it gets split before
   any code is written — an oversized issue produces an unreviewable PR and an unfinishable proof
3. **Verify Core Value alignment** — if the issue lacks a "Core Value Alignment" section, ask the user before proceeding
4. Confirm design/plan with **Codex** (see rules/behavior.md for usage):
   - **Required**: architecture changes, new patterns, migrations, security-sensitive design
   - **Optional**: complex trade-offs where existing patterns don't clearly apply
   - **Skip**: existing-pattern implementations, small bug fixes, naming, test strategy (auto-decide from codebase)
   - **Codex unavailable?** Use WebSearch to verify against official docs, document rationale in PR
5. Implement according to plan
6. Verify build and lint pass
7. Run `/ai-dev:review` for self-review
8. Fix any review findings; extract reusable insights into `docs/claude/review_points.md`
9. Create PR (`Closes #N` in body)

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
