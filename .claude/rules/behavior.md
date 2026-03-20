# Behavior Rules

## No Guessing
- **Do not make assumptions.** Verify API specs, library behavior, and OS constraints before implementation
- When uncertain, use web search, official docs, or Codex MCP (`mcp__codex__codex`) to confirm
- Code based on guesses will always have bugs

## Verify Before Implementing
- Check implementation details and design decisions with **Codex MCP** before starting
- Mandatory for:
  - New architecture patterns
  - Design decisions not specified in docs
  - Platform-specific API usage
  - Library selection and version decisions

## Think Twice
- After writing code, **re-read and verify it's correct**
- Check:
  - Requirements are met
  - CLAUDE.md rules are followed
  - Edge cases covered (empty data, null, offline)
  - Consistent with existing patterns
