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

Mark gathering task `in_progress`. Launch **4 Explore subagents in parallel** (model: haiku).

---

### Subagent A: Project Structure Scanner

```
Scan the project directory structure to understand the codebase layout.

Root: {project root — derive from git rev-parse --show-toplevel}

## What to find:

### 1. Top-level directory inventory
List all top-level directories and their purpose (infer from names and contents).

### 2. Source code structure
Find the primary source directories and their organization:
- Look for src/, lib/, app/, packages/, modules/, feature/, core/ patterns
- For each major directory, list subdirectories (1-2 levels deep)
- Note the primary language(s) (check file extensions)

### 3. Build system
Identify the build system and its configuration:
- Package managers: package.json, Cargo.toml, go.mod, build.gradle.kts, pyproject.toml, etc.
- Monorepo tools: nx.json, turbo.json, lerna.json, settings.gradle.kts, Cargo workspace
- If monorepo: list all packages/modules with their names

### 4. Configuration files
List notable config files: CI workflows, Docker, linting, formatting, etc.

Return a structured summary of the project layout.
```

---

### Subagent B: Existing Docs Scanner

```
Scan all documentation files in the project.

Root: {project root}

## What to find:

### 1. Document inventory
Use Glob to find all *.md files in: root, docs/, .github/.
For each file, record: path, first heading (title), approximate line count.

### 2. Document content summary
For each of these files (if they exist), read and summarize key sections:
- README.md — Features/description section, links section
- docs/ARCHITECTURE.md or ARCHITECTURE.md — structure section
- CONTRIBUTING.md or docs/CONTRIBUTING.md — first 10 lines
- CHANGELOG.md — latest version entry

### 3. Internal link inventory
For README.md, extract ALL markdown links (pattern: `[text](path)`).
For each link: text, target path, whether it's a relative link.

### 4. Missing standard files
Check existence of: CHANGELOG.md, SECURITY.md, CONTRIBUTING.md, LICENSE

Return a structured summary including: allDocs, missingFiles, readmeLinks, existing doc summaries.
```

---

### Subagent C: Git History Scanner

```
Scan git history for CHANGELOG generation.

Root: {project root}

## What to find:

### 1. Latest git tags
Run: git tag --sort=-version:refname | head -10
If no tags, note "no tags — will use full history".

### 2. Commits since last tag (or recent 100 if no tags)
If tag exists:
  git log {latest_tag}..HEAD --pretty=format:"%H|%s|%as|%an" --no-merges
If no tag:
  git log --pretty=format:"%H|%s|%as|%an" --no-merges | head -100

### 3. Classify each commit
For each commit subject, classify into:
- Added: new feature, "add", "implement", "support", "introduce"
- Changed: update, refactor, improve, bump, migrate, rename
- Fixed: fix, bug, crash, error, broken, revert
- Removed: remove, delete, drop, clean
- Infrastructure: CI, lint, build, workflow, docker, deps

### 4. Tagged releases
For each tag found, get the tag date:
  git log -1 --format="%as" {tag}

Return: latestTag, classified commits, tag list with dates.
```

---

### Subagent D: Feature Inventory Scanner

```
Inventory the project's implemented features by examining the source code.

Root: {project root}

## What to find:

### 1. Entry points and main modules
Find main application entry points (main.py, index.ts, App.kt, main.go, etc.).
List the primary features/screens/routes/commands exposed.

### 2. API surface (if applicable)
Grep for route/endpoint definitions, CLI commands, or exported modules.

### 3. Notable integrations
Grep for: database, auth, API client, websocket, queue, cache, storage patterns.
List external service integrations found.

### 4. README-claimed features vs actual
Read README.md's features section. For each claimed feature, verify it exists in code.
Note any features found in code but NOT mentioned in README.

Return: list of implemented features, integrations, and any README gaps.
```

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

## Phase 4: Update ARCHITECTURE.md

*(Skip if target = changelog, readme, or oss)*

Mark task `in_progress`.

### 4a. Build updated structure

Using Subagent A results, construct/update the project structure tree. Match the style already used in ARCHITECTURE.md (if it exists). If no ARCHITECTURE.md exists, create one with this structure:

```markdown
# Architecture

## Project Structure

```
project-root/
├── {dir}/    # {description}
├── {dir}/    # {description}
└── ...
```

## Key Design Decisions

{Extract from existing docs or leave as TODO for user}
```

### 4b. Update existing sections

If ARCHITECTURE.md already exists:
- Read the file first
- Use `Edit` to update only the outdated sections (structure tree, module descriptions)
- Preserve sections that are still accurate (design decisions, diagrams, etc.)
- Do NOT rewrite sections that haven't changed

Mark task `completed`.

---

## Phase 5: Generate/Update CHANGELOG.md

*(Skip if target = architecture, readme, or oss)*

Mark task `in_progress`.

### 5a. Check existing CHANGELOG

If exists: read to find the latest version/entry recorded.
If not: create from scratch.

### 5b. Write CHANGELOG.md

Format: [Keep a Changelog 1.1.0](https://keepachangelog.com/en/1.1.0/)

```markdown
# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- {commit subject} ({short hash})

### Changed
- ...

### Fixed
- ...

### Infrastructure
- ...
```

Rules:
- Group commits by category (Added, Changed, Fixed, Removed, Infrastructure)
- Use commit subject as-is, appended with `({short_hash})`
- Skip merge commits
- Infrastructure category goes last
- If tags exist, create a section for each tagged version with its date
- Filter out commits already in the existing CHANGELOG

Mark task `completed`.

---

## Phase 6: Update README.md

*(Skip if target = architecture, changelog, or oss)*

Mark task `in_progress`.

### 6a. Determine missing features

From Subagent D, identify features implemented in code but not listed in README.

### 6b. Update Features section

Add missing feature descriptions to the Features section. Keep existing content unchanged.

### 6c. Add doc links

If CHANGELOG.md exists (or was just created) and not already linked in README:
```markdown
## Changelog
See [CHANGELOG.md](CHANGELOG.md) for a full list of changes.
```

If SECURITY.md exists (or was just created) and not already linked:
```markdown
For security issues, see [SECURITY.md](SECURITY.md).
```

### 6d. Mirror in localized README (if exists)

Check for `README.ja.md`, `README.ko.md`, etc. If found, apply equivalent changes (translated headings, same link paths).

Use `Edit` for targeted replacements. Read files before editing.

Mark task `completed`.

---

## Phase 7: Create Missing OSS Docs

*(Skip if target = architecture, changelog, or readme)*

Mark task `in_progress`.

### SECURITY.md (if missing)

Adapt to the project's context (web app, CLI tool, library, mobile app):

```markdown
# Security Policy

## Supported Versions

| Version | Supported |
|---------|-----------|
| Latest  | ✅        |
| Older   | ❌        |

## Reporting a Vulnerability

Please **do not** open a public GitHub Issue for security vulnerabilities.

Report via [GitHub Security Advisories]({repo_url}/security/advisories/new):

1. Go to the Security tab → Advisories → "Report a vulnerability"
2. Describe the vulnerability, steps to reproduce, and potential impact
3. We aim to respond within 7 days
```

Derive `{repo_url}` from `git remote get-url origin`.

### CONTRIBUTING.md (if missing and project has >5 contributors or is public)

Only suggest creation — do NOT auto-create without user confirmation, as contribution guidelines are opinionated.

Mark task `completed`.

---

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
