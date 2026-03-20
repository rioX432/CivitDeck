---
name: competitive-audit
description: "Audit project completeness, research competitors, identify advantages/gaps, research improvement opportunities, and file GitHub issues for actionable items"
argument-hint: "[focus-area]"
user-invocable: true
disable-model-invocation: true
allowed-tools:
  - Read
  - Glob
  - Grep
  - Agent
  - Bash(gh issue create:*)
  - Bash(gh issue list:*)
  - Bash(gh label list:*)
  - Bash(gh label create:*)
  - TaskCreate
  - TaskUpdate
  - TaskList
  - TaskGet
  - AskUserQuestion
  - WebSearch
  - WebFetch
---

# /competitive-audit — Competitive Analysis & Improvement Filing

Evaluate project completeness, research competitors, analyze advantages/gaps, investigate improvement opportunities, and file actionable GitHub issues.

**This is a long-running skill.** Use TaskCreate to track phases and persist progress across `/compact`.

## Trigger

- Manual: `/competitive-audit` or `/competitive-audit [focus-area]`
- When `$ARGUMENTS` is provided, Phase 4 research focuses on that specific area

## Process Overview

```
Phase 1: Project Audit (assess current completeness)
Phase 2: Competitive Research (thorough competitor & prior art survey)
Phase 3: Gap Analysis (advantages & disadvantages analysis)
Phase 4: Improvement Research (latest research & improvement ideas)
Phase 5: Issue Filing (create GitHub issues)
```

---

## Phase 1: Project Audit

**Goal**: Accurately understand the current state of the project.

### 1a. Project Overview

Use Agent (subagent_type: Explore) to investigate:

- **What the app does**: Purpose, target users, use cases
- **Architecture & tech stack**: Frameworks, languages, key libraries
- **Feature inventory**: Exhaustive list of implemented features
- **Unimplemented / TODO**: TODO comments in code, README placeholders, planned features
- **Supported platforms**: OS, devices, browsers
- **Build & distribution status**: Packaging, CI/CD, code signing, auto-update
- **Test status**: Test count, coverage, CI
- **Documentation**: README, ARCHITECTURE, roadmap presence and quality

### 1b. Codebase Health

- Lines of code (approximate)
- Module structure and maturity
- Recent commit trends (active? bug-fix focused? feature development?)
- Known limitations

### 1c. Output

Create TaskUpdate with completeness score (1-10) and key findings:
```
Completeness: X/10
Strengths: [list]
Weaknesses: [list]
Unimplemented: [list]
```

---

## Phase 2: Competitive Research

**Goal**: Thoroughly research direct competitors, indirect competitors, and OSS alternatives.

Use Agent (subagent_type: general-purpose) to WebSearch **in parallel**:

### 2a. Commercial Competitors

- List all commercial products solving the same problem
- For each product:
  - Pricing model (free tier, paid plans)
  - Key features
  - Supported platforms
  - Privacy model (cloud vs local)
  - Target use case
  - Common user complaints / reviews

### 2b. OSS Competitors

- Search GitHub for similar projects
- Star count, last update, activity level
- Tech stack, feature scope
- Differences from this project

### 2c. Market Trends

- Market size and growth rate for this space
- Common user pain points with existing solutions
- Privacy and regulatory trends
- Technology trends (on-device AI, edge computing, etc.)

### Research Rules

- Use WebSearch extensively, prioritize recent information
- Record each competitor with source URLs
- No guessing — only confirmed facts

---

## Phase 3: Gap Analysis

**Goal**: Clarify competitive positioning.

### 3a. Advantages (Where We Win)

Cross-reference Phase 1 & 2 results. List clear advantages over competitors:
- Technical differentiation (unique features)
- Architectural strengths
- Cost advantages
- Privacy / security strengths

### 3b. Disadvantages (Where We Lose)

List areas where competitors are stronger:
- Missing features
- Platform limitations
- Distribution / awareness gaps
- Quality / maturity gaps

### 3c. Positioning Map

Create a 2-axis positioning map (ASCII art) to identify white space:
```
              Axis A                    Axis B
              ┌──────────────────────────────────┐
  Category 1  │ [Competitor A]     │             │
              │ [Competitor B]     │ ★ Us        │
              └──────────────────────────────────┘
```

### 3d. Output

- Advantages / disadvantages comparison table
- Positioning map
- Strategic implications (white space, differentiation points)

---

## Phase 4: Improvement Research

**Goal**: Research concrete improvement opportunities for the area specified in `$ARGUMENTS`. If not specified, focus on the biggest improvement opportunity identified in Phase 3.

Use Agent (subagent_type: general-purpose) with extensive WebSearch:

### 4a. Research Categories

Cover all of the following thoroughly:

1. **Better models / libraries / tools**
   - Latest releases
   - Benchmark comparisons
   - Lightweight format support

2. **Optimization techniques**
   - Performance optimization
   - Memory optimization
   - Hardware acceleration

3. **Architecture improvements**
   - Hybrid approaches
   - Caching strategies
   - Streaming processing

4. **Latest research**
   - Relevant academic papers
   - Practically implementable ideas

5. **Prior implementations**
   - OSS projects implementing similar approaches
   - Benchmark results

### 4b. Output

For each improvement idea:
- Summary and expected impact
- Implementation difficulty (effort estimate)
- Reference links (GitHub, papers, docs)
- Recommended priority

---

## Phase 5: Issue Filing

**Goal**: File research results as GitHub issues.

### 5a. Issue Composition

Create actionable issues from Phase 3 (Gap Analysis) and Phase 4 (Improvement Research) results.

### 5b. Classification & Priority

Check existing labels with `gh label list`. Create missing labels as needed.

Priority labels:
- **P0**: Highest impact, should start immediately
- **P1**: Important but can follow P0
- **P2**: Worth pursuing mid-term
- **P3**: Future consideration, research stage

### 5c. Issue Template

Each issue follows this structure:

```markdown
## Summary
{1-2 line overview}

## Motivation
{Why this improvement is needed, with evidence from competitive analysis}

## Tasks
- [ ] {Concrete task 1}
- [ ] {Concrete task 2}
...

## References
- {Related URLs, papers, GitHub repos}
```

### 5d. Issue Filing Rules

- Use `gh issue create` to file
- 1 issue = 1 independent improvement item
- Cross-reference related issues with `#number` in body
- Add `research` label to items requiring investigation
- Pass body via HEREDOC

### 5e. Output

Summary table of filed issues:
```
| # | Title | Priority | Category |
|---|-------|----------|----------|
| #N | ... | P0 | ... |
```

---

## Final Report

After all phases, present to user:

1. **Completeness score** (X/10) with rationale
2. **Competitor map** (commercial + OSS comparison table)
3. **Positioning map**
4. **Advantages / disadvantages summary**
5. **Filed issues list**
6. **Strategic recommendations** (top priorities)

---

## Error Handling

- If WebSearch fails for a category, skip and note it
- If `gh` command fails, output issue content as text for manual creation
- If interrupted mid-phase, save progress via TaskUpdate for resumption
