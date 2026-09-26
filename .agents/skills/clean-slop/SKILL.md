---
name: clean-slop
description: "Remove redundant AI narration, change-history prose, and verbose comments from the current change's diff while preserving useful rationale and documentation. Use for an explicitly requested comment cleanup or as the cleanup pass of /dev or a PR workflow; not for logic refactoring or repository-wide documentation rewriting."
---

> **Host adaptation:** Treat product-specific tool names as capabilities. Use the
> equivalent tools available in the current host, ask through its interaction mechanism,
> and skip optional integrations that are unavailable. Resolve repository guidance from
> `AGENTS.md`, falling back to `CLAUDE.md` only when needed. `$ARGUMENTS` means the current
> user request; `${CLAUDE_SKILL_DIR}` means the directory containing this skill.

# Clean comment slop

Make comments explain the lasting reason or contract. Change comment text only; never change logic, imports, declarations, code structure, or unrelated formatting.

**Scope:** $ARGUMENTS

## Establish the diff

Read the project's `AGENTS.md` and any host-specific project override, inspect `git status`, and record the existing changes before editing. Respect project-required documentation, including operational headers such as seed contents, invocation, and expected results.

Choose the scope from the arguments or the calling workflow:

- **Working tree:** inspect staged and unstaged diffs, plus relevant untracked source files. Limit cleanup to the current task's changes; do not absorb unrelated staged edits.
- **Committed branch:** use the caller's verified PR base and its merge base with `HEAD`. Resolve the actual base before comparing; the feature branch's tracking upstream is not necessarily the PR base. Do not assume `main`. Keep existing uncommitted work distinct; leave overlapping comments alone if ownership cannot be established safely.

If scope is not specified, use the working-tree changes when present, otherwise the committed branch. Ask for the missing base only if it cannot be established from the repository or PR. These are inspection scopes, not permission to create a commit.

Exclude generated, vendored, build-output, golden/snapshot, and non-reviewable files. Inspect only languages whose comment syntax you can identify reliably. Read changed hunks and enough surrounding code, symbols, and callers to understand each comment's purpose.

Only consider comments newly added by the chosen diff or directly attached to a changed statement or declaration. Being in the same file or diff context is not sufficient. Preserve unrelated historical comments.

## Classify conservatively

Remove or trim only text that adds no information beyond the code:

- Process narration, such as "loop over accounts" or "set the flag."
- Change-history prose, such as "previously used X," without a lasting constraint.
- "Per the request," a task ID, or a spec section used as the entire reason for an implementation.
- Repetition of a clear name or signature, or verbose prose whose useful rationale can be stated more briefly without losing meaning.

Preserve useful content even when surrounded by redundant prose:

- Non-obvious rationale, invariants, caveats, intentional deviations, compatibility requirements, and operational instructions.
- External constraints and links to issues, specifications, RFCs, or upstream bugs. Cross-platform parity can be a real contract, not merely historical narration.
- Legal/license notices, generated content and markers, lint or type-checker pragmas, compiler and tool directives, and coverage or formatter controls.
- Tracked TODOs and follow-up descriptions; do not treat a task link as noise merely because it contains an ID.
- Public API documentation that explains contracts, parameters, errors, side effects, examples, or usage beyond the signature; preserve required documentation tags.

Length, comment density, or suspected AI authorship alone is not a reason to delete. Keep ambiguous comments and state the uncertainty if material. Preserve runtime-significant docstrings and documentation consumed by code generation or tools; do not treat string literals as comments.

## Edit and verify

Use narrow edits. For a mixed useful/redundant comment, trim only the redundant portion. Preserve inline code, comment delimiters, token separation, and meaningful newlines. Do not run an auto-fix command that would change imports or code as part of this pass.

Review the cleanup delta separately from pre-existing edits and check whitespace with `git diff --check`. Verify that every removed or changed character belongs to a permitted comment or its otherwise empty line, and that no contract or directive was lost.

Run the smallest relevant check from project guidance → Commands (compile, lint, or documentation check for the affected files); run affected tests when project rules or the changed comment syntax warrant them. Report unavailable checks accurately. Correct cleanup-induced failures; do not expand into unrelated fixes.

Leave changes uncommitted unless the enclosing workflow includes committing. Stage only intended cleanup changes when that workflow authorizes it, preserving unrelated staged content. Scope selection alone never authorizes a commit or push.

Report files cleaned, any material borderline content deliberately retained, validation results, and remaining limitations. If nothing qualifies, report no cleanup needed and make no changes. Repeating the pass should produce no further edits.
