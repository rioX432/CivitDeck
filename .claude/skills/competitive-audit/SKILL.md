---
name: competitive-audit
description: "Core Value-filtered competitive analysis: research user pain points, identify gaps within Core Value scope, file max 3 high-impact issues"
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
Phase 0: Core Value Check (GATE — stop if undefined)
Phase 1: Project Audit (assess current completeness)
Phase 2: Competitive Research (user pain points & competitor survey)
Phase 3: Gap Analysis (Core Value-filtered advantages & gaps)
Phase 4: Improvement Research (Core Value-scoped improvements only)
Phase 5: Issue Filing (max 3 issues + Won't Do recording)
```

---

## Phase 0: Core Value Check (GATE)

**Goal**: Ensure the project has defined Core Values before proceeding.

1. Read the project's `CLAUDE.md` and look for `## Core Values` section
2. **If missing**: Stop and ask the user to define Core Values (max 3) before running this audit. Provide examples:
   ```
   ## Core Values
   1. {What is the ONE thing this product does better than anything else?}
   2. {What is the second most important thing?}
   3. {Optional: third}
   ```
3. **If present**: Extract Core Values and use them as the filter for ALL subsequent phases

**Core Values are the single filter for this entire audit.** Every finding, gap, and issue must pass through: "Does this directly strengthen a Core Value (one step, no indirect reasoning)?"

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
- **Complexity indicators**: package/module count, dependency depth, configuration surface area

### 1c. Output

Create TaskUpdate with completeness score (1-10) and key findings:
```
Completeness: X/10
Core Values: [extracted from CLAUDE.md]
Strengths: [list]
Weaknesses: [list]
Unimplemented: [list]
Complexity: [package count, module count, config surface area]
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
  - **User pain points**: Common complaints, unresolved issues, feature requests from reviews/forums

### 2b. OSS Competitors

- Search GitHub for similar projects
- Star count, last update, activity level
- Tech stack, feature scope
- Differences from this project
- **Open issues & discussions**: What are users struggling with? What PRs are requested but not merged?

### 2c. User Pain Point Synthesis

**This is the primary input for issue filing.** Not competitor feature lists.

- Aggregate unresolved user pain points from 2a and 2b
- Search forums, Reddit, Discord, Stack Overflow for user complaints in this domain
- For each pain point:
  - How many users report it?
  - How severe is it? (blocker vs annoyance)
  - **Does it relate to one of our Core Values?** (Yes/No — if No, record in Won't Do candidates)
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
- **Core Value depth**: How much deeper are we than competitors on our Core Values?

### 3b. Disadvantages (Where We Lose)

List areas where competitors are stronger, **classified by Core Value relevance**:

**Core Value Related (actionable):**
- Gaps that directly weaken a Core Value
- UX quality gaps on Core Value flows (from Phase 2d)
- User pain points on Core Value features (from Phase 2c)

**Non-Core Value (record but do NOT file issues):**
- Missing features outside Core Value scope
- Platform limitations unrelated to Core Values
- Distribution / awareness gaps
- Quality / maturity gaps in non-core areas

### 3c. Core Value Distance Test

For each disadvantage in 3b "Core Value Related", apply the **one-step test**:

> "Does fixing this DIRECTLY strengthen a Core Value, without intermediate reasoning?"

- ✅ Direct: "Translation accuracy improvement → Core Value: accurate translation" (1 step)
- ❌ Indirect: "Add meeting summary feature → helps users → they'll use translation more" (2+ steps)

**Only ✅ Direct items proceed to Phase 4 and 5.**

### 3d. Positioning Map

Create a 2-axis positioning map (ASCII art) to identify white space:
```
              Axis A                    Axis B
              ┌──────────────────────────────────┐
  Category 1  │ [Competitor A]     │             │
              │ [Competitor B]     │ ★ Us        │
              └──────────────────────────────────┘
```

### 3e. Output

- Advantages / disadvantages comparison table (with Core Value relevance column)
- Positioning map
- Strategic implications (deepen Core Value differentiation, not broaden scope)
- **Won't Do candidates**: Non-Core items from 3b with brief rationale

---

## Phase 4: Improvement Research

**Goal**: Research concrete improvement opportunities **within Core Value scope only**.

If `$ARGUMENTS` is provided, focus on that area (still filtered by Core Values).
If not specified, focus on the biggest Core Value-related gap from Phase 3.

**Scope constraint**: Only research improvements that passed the Phase 3c one-step test.

Use Agent (subagent_type: general-purpose) with extensive WebSearch:

### 4a. Research Categories

Cover the following, **all scoped to Core Value improvements**:

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

4. **UX deepening opportunities**
   - How can the Core Value experience be made smoother, faster, more delightful?
   - Competitor UX patterns worth adopting (from Phase 2d)
   - User pain points that degrade Core Value experience (from Phase 2c)

5. **Latest research & academic papers**
   - Relevant papers from arXiv, CHI, ICSE, UIST
   - Practically implementable ideas
   - Use `mcp__gemini-deepsearch__deep_search` for thorough literature search

6. **Prior implementations**
   - OSS projects implementing similar approaches
   - Benchmark results

7. **Emerging technologies (no existing examples required)**
   - Technologies that don't have production examples yet but show promise
   - Use `mcp__gemini-deepsearch__deep_search` and/or `mcp__perplexity__perplexity_research` to find:
     - New frameworks, libraries, or APIs announced in the last 6 months
     - Research prototypes that could become practical soon
     - Platform capabilities (Android/iOS) not yet widely adopted
     - AI/ML techniques applicable to this domain
   - For each: assess feasibility, potential impact, and first-mover advantage
   - **Focus on deepening Core Value differentiation, not broadening scope**

### 4b. Output

For each improvement idea:
- Summary and expected impact
- **Core Value alignment**: Which Core Value does this strengthen? (must be explicit)
- Implementation difficulty (effort estimate)
- **Complexity cost**: New dependencies, config surface, maintenance burden
- Reference links (GitHub, papers, docs)
- Recommended priority
- **Origin**: deepen (strengthen existing Core Value) / frontier (new approach to Core Value, no one has it yet)

---

## Phase 5: Issue Filing

**Goal**: File research results as GitHub issues. **Maximum 3 issues per audit run.**

### 5a. Issue Selection (GATE)

From Phase 3 and Phase 4 results, select **at most 3** issues to file.

**Selection criteria (all must be Yes):**
1. Does it directly strengthen a Core Value? (Phase 3c one-step test passed)
2. Is the complexity cost justified? (Phase 4b complexity cost assessment)
3. Is this a user pain point, not just a competitive gap? (Phase 2c evidence exists)

If more than 3 candidates pass, rank by:
- Severity of user pain point (blocker > annoyance)
- Core Value impact (direct improvement > marginal improvement)
- Complexity cost (lower is better)

**Rejected candidates**: Record in the Won't Do section of the final report with reasoning.

### 5b. Classification & Priority

Check existing labels with `gh label list`. Create missing labels as needed.

Priority labels:
- **P0**: Highest impact, should start immediately
- **P1**: Important but can follow P0
- **P2**: Worth pursuing mid-term
- **won't**: Explicitly decided not to implement (add to CLAUDE.md `## Won't Do`)

### 5c. Issue Template

Each issue follows this structure:

```markdown
## Summary
{1-2 line overview}

## Core Value Alignment
{Which Core Value this strengthens and how (one step, direct)}

## Motivation
{Why this improvement is needed, with USER PAIN POINT evidence (not just "competitor has it")}

## Complexity Cost
{New dependencies, config surface area, maintenance burden, affected existing features}

## Tasks
- [ ] {Concrete task 1}
- [ ] {Concrete task 2}
...

## References
- {Related URLs, papers, GitHub repos}
```

### 5d. Issue Filing Rules

- Use `gh issue create` to file
- **Maximum 3 issues per audit run** — quality over quantity
- 1 issue = 1 independent improvement item
- Cross-reference related issues with `#number` in body
- Add `research` label to items requiring investigation
- Pass body via HEREDOC

### 5e. Won't Do Filing

For items that were considered but rejected, append to the project's `CLAUDE.md` under `## Won't Do`:

```markdown
- **{Feature/idea}**: {Why not — e.g., "outside Core Value scope", "complexity cost too high", "indirect benefit only"}
```

This prevents future audits from re-proposing the same items.

### 5f. Output

Summary table of filed issues:
```
| # | Title | Priority | Core Value | Complexity Cost |
|---|-------|----------|------------|-----------------|
| #N | ... | P0 | ... | Low/Med/High |
```

Won't Do items added: [count]

---

## Final Report

After all phases, present to user:

1. **Core Values** (extracted from CLAUDE.md)
2. **Completeness score** (X/10) with rationale
3. **Competitor map** (commercial + OSS comparison table)
4. **Positioning map**
5. **Advantages / disadvantages summary** (with Core Value relevance)
6. **Filed issues list** (max 3, with Core Value alignment and complexity cost)
7. **Won't Do additions** (items considered but rejected, with reasons)
8. **Strategic recommendations**: How to deepen Core Value differentiation (not broaden scope)

---

## Error Handling

- If WebSearch fails for a category, skip and note it
- If `gh` command fails, output issue content as text for manual creation
- If interrupted mid-phase, save progress via TaskUpdate for resumption
