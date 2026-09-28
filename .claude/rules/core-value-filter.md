# Product Policy: Core Value Filter

An optional product policy: it decides which features are worth building. It is not an engineering rule. Bug fixes,
security and crash fixes, investigation, implementation, verification, review, refactoring, and documentation never
wait on it.

## When this policy applies

Resolve it from repository guidance (`AGENTS.md`, host-specific `CLAUDE.md`) in this order; the first match wins:

1. **`## Product policy` names it** — `none` turns it off; `core-value-filter` turns this file on; a repository
   path (for example `docs/product-policy.md`) replaces this file with the repository's own policy.
2. **Compatibility default** — with no `## Product policy` section, this file applies when the repository has a
   `## Core Values` or `## Won't Do` section, or carries this file as `.claude/rules/core-value-filter.md` (projects
   synced from ai-dev-templates). Existing installations therefore keep their current enforcement.
3. Otherwise the repository is generic-only: skip every gate below and do not ask for Core Values.

Repository truth always outranks this file: the repository's Core Values, Won't Do entries, and any replacement
policy are the content; this file only supplies the procedure.

## Core Value Guard

- Every feature must directly strengthen a Core Value (one-step test: no indirect reasoning).
- A feature that fails the one-step test goes to `## Won't Do` with the reason, not to the backlog.
- If the policy applies but no Core Values are defined, ask the user to define them before filing or starting
  **feature** work. Bug, security, crash, and non-feature engineering work proceed.

## Won't Do Registry

Features explicitly decided not to build are recorded under `## Won't Do` with the reason. An issue or proposal that
matches an entry is not filed or started; report the entry instead. An issue labeled `won't` is treated the same.

## Research → Weekly Review

Research findings become issues through `/ai-dev:issue` with a Core Value Alignment section and a complexity cost.
An issue from research enters development only after the user's weekly review accepts it.

## Feature Prioritization: 2-Axis Evaluation

Next features are decided by two axes, **User Requests** and **Metrics**, not by gut feeling.

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

**Rule:** Features where both axes don't align are not implemented. Exception: crash/security fixes act on metrics
alone.

**Additional filter:** Even if both axes align, the feature must pass the Core Value one-step test. A popular
request outside Core Value scope goes to `## Won't Do`, not the backlog.
