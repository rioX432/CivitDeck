# AI-Driven Development & Operations

## Development Flow (Issue-Driven)

**All work is driven by GitHub Issues.** Pick the lowest-numbered unblocked issue and work through it.

1. `gh issue list` to find unstarted issues
2. Read the issue, understand requirements, plan implementation
3. Confirm design/plan with **Codex MCP** before starting
4. Implement according to plan
5. Verify build and lint pass
6. Run `/ai-dev:review` for self-review
7. Fix any review findings; extract reusable insights into `docs/claude/review_points.md`
8. Create PR (`Closes #N` in body)

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

## Agent Teams (Parallel Development)

Use Claude Code Agent Teams for parallel development when tasks are independent:

| Teammate | Scope | File Access |
|---|---|---|
| <!-- fill per project --> | | |

- **No file conflicts**: each teammate edits only their assigned directories
- Shared API changes require Lead coordination
