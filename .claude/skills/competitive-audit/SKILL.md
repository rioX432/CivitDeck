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
  - mcp__playwright__browser_navigate
  - mcp__playwright__browser_snapshot
  - mcp__playwright__browser_take_screenshot
  - mcp__playwright__browser_close
  - mcp__gemini-deepsearch__deep_search
  - mcp__perplexity__perplexity_research
  - mcp__perplexity__perplexity_search
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

### 2d. UX Quality Comparison

Compare UI/UX quality between this project and competitors:

**If Playwright MCP is available and competitors are web apps:**
- Navigate to competitor apps, capture screenshots of key flows
- Compare information architecture, navigation, onboarding, error handling

**Otherwise:**
- WebSearch for competitor UI screenshots, design reviews, and UX teardowns
- Collect app store screenshots for visual comparison

For each competitor, evaluate:
- First-time user experience (onboarding flow)
- Core task completion flow (how many steps?)
- Error handling and edge cases (what happens when things go wrong?)
- Visual design quality and consistency
- Accessibility posture (public a11y statements, known issues)

### 2e. Store Review Analysis

Analyze app store reviews for UI/UX insights (skip if not a mobile/desktop app):

1. WebSearch: `"{app name}" app store review`, `"{app name}" user feedback`
2. For each app (self + competitors), extract:
   - UI/UX bug reports (crashes, display issues)
   - Usability complaints (confusing, hard to use)
   - Feature requests related to UX
   - Positive feedback (what users love about the UX)
3. Compare: what do users praise/criticize about each app's UX?

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
- **UX quality gaps** (from Phase 2d comparison)
- **User sentiment gaps** (from Phase 2e store reviews)

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

4. **Latest research & academic papers**
   - Relevant papers from arXiv, CHI, ICSE, UIST
   - Practically implementable ideas
   - Use `mcp__gemini-deepsearch__deep_search` for thorough literature search

5. **Prior implementations**
   - OSS projects implementing similar approaches
   - Benchmark results

6. **Emerging technologies (no existing examples required)**
   - Technologies that don't have production examples yet but show promise
   - Use `mcp__gemini-deepsearch__deep_search` and/or `mcp__perplexity__perplexity_research` to find:
     - New frameworks, libraries, or APIs announced in the last 6 months
     - Research prototypes that could become practical soon
     - Platform capabilities (Android/iOS) not yet widely adopted
     - AI/ML techniques applicable to this domain
   - For each: assess feasibility, potential impact, and first-mover advantage
   - **This is where we find ideas to get AHEAD of competitors, not just catch up**

### 4b. Output

For each improvement idea:
- Summary and expected impact
- Implementation difficulty (effort estimate)
- Reference links (GitHub, papers, docs)
- Recommended priority
- **Origin**: catch-up (competitors already have it) / frontier (no one has it yet)

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
