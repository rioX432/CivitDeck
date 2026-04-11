---
name: monitor
description: "KPI monitoring and issue proposal: analyze crash rates, store reviews, and metrics to suggest next priorities"
user-invocable: true
disable-model-invocation: true
allowed-tools:
  - Read
  - Grep
  - Glob
  - Agent
  - Bash(gh issue create:*)
  - Bash(gh issue list:*)
  - TaskCreate
  - TaskUpdate
  - TaskList
  - AskUserQuestion
  - ToolSearch
---

# /monitor — KPI Monitoring & Prioritization (PoC)

> **Status: Proof of Concept** — Requires Firebase MCP, Store API access, and analytics integration.
> This skill defines the target workflow for fully autonomous AI-driven operations.

Monitor project KPIs and propose next actions based on data.

## Steps

### 1. Crashlytics Analysis (if Firebase configured)

- Use Firebase MCP to fetch recent crash data
- Identify top crashes by frequency and user impact
- For critical crashes (crash-free rate < 99.5%), create GitHub Issues automatically

### 2. Store Review Analysis (if applicable)

- Collect recent App Store / Google Play reviews
- Perform sentiment analysis: positive / neutral / negative
- Identify recurring complaints or feature requests
- For strong negative trends, create GitHub Issues with `user-feedback` label

### 3. Metrics Dashboard

Collect and report on key metrics:

| Metric | Source | Threshold |
|---|---|---|
| Crash-free rate | Crashlytics | < 99.5% → Issue |
| D1 Retention | Analytics | < 40% → investigate |
| D7 Retention | Analytics | < 20% → investigate |
| D30 Retention | Analytics | < 10% → investigate |
| Feature usage rate | Analytics | < 5% → consider removal |
| Conversion rate | Store | declining → investigate |

### 4. Feature Prioritization (2-Axis)

Score potential features on two axes:

**Axis 1: User Requests (Qualitative)**
- Request volume (votes, mentions, reviews)
- Sentiment intensity
- User segment (free/paid, engagement level)

**Axis 2: Metrics (Quantitative)**
- Impact on retention
- Impact on conversion
- Impact on crash-free rate
- Development effort (S/M/L)

**Rule:** Only features where both axes align get proposed. Exception: crash/security fixes act on metrics alone.

### 5. Issue Proposal

For each recommended action:
```bash
gh issue create \
  --title "{priority-label}: {concise description}" \
  --body "{rationale with data points}" \
  --label "ai-proposed,{priority}"
```

## Output Format

```
## KPI Monitor Report

**Date:** YYYY-MM-DD
**Period:** last 7 days

### Health
| Metric | Current | Trend | Status |
|---|---|---|---|

### Issues Created
| # | Title | Priority | Rationale |
|---|---|---|---|

### Recommended Next Actions
1. ...
2. ...
```
