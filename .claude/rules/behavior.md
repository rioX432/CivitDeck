# Behavior Rules

## No Guessing
- **Do not make assumptions.** Verify API specs, library behavior, and OS constraints before implementation
- **Fact-checking priority**: WebSearch + official docs first. Codex is for **design verification**, not fact lookup
- When uncertain about API behavior or library specs, use WebSearch and cite the source URL
- Code based on guesses will always have bugs

## Verify Before Implementing

Use **Codex** as a second model to validate design decisions before implementation.

### How to use Codex

Run it headless through the `codex` CLI, read-only, in the repository root, with a single focused design question per call. Codex CLI 0.154.0 removed `codex mcp-server`, so the `mcp__codex__codex` tool exists only where an older CLI still serves it.

### When to use Codex

- **Mandatory**: New architecture patterns, design decisions not in docs, platform-specific API usage, library selection
- **Optional**: Complex trade-off analysis, migration planning
- **Skip**: Naming conventions, test strategy, anything decidable from existing codebase patterns

### Fallback (Codex unavailable)

If the `codex` CLI is not installed, or the call fails:
1. WebSearch for official documentation and established patterns
2. Check existing codebase for precedent
3. Document the decision rationale in the PR description

### Call pattern (used by /dev, /dig, /decompose)

Skills that consult Codex all follow the same shape. Do not restate it in the skill — reference this section.

Write the prompt to a scratch file with the Write tool, not inline in the shell command — issue text and code spans contain `` ` `` and `$(…)`, which the shell would execute:

```
## {what Codex is being given}
{context}

## {what Codex is asked for}
{numbered request — one topic only}
```

Then run it with a 10-minute Bash timeout (`timeout: 600000`); a repo-reading call routinely outlives the 2-minute default:

```bash
codex exec -s read-only -C "$(git rev-parse --show-toplevel)" -o {answer file} - < {prompt file}
```

Read the answer from `{answer file}`. Where `mcp__codex__codex` is still available, the equivalent call passes the same prompt with `cwd` set to the repository root and `sandbox: "read-only"`.

Rules for every call site:

- **One topic per call.** A call mixing architecture and naming gets a worse answer on both.
- **Feed it facts, not the raw issue.** Pass the investigation findings, the affected-files table, the existing patterns. Codex can read the repository in its read-only sandbox, so name the files to check — it verifies claims against the code instead of trusting the summary.
- **Codex advises, Claude decides.** Its output is an input to the decision matrix or task list, never the final answer, and never applied unreviewed.
- **On failure — `codex` is not on `PATH`, or the call errors — skip the step and continue** with the traditional flow. Log one line saying Codex was unavailable. A Codex outage must never block the workflow.

## Think Twice
- After writing code, **re-read and verify it's correct**
- Check:
  - Requirements are met
  - CLAUDE.md rules are followed
  - Edge cases covered (empty data, null, offline)
  - Consistent with existing patterns
