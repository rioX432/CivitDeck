# Behavior Rules

## No Guessing
- **Do not make assumptions.** Verify API specs, library behavior, and OS constraints before implementation
- **Fact-checking priority**: WebSearch + official docs first. Codex MCP is for **design verification**, not fact lookup
- When uncertain about API behavior or library specs, use WebSearch and cite the source URL
- Code based on guesses will always have bugs

## Verify Before Implementing

Use **Codex MCP** to validate design decisions before implementation.

### How to use Codex MCP

1. Load the tool: `ToolSearch("select:mcp__codex__codex")`
2. Call `mcp__codex__codex` with a single, focused design question
3. Keep each request to **one topic** — cost: input $1.50/1M, output $6.00/1M tokens

### When to use Codex

- **Mandatory**: New architecture patterns, design decisions not in docs, platform-specific API usage, library selection
- **Optional**: Complex trade-off analysis, migration planning
- **Skip**: Naming conventions, test strategy, anything decidable from existing codebase patterns

### Fallback (Codex unavailable)

If Codex MCP is not configured or fails to respond:
1. WebSearch for official documentation and established patterns
2. Check existing codebase for precedent
3. Document the decision rationale in the PR description

## Think Twice
- After writing code, **re-read and verify it's correct**
- Check:
  - Requirements are met
  - CLAUDE.md rules are followed
  - Edge cases covered (empty data, null, offline)
  - Consistent with existing patterns
