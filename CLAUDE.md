# CLAUDE.md

Strictly follow the rules in [AGENTS.md](./AGENTS.md).

## Think Twice

Before acting, always pause and reconsider. Re-read the requirements, re-check your assumptions, and verify your approach is correct before writing any code.

## Research-First Development

Never design or implement based on guesses or assumptions. Always follow this order:

1. **Identify the source of truth** — Read official documentation, inspect actual source code (JARs, library internals), or search the web to confirm API signatures, behavior, and best practices.
2. **Verify with Codex** — Before starting design or implementation, use the Codex MCP tool to confirm that your approach, API usage, and architectural decisions are correct.
3. **Proceed only with confirmed information** — If the source of truth is unclear or Codex raises concerns, investigate further or ask the user before writing code.

This applies to:
- Choosing library APIs and their correct usage
- Diagnosing bugs and identifying root causes
- Designing architecture and component structure
- Any decision where you are not 100% certain
