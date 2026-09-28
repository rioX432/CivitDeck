# AI-Driven Development & Operations

## Product Policy (optional)

This flow does not require Core Values. Which features are worth building is a product decision, owned by the
repository's own guidance or by the optional Core Value filter policy (`policies/core-value-filter.md` in the
provider, `.claude/rules/core-value-filter.md` in synced projects). That file states when it applies; where it applies, its gates run at the steps marked below. Where it does not, skip
them and never block engineering work on undefined Core Values.

## Research → Implementation Gate

**Research documents (docs/research/, RESEARCH.md, etc.) must NOT be directly implemented.**

Research findings become a GitHub Issue **via `/ai-dev:issue`** first, and enter the development flow below only
once that issue is admitted (plus the product policy's review, where it applies).

**No shortcut from "interesting research" to "let's build it".**

## Development Flow (Issue-Driven)

**All work is driven by GitHub Issues.** Work the issue that was admitted to you — by the user, a standalone
wrapper, or a Control Plane. Choosing which issue comes next, and how many run at once, belongs to whoever admitted
the work, not to this rule.

1. Read the issue, understand requirements, plan implementation
2. **Run the sizing gate** (`/ai-dev:issue → Step 3`). An issue that fails it gets split before
   any code is written — an oversized issue produces an unreviewable PR and an unfinishable proof
3. **Product policy gate** (only where it applies) — a feature issue without a Core Value Alignment section goes
   back to the user before work starts
4. Confirm design/plan with **Codex** (see rules/behavior.md for usage):
   - **Required**: architecture changes, new patterns, migrations, security-sensitive design
   - **Optional**: complex trade-offs where existing patterns don't clearly apply
   - **Skip**: existing-pattern implementations, small bug fixes, naming, test strategy (auto-decide from codebase)
   - **Codex unavailable?** Use WebSearch to verify against official docs, document rationale in PR
5. Implement according to plan
6. Verify with the profile the change's risk requires ([rules/verification.md](verification.md))
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
