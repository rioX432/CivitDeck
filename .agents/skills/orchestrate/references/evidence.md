# Evidence

Where each rule in `SKILL.md` comes from. Every item below was read in the fetched page or PDF on 2026-09-20, and
the numbers and quoted phrases were re-verified by a second agent against the raw page text. Re-check before
relying on a number: these pages change.

## Anthropic

| Rule in SKILL.md | Source | What the source says |
| --- | --- | --- |
| Fan-out is expensive; single agent first | [Building effective agents](https://www.anthropic.com/engineering/building-effective-agents) | Recommends finding the simplest solution possible — which may mean not building an agentic system at all. Agentic systems trade latency and cost for task performance. |
| ~4x / ~15x token cost | [Multi-agent research system](https://www.anthropic.com/engineering/built-multi-agent-research-system) (Jun 13, 2025) | Agents use about 4x the tokens of chat; multi-agent systems about 15x. |
| Fan-out can be worth it | same | Opus 4 lead with Sonnet 4 subagents beat single-agent Opus 4 by 90.2% on an internal research eval; token usage alone explains 80% of the variance on BrowseComp. |
| Brief needs objective / output format / tool and source guidance / boundaries | same | Listed as what each subagent needs. Vague briefs produced duplicated work: one subagent researched the 2021 automotive chip crisis while two duplicated each other on 2025 supply chains. |
| 3–5 parallel workers; scale effort to the question | same | The lead spins up 3–5 subagents in parallel rather than serially. Heuristic: simple fact-finding is 1 agent with 3–10 tool calls, direct comparisons 2–4 subagents with 10–15 calls each, complex research more than 10 subagents. |
| Over-spawning is a real failure | same | Agents made errors like spawning 50 subagents for simple queries. |
| Do not fan out coupled work | same | Domains that require all agents to share context, or that involve many dependencies between agents, are a poor fit. The lead cannot steer subagents and subagents cannot coordinate with each other. |
| Persist the plan before spawning | same | The lead agent saves its plan to memory because context beyond 200,000 tokens is truncated. |
| Resume from a checkpoint, not from zero | same | Systems were built to resume from where the agent was when errors occurred; rainbow deployments avoid disrupting running agents. |
| Evaluate with rubrics and a judge, plus human review | same | LLM-as-judge against a rubric (accuracy, citations, completeness, source quality, tool efficiency) alongside human evaluation. |
| Returns are compressed to ~1,000–2,000 tokens | [Effective context engineering](https://www.anthropic.com/engineering/effective-context-engineering-for-ai-agents) (Sep 29, 2025) | A subagent may explore with tens of thousands of tokens but returns a condensed summary, often 1,000–2,000 tokens. Context is a finite resource; compaction, note-taking, and just-in-time retrieval manage it. |
| Keep returns small, name things meaningfully | [Writing tools for agents](https://www.anthropic.com/engineering/writing-tools-for-agents) (Sep 11, 2025) | Claude Code restricts tool responses to 25,000 tokens by default; concise response modes cut token use substantially. |
| Only the final message returns to the lead; tool allowlists, per-worker model, worktree isolation, turn caps | [Claude Code sub-agents](https://code.claude.com/docs/en/sub-agents), [Agent SDK subagents](https://code.claude.com/docs/en/agent-sdk/subagents) | Documented frontmatter includes `tools`, `disallowedTools`, `model`, `permissionMode`, `skills`, `memory`, `maxTurns`, `isolation: worktree`. Intermediate tool calls stay inside the subagent. Concurrency defaults to 20 subagents (`CLAUDE_CODE_MAX_CONCURRENT_SUBAGENTS`) with a spawn depth of 3. |
| A subagent does not gain permissions by being spawned | [Agent SDK permissions](https://code.claude.com/docs/en/agent-sdk/permissions) | A subagent runs in the parent session's permission mode; it runs in `bypassPermissions` only when the parent session itself does. |
| Delegate to keep the lead's context clean; give the agent a check it can run | [Claude Code best practices](https://code.claude.com/docs/en/best-practices) | Context is the fundamental constraint, so use subagents to keep research out of it. A reviewer in a fresh subagent context sees only the diff, not the reasoning that produced it. |
| `context: fork`, `agent`, `background`, `allowed-tools`, `user-invocable`, `disable-model-invocation`, `argument-hint` | [Claude Code skills](https://code.claude.com/docs/en/skills) | All documented frontmatter fields, not convention. |
| SKILL.md under 500 lines, description ≤ 1,024 chars, references one level deep, degrees of freedom | [Skill authoring best practices](https://platform.claude.com/docs/en/agents-and-tools/agent-skills/best-practices) | Stated limits. Also: write the description in the third person, saying what the skill does and when to use it. |
| `evals/evals.json` with `expectations` | [anthropics/skills](https://github.com/anthropics/skills) — `skills/skill-creator/references/schemas.md` | Schema: `skill_name`, `evals[]` with `id`, `prompt`, `expected_output`, `expectations[]`. The schema doc says `expectations`; skill-creator's own prose says `assertions` — the schema is authoritative. |

## OpenAI

| Rule in SKILL.md | Source | What the source says |
| --- | --- | --- |
| Start with one agent; splitting early costs more than it gives | [Orchestration and handoffs](https://developers.openai.com/api/docs/guides/agents/orchestration) | "Start with one agent whenever you can. Add specialists only when they materially improve capability isolation, policy isolation, prompt clarity, or trace legibility." Splitting too early adds prompts, traces, and approval surfaces without necessarily improving the workflow. |
| Split when the contract changes | same | Give each specialist a narrow job; split only when the next branch truly needs different instructions, tools, or policy. Refine each specialist's instructions, tools, and output contract. |
| Manager vs decentralized | [A practical guide to building agents](https://cdn.openai.com/business-guides-and-resources/a-practical-guide-to-building-agents.pdf) | Manager pattern: a central agent calls specialists as tools and owns the final response. Decentralized pattern: agents hand off ownership of the conversation. |
| Tool count is not the criterion | same, "Tool overload" | Some implementations manage more than 15 well-defined, distinct tools while others struggle with fewer than 10 overlapping ones; the issue is similarity and overlap, not the count. Do not quote "15 tools" as a rule. |
| Worker instructions: define clear actions, capture edge cases | same, "Configuring instructions" | Use existing documents, prompt agents to break down tasks, define clear actions, capture edge cases. |
| Always set a turn cap | [Running agents (Agents SDK)](https://openai.github.io/openai-agents-python/running_agents/) | `max_turns` bounds the loop and raises `MaxTurnsExceeded` when exceeded. |
| Guardrail placement | [Guardrails (Agents SDK)](https://openai.github.io/openai-agents-python/guardrails/) | Input guardrails run on the first agent only, output guardrails on the last; a tripwire raises and stops the run. |
| Escalate irreversible and repeatedly failing work to a human | practical guide; [Safety in building agents](https://developers.openai.com/api/docs/guides/agent-builder-safety) | Human-in-the-loop triggers: exceeding a failure threshold, and high-risk or irreversible actions. Tool risk is rated low/medium/high. The safety guide advises keeping tool approvals on so users confirm operations, including reads and writes. |
| Pass/fail or pairwise, and control for bias | [Evaluation best practices](https://developers.openai.com/api/docs/guides/evaluation-best-practices) | Adopt eval-driven development; prefer pairwise comparison or pass/fail for reliability; control for response length, since judges favor longer answers; position and verbosity bias are named challenges. |
| Traces make a multi-agent run legible | [Tracing (Agents SDK)](https://openai.github.io/openai-agents-python/tracing/) | Traces carry `workflow_name`, `trace_id`, and `group_id`; agent runs, handoffs, and guardrails each get spans. Unavailable for zero-data-retention organizations. |

## Not verified

- The AgentKit announcement page returned HTTP 403 and was never read; nothing in this skill relies on it.
- Anthropic's `/docs/en/agent-teams` page was not fetched. The cost caveat used here ("running several sessions or
  subagents at once multiplies token usage") and the note that agent teams do not isolate teammates in worktrees
  come from the [agents index](https://code.claude.com/docs/en/agents).
