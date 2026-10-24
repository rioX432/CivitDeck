---
name: update-docs
description: "Audit docs for active-vs-stale authority, reconcile outdated instructions, and update architecture, changelog, README, or OSS docs"
---

> **Host adaptation:** Treat product-specific tool names as capabilities. Use the
> equivalent tools available in the current host, ask through its interaction mechanism,
> and skip optional integrations that are unavailable. Resolve repository guidance from
> `AGENTS.md`, falling back to `CLAUDE.md` only when needed. `$ARGUMENTS` means the current
> user request; `${CLAUDE_SKILL_DIR}` means the directory containing this skill.

# /update-docs — Documentation Audit & Update

Sync project documentation with implementation state. Reads project structure, git history, and existing docs to find and fix gaps.

**Arguments:** "$ARGUMENTS"

---

## Phase 0: Parse Arguments

Parse `$ARGUMENTS` (case-insensitive):
- `architecture` → update ARCHITECTURE.md only
- `changelog` → generate/update CHANGELOG.md only
- `readme` → update README cross-references only
- `oss` → create missing OSS docs (SECURITY.md, CONTRIBUTING.md) + add README links
- `hygiene [path]` → source-of-truth and stale-document audit, scoped to one project/app when supplied; run Procedure H below instead of the other phases
- `all` or empty → run all phases

If `$ARGUMENTS` is empty or unrecognized, use `AskUserQuestion`:

**Q: Which documents should be updated?**
- All documents (architecture + changelog + readme + oss) *(Recommended)*
- ARCHITECTURE.md — fix outdated module/directory structure
- CHANGELOG.md — generate from git history
- README.md — update cross-references and features
- OSS docs — create SECURITY.md, add README links

---

## Procedure H: Source-of-truth and stale-document hygiene

This branch is used only for `/update-docs hygiene [path]`. Do not run the architecture/changelog/OSS phases implicitly.
The job is to stop an AI agent from mistaking historical instructions for the current product contract.

1. **Define the boundary.** Read `git status`, root `AGENTS.md` / `CLAUDE.md`, then nearest project/app-specific
   guidance and any declared source-of-truth index. Respect higher-priority user instructions and repository rules.
   Scope the audit to the requested app or feature; do not sweep all unrelated apps by default.
2. **Inventory evidence.** Enumerate relevant README, docs, PRDs, design prompts, mocks, ADRs, code/tests and linked
   GitHub Issues/PRs when available. Read only the subset touching the current contract; include references leading
   agents to old files. Dates and closed state are clues, not proof that content is wrong.
3. **Classify each document** as `current-contract`, `current-reference`, `proposal`, `historical`,
   `superseded`, or `uncertain`. Contrast claims with maintained product guidance and actual code/tests.
   Distinguish implemented behavior from future intent; never infer product decisions from a closed Issue alone.
4. **Make a conflict table**: `claim | source (path/line) | conflicting evidence | status | safe resolution`.
   Prioritize contradictions that would change functionality, permissions, privacy, pricing, or the user flow.
5. **Present the conflict table and edit plan first.** If the user requested an audit only, ask for approval before
   writing. If the user's current request explicitly authorizes a bounded cleanup, do not ask again.\n   **Apply only safe, bounded documentation changes once authorized**: repair incorrect facts; add explicit
   `SUPERSEDED — DO NOT USE FOR IMPLEMENTATION` headers linking to the replacement; update entrypoint pointers;
   remove obsolete links to unapproved specs. Preserve the former text through Git history. For unresolved product
   questions, label `needs decision` and leave executable instructions unchanged. Never automatically delete
   directories, rewrite application behavior, or close/reopen Issues in bulk.
6. **Verify discoverability**: check relative links, README/guidance references, stale terms in active instructions,
   and whether the primary agent's normal reading path resolves to the intended canonical document. A PRD or mock
   does not outrank runtime behavior by accident; differences must be explicit. Report code/test checks as `not run`
   for doc-only edits.

Output: a short `Current sources` map, `Conflicts resolved`, `Historical references retained`,
`Unresolved decisions`, and `Verification` with precise paths. When GitHub Issues need triage, recommend
`keep / clarify / supersede / close as not planned` with a rationale and links; do not erase their history.
If there is no reliable canonical document, propose the smallest new current-contract index and clearly mark it
as requiring a product decision rather than inventing one.

---

## Phase 1: Create Task Tracker

Create tasks based on selected scope:

| Subject | When |
|---------|------|
| "Gather: project structure + docs + git history (parallel scan)" | always |
| "Update ARCHITECTURE.md" | target = architecture or all |
| "Generate/update CHANGELOG.md" | target = changelog or all |
| "Update README.md" | target = readme or all |
| "Create missing OSS docs" | target = oss or all |
| "Verify internal links" | target = all (or whenever files are modified) |

---

## Phase 1b: Parallel Information Gathering

Mark gathering task `in_progress`.

Read [scanners.md](scanners.md) and run only the independent scanners required by the selected scope. Parallelize
them when the host supports isolated read-only workers; otherwise run them sequentially. Each returns a structured
list and none writes files. Choose an available cost-effective model for mechanical collection rather than relying
on a hard-coded provider tier.

Scope the scanners to the phases selected in Phase 0: a `changelog`-only run needs C alone, `readme` needs B and D, `architecture` needs A and B, `oss` needs B.

---

## Phase 2: Gap Analysis

Mark gathering task `completed`.

Consolidate results from all subagents and identify gaps:

### Architecture gaps (compare A vs B)
- Directories/modules in the actual project (Subagent A) NOT reflected in ARCHITECTURE.md (Subagent B)
- Outdated paths or descriptions in ARCHITECTURE.md

### CHANGELOG gaps (from C)
- Does CHANGELOG.md exist?
- How many unlogged commits since last tag/entry?

### README gaps (compare B vs D)
- Features found in code (Subagent D) NOT listed in README Features section
- Broken or missing internal links

### OSS gaps (from B)
- Which standard files (SECURITY.md, CONTRIBUTING.md, LICENSE) are missing?

---

## Phase 3: Confirm Scope

Present gap analysis to user with `AskUserQuestion`:

```
Gap Analysis Results:

Architecture: [N outdated/missing entries in ARCHITECTURE.md]
CHANGELOG: [exists / missing — N unlogged commits]
README Features: [N implemented features not listed]
OSS docs: [list missing files]
```

**Q: Proceed with these updates?**
- Yes, update everything found above *(Recommended)*
- Let me review first (show details)
- Skip specific section (specify which)

---

## Phases 4-7: Write the Documents

Run only the phases selected in Phase 0, in this order:

| Phase | Target | Runs when |
|---|---|---|
| 4 | `ARCHITECTURE.md` | architecture gaps found |
| 5 | `CHANGELOG.md` | unlogged commits exist |
| 6 | `README.md` | feature or link gaps found |
| 7 | `SECURITY.md` / `CONTRIBUTING.md` / `LICENSE` | the file is missing |

The output structure for each is in [templates.md](templates.md). Read it before writing.

Rules that apply to all four:

- **Never invent content.** Every statement comes from a scanner result. A feature no scanner found does not go in the README.
- **Preserve hand-written prose.** Update the facts around it; do not rewrite a paragraph a human wrote to say the same thing differently.
- **One file at a time**, so a failure mid-run leaves the rest untouched.

## Phase 8: Verify Internal Links

*(Always run when any file was modified)*

For each modified doc file, extract all relative markdown links `[text](path)` and verify the target file exists using `ls`.

Report any broken links. If the target was supposed to be created in this run but wasn't, note as error.

---

## Phase 9: Summary Report

Mark all remaining tasks `completed`.

```
## /update-docs Complete

Target: {all | architecture | changelog | readme | oss}

### Files Modified
| File | Action |
|------|--------|
| {path} | {Created / Updated — brief description} |

### Gap Analysis Results
| Area | Before | After |
|------|--------|-------|
| Architecture outdated entries | {N} | 0 |
| Unlogged commits | {N} | 0 |
| README missing features | {N} | 0 |
| Missing OSS files | {N} | 0 |

### Internal Links
All {N} verified links: ✓ (or list broken ones)

### Skipped
{Any phases skipped and reason}
```

---

## Error Handling

| Situation | Action |
|-----------|--------|
| Subagent returns no data | Fallback: read project root directly |
| ARCHITECTURE.md doesn't exist | Create new one from scratch |
| git log returns nothing | Note "no commits to log", skip CHANGELOG |
| Feature exists but can't determine if shipped | Mark as "likely implemented", let user confirm |
| CHANGELOG.md already up to date | Note "already current", skip |
| SECURITY.md already exists | Skip creation, note in report |
| Edit tool fails (pattern not found) | Read file again, retry with correct pattern |
| No README.md exists | Skip README update phase, note in report |
| Localized README out of sync | Only update links section, flag content sync as manual task |
