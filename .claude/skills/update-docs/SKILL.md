---
name: update-docs
description: "Audit and update project docs — ARCHITECTURE.md, CHANGELOG.md, README cross-references, and missing OSS docs"
argument-hint: "[target: all | architecture | changelog | readme | oss]"
user-invocable: true
disable-model-invocation: true
allowed-tools:
  - Bash(git log:*)
  - Bash(git tag:*)
  - Bash(git show:*)
  - Bash(git diff:*)
  - Bash(ls *)
  - Glob
  - Grep
  - Read
  - Edit
  - Write
  - Agent
  - TaskCreate
  - TaskUpdate
  - TaskList
  - TaskGet
  - AskUserQuestion
---

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
- `all` or empty → run all phases

If `$ARGUMENTS` is empty or unrecognized, use `AskUserQuestion`:

**Q: Which documents should be updated?**
- All documents (architecture + changelog + readme + oss) *(Recommended)*
- ARCHITECTURE.md — fix outdated module/directory structure
- CHANGELOG.md — generate from git history
- README.md — update cross-references and features
- OSS docs — create SECURITY.md, add README links

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

Read [scanners.md](scanners.md) and launch its **Subagents A, B, C and D in parallel** (Explore, model: haiku) — project structure, existing docs, git history, feature inventory. Each returns a structured list; none of them writes files.

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
