# competitive-audit — Research Method

How Phase 2 (competitive research) and Phase 4 (improvement research) are actually carried out.
Phase 3 (gap analysis) and Phase 5 (issue filing) stay in SKILL.md — they are decisions, not procedures.

## Contents
- Phase 2a: Commercial competitors
- Phase 2b: OSS competitors
- Phase 2c: User pain point synthesis
- Phase 2d: UX quality comparison
- Phase 2e: Store review analysis
- Research rules
- Phase 4a: Improvement research categories

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

---

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
