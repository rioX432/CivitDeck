---
name: think
description: "Zero-base deep research: structured investigation → synthesis → proposal with counter-arguments"
argument-hint: "<topic> [github.com/owner/repo]"
user-invocable: true
disable-model-invocation: true
allowed-tools:
  - Read
  - Glob
  - Grep
  - Write
  - Agent
  - Bash(gh issue list:*)
  - Bash(gh api:*)
  - Bash(git log:*)
  - Bash(git diff:*)
  - TaskCreate
  - TaskUpdate
  - TaskList
  - TaskGet
  - AskUserQuestion
  - WebSearch
  - WebFetch
  - mcp__gemini-deepsearch__deep_search
  - mcp__perplexity__perplexity_research
  - mcp__perplexity__perplexity_search
  - mcp__perplexity__perplexity_ask
  - mcp__perplexity__perplexity_reason
  - mcp__playwright__browser_navigate
  - mcp__playwright__browser_snapshot
  - mcp__playwright__browser_take_screenshot
  - mcp__playwright__browser_close
---

# /think — Zero-Base Deep Research & Proposal

Zero-base thinking: discard assumptions, collect facts, synthesize essentials, propose with counter-arguments.

**This is a long-running skill.** Use TaskCreate to track phases and persist progress across `/compact`.

## Principles

1. **No guessing**: Every claim requires a source URL. Unknown = say "unknown"
2. **Zero-base**: Discard existing assumptions. Build conclusions bottom-up from facts
3. **MECE**: Design research axes with no gaps or overlaps
4. **Pyramid principle**: Conclusion → rationale → data
5. **Generator-Critic**: Always verify proposals with counter-arguments

## Modes

| Mode | Trigger | Purpose |
|------|---------|---------|
| Normal | `/think <topic>` | General deep research → analysis → proposal |
| Repo Analysis | `/think <topic> github.com/owner/repo` | Repository competitive analysis → roadmap proposal |

---

## Normal Mode

```
/think <topic>
  │
  Phase 1: Scoping + Deep Search (auto)
  │  ├─ Research design (MECE axes)
  │  └─ Gemini DeepSearch + Perplexity MCP parallel execution
  │
  Phase 2: Research (integration + supplemental)
  │  ├─ Cross-validate Gemini/Perplexity results
  │  ├─ deep-researcher for supplemental (SNS, local, latest)
  │  └─ source-verifier for all URL validation
  │
  Phase 3: Deep Dive (parallel analysis)
  │  ├─ case-analyzer × N (parallel)
  │  ├─ social-scanner (reputation research)
  │  └─ Additional Deep Search via MCP as needed
  │
  Phase 4: Synthesis (identify essentials) ← Main agent
  │
  Phase 5: Proposal (propose + counter-argument verification)
       └─ counter-argument agent
```

### Phase 1: Scoping + Deep Search

**Goal**: Design research axes and auto-execute deep searches.

1. Decompose the topic into 3-5 MECE research axes
2. For each axis, execute in parallel:
   - `mcp__gemini-deepsearch__deep_search` (free, 250/day) — primary
   - `mcp__perplexity__perplexity_research` — top 1-3 axes only (cost: ~$0.4-1.3/call)
3. TaskCreate for each axis to track progress

### Phase 2: Research Integration

**Goal**: Cross-validate and fill gaps.

1. Compare Gemini and Perplexity results for consistency
2. Launch `deep-researcher` agent for supplemental research:
   - SNS/community discussions
   - Japan-local information
   - Latest developments not in deep search results
3. Launch `source-verifier` agent to validate all URLs
4. Flag contradictions between sources

### Phase 3: Deep Dive

**Goal**: Detailed analysis of key cases and social impact.

Launch in parallel:
- `case-analyzer` agents for each important case/example (up to 5)
- `social-scanner` agent for community sentiment and reception
- Additional targeted deep searches if gaps remain

### Phase 4: Synthesis

**Goal**: Extract the essential insight from all research. Main agent executes this.

1. What is the structural pattern across all findings?
2. What is the root cause / driving force?
3. What does this mean for the topic's future?
4. Distill into 1-3 key insights with supporting evidence chains

### Phase 5: Proposal

**Goal**: Actionable proposals with counter-argument verification.

1. Generate 2+ proposals based on Phase 4 insights
2. Each proposal includes:
   - Summary and expected impact
   - Pros / cons / risks
   - Implementation approach
   - Evidence chain (fact → inference → conclusion)
3. Launch `counter-argument` agent to stress-test each proposal
4. Revise proposals based on counter-arguments

---

## Repo Analysis Mode

```
/think <topic> github.com/owner/repo
  │
  Phase 0: Repo Analysis (understand the repository)
  │  ├─ repo-analyzer: code, features, Issues/PRs
  │  ├─ Build feature map
  │  └─ ★ Confirm feature map with user
  │
  Phase 1: Scoping + Deep Search (competitor/prior art survey)
  │  ├─ Research design scoped to repository's domain
  │  └─ Gemini/Perplexity MCP auto-execution
  │
  Phase 2-3: Research + Deep Dive (competitor analysis)
  │
  Phase 4: Gap Analysis (identify differentials)
  │  ├─ Feature comparison matrix
  │  ├─ Positioning analysis (strengths/weaknesses/opportunities/threats)
  │  └─ Essence: why chosen / why not chosen
  │
  Phase 5: Roadmap Proposal
       ├─ Short/mid/long-term prioritized actions
       ├─ 2 roadmap variants
       └─ counter-argument verification
```

★ = User interaction point

### Phase 0: Repo Analysis

1. Launch `repo-analyzer` agent with the GitHub URL
2. Build feature map from analysis results
3. **Ask user** to confirm/correct the feature map before proceeding

### Phase 4 (Repo Mode): Gap Analysis

Instead of general synthesis, perform:
- Feature comparison matrix (this repo vs competitors)
- Positioning analysis with axes relevant to the domain
- Identify: "Why would someone choose this over alternatives?"
- Identify: "Why would someone NOT choose this?"

### Phase 5 (Repo Mode): Roadmap Proposal

1. Generate 2 roadmap variants:
   - **Variant A**: Focus on deepening existing strengths
   - **Variant B**: Focus on addressing critical gaps
2. Each with short-term (1-3 months), mid-term (3-6 months), long-term (6-12 months)
3. Launch `counter-argument` agent
4. Present both variants with counter-argument results

---

## Deep Search Usage

MCP tools for auto-execution (no manual user action needed):

| MCP | Model | Cost | Usage |
|-----|-------|------|-------|
| `mcp__gemini-deepsearch__deep_search` | Gemini Deep Research | Free (250/day) | Primary research |
| `mcp__perplexity__perplexity_research` | sonar-deep-research | ~$0.4-1.3/call | Supplemental, top axes only |

**Cost management**: Run Gemini first for all axes → Perplexity for top 1-3 axes only.

---

## Sub-Agents

| Agent | Role | File |
|-------|------|------|
| `repo-analyzer` | Collect GitHub repo features, Issues/PRs, external reviews | `agents/repo-analyzer.md` |
| `deep-researcher` | Web/SNS search to supplement Deep Search results. Collector only | `agents/deep-researcher.md` |
| `case-analyzer` | Detailed analysis of individual cases (outcomes, success factors, reception) | `agents/case-analyzer.md` |
| `social-scanner` | X/Reddit/HackerNews/community sentiment research | `agents/social-scanner.md` |
| `source-verifier` | Validate all URLs exist + check claim consistency | `agents/source-verifier.md` |
| `counter-argument` | Challenge proposals: counter-arguments, logical leaps, risks | `agents/counter-argument.md` |

---

## Output

Save to `workspace/{topic}/`:
- `repo-analysis.md` — Repository analysis & feature map (repo mode only)
- `research.md` — Collected information with sources
- `analysis.md` — Deep dive analysis
- `proposal.md` — Final proposals / roadmap (the deliverable)

---

## Quality Criteria

- All factual claims have source URLs
- 2+ proposals (comparable alternatives)
- Each proposal has pros / cons / risks
- source-verifier: all URLs validated
- counter-argument: all proposals stress-tested
- Logic chain (fact → inference → conclusion) explainable to a third party
