---
name: update-docs
description: "Audit and update project docs — ARCHITECTURE.md from module structure, CHANGELOG.md from git history, README cross-references, and missing OSS docs"
argument-hint: "[target: all | architecture | changelog | readme | oss]"
user-invocable: true
disable-model-invocation: true
allowed-tools:
  - Bash(git log:*)
  - Bash(git tag:*)
  - Bash(git show:*)
  - Bash(ls *)
  - Glob
  - Grep
  - Read
  - Edit
  - Write
  - Task
  - TaskCreate
  - TaskUpdate
  - TaskList
  - TaskGet
  - AskUserQuestion
  - mcp__codex__codex
  - mcp__codex__codex-reply
---

# /update-docs — Documentation Audit & Update

Sync CivitDeck documentation with implementation state. Reads module structure, git history, and existing docs to fix gaps.

**Arguments:** "$ARGUMENTS"

---

## Phase 0: Parse Arguments

Parse `$ARGUMENTS` (case-insensitive):
- `architecture` → update ARCHITECTURE.md + AGENTS.md only
- `changelog` → generate/update CHANGELOG.md only
- `readme` → update README.md + README.ja.md cross-references only
- `oss` → create missing OSS docs (SECURITY.md) + add README links
- `all` or empty → run all phases

If `$ARGUMENTS` is empty or unrecognized, use `AskUserQuestion`:

**Q: Which documents should be updated?**
- All documents (architecture + changelog + readme + oss) *(Recommended)*
- ARCHITECTURE.md + AGENTS.md — fix outdated module structure
- CHANGELOG.md — generate from git history
- README.md + README.ja.md — update cross-references and Features
- OSS docs — create SECURITY.md, add README links

---

## Phase 1: Create Task Tracker

Create tasks based on selected scope. Use `TaskCreate` for each:

| Subject | When |
|---------|------|
| "Gather: modules + docs + git + features (parallel scan)" | always |
| "Update ARCHITECTURE.md + AGENTS.md" | target = architecture or all |
| "Generate/update CHANGELOG.md" | target = changelog or all |
| "Update README.md + README.ja.md" | target = readme or all |
| "Create SECURITY.md" | target = oss or all |
| "Verify internal links" | target = all (or whenever files are modified) |

---

## Phase 1b: Parallel Information Gathering

Mark task 1 `in_progress`. Launch **4 Task subagents in parallel** (`subagent_type: "Explore"`, `model: "haiku"`).

---

### Subagent A: Module Structure Scanner

```
Scan the CivitDeck KMP project module structure.

Root: /Users/rio/workspace/projects/CivitDeck/

## What to find:

### 1. All Gradle modules
Read `settings.gradle.kts` and extract every `include(...)` line.
For each module path (e.g. ":core:core-domain"), derive:
- gradle path (e.g. ":core:core-domain")
- directory path (e.g. "core/core-domain/")
- one-line description (infer from directory name and contents)

### 2. Module contents (spot-check)
For each module, check if its `src/commonMain/kotlin/.../` (or `src/main/kotlin/...`) directory exists and list top-level subdirectories (e.g. domain/model, data/api, ui/components).

### 3. Convention plugins
Check `build-logic/` for convention plugin files (*.gradle.kts). List their names.

Return format (JSON-like):
{
  "modules": [
    {
      "gradlePath": ":core:core-domain",
      "dirPath": "core/core-domain",
      "description": "Domain layer: models, repository interfaces, use cases",
      "topLevelDirs": ["domain/model", "domain/repository", "domain/usecase"]
    }
  ],
  "conventionPlugins": ["civitdeck.kmp.library", "civitdeck.android.application"]
}
```

---

### Subagent B: Existing Docs Scanner

```
Scan all documentation files in the CivitDeck project.

Root: /Users/rio/workspace/projects/CivitDeck/

## What to find:

### 1. Document inventory
Use Glob to find all *.md files in: root, docs/, .github/.
For each file, record:
- path
- first heading (title)
- approximate line count
- last-modified (not available — skip)

### 2. Document content summary
For each of these specific files, read and summarize key sections:
- README.md — Features section, links section
- README.ja.md — Features section (機能), links
- docs/ARCHITECTURE.md — Module Structure section
- AGENTS.md — any Module Structure or module list section
- docs/CONTRIBUTING.md — first 10 lines
- docs/ROADMAP.md — first 10 lines

### 3. Internal link inventory
For README.md and README.ja.md, extract ALL markdown links (pattern: `[text](path)`).
For each link, record: link text, target path, is relative (starts with docs/ or #).

### 4. Missing OSS files
Check if these files exist (use ls or Glob):
- CHANGELOG.md (root)
- SECURITY.md (root)
- docs/CODE_OF_CONDUCT.md

Return format (JSON-like):
{
  "allDocs": ["README.md", "README.ja.md", "AGENTS.md", "docs/ARCHITECTURE.md", ...],
  "missingOssDocs": ["CHANGELOG.md", "SECURITY.md"],
  "readmeLinks": [
    {"text": "CONTRIBUTING.md", "path": "docs/CONTRIBUTING.md", "exists": true},
    {"text": "ROADMAP.md", "path": "docs/ROADMAP.md", "exists": true}
  ],
  "architectureMdModuleTree": "copy of the Module Structure section content",
  "agentsMdModuleSection": "copy of any module list in AGENTS.md"
}
```

---

### Subagent C: Git History Scanner

```
Scan git history for CHANGELOG generation in CivitDeck.

Root: /Users/rio/workspace/projects/CivitDeck/

## What to find:

### 1. Latest git tag
Run: git tag --sort=-version:refname | head -5
If no tags exist, note "no tags — will use full history from initial commit".

### 2. Commits since last tag (or all if no tags)
If tag exists:
  git log {latest_tag}..HEAD --pretty=format:"%H|%s|%as|%an" --no-merges
If no tag:
  git log --pretty=format:"%H|%s|%as|%an" --no-merges | head -100

### 3. Classify each commit
For each commit subject line, classify into one category:
- Added: new feature, new screen, new module, "add", "implement", "support"
- Changed: update, refactor, improve, bump, migrate, rename
- Fixed: fix, bug, crash, error, broken, wrong, revert
- Removed: remove, delete, drop, clean
- Infrastructure: CI, lint, Detekt, SwiftLint, build, gradle, workflow, githubactions

### 4. PR merge commits (for context only)
Run: git log --pretty=format:"%H|%s|%as" --merges | head -20
Extract PR numbers from merge commit subjects if present.

Return format (JSON-like):
{
  "latestTag": "v1.0.0",  // or null
  "commits": [
    {
      "hash": "abc1234",
      "subject": "Add ComfyUI queue management (#194)",
      "date": "2025-01-15",
      "category": "Added"
    }
  ],
  "prMerges": [
    {"hash": "def5678", "subject": "Add workflow template library (#177)", "date": "2025-01-10"}
  ]
}
```

---

### Subagent D: Feature Inventory Scanner

```
Inventory implemented features in CivitDeck.

Root: /Users/rio/workspace/projects/CivitDeck/

## What to find:

### 1. Android feature modules (from feature/ directory)
For each subdirectory in feature/:
- Module name (e.g. feature-comfyui)
- Does it have a Screen composable? (Grep for "fun.*Screen(" in the module)
- Does it have a ViewModel? (Grep for "ViewModel()" in the module)
- One-line inferred description

### 2. iOS feature ViewModels
List Swift files matching: iosApp/iosApp/Features/**/*ViewModel.swift
For each: ViewModel name, parent feature directory

### 3. Database version
Read: core/core-database/src/commonMain/kotlin/.../CivitDeckDatabase.kt
Extract the `version = N` value.
Also count migration files in: core/core-database/src/commonMain/kotlin/.../migration/

### 4. Notable implemented features (deep check)
Check for these specific feature signals:
- ComfyUI integration: Grep for "ComfyUI" in feature-comfyui/
- Collections: Grep for "Collection" in feature-collections/
- Prompt templates: Grep for "template" (case-insensitive) in feature-prompts/
- LoRA/ControlNet: Grep for "LoRA\|ControlNet" in feature-comfyui/
- Custom workflow import: Grep for "import.*workflow\|workflow.*import" (case-insensitive)

Return format (JSON-like):
{
  "androidFeatures": [
    {
      "module": "feature-comfyui",
      "hasScreen": true,
      "hasViewModel": true,
      "description": "ComfyUI generation with queue management and workflow templates"
    }
  ],
  "iosViewModels": ["ComfyUIGenerationViewModel", "CollectionsViewModel", ...],
  "dbVersion": 19,
  "migrationCount": 18,
  "notableFeatures": {
    "comfyUI": true,
    "collections": true,
    "promptTemplates": true,
    "loraControlNet": true,
    "customWorkflowImport": true
  }
}
```

---

## Phase 2: Gap Analysis

Mark task 1 `completed`.

Consolidate results from all 4 subagents and identify gaps:

### Architecture gaps (compare A vs B)
- Modules in `settings.gradle.kts` (Subagent A) that are NOT reflected in `docs/ARCHITECTURE.md` Module Structure tree (Subagent B)
- Outdated module paths (e.g. `shared/data/`, `shared/domain/` — the old monolith structure)
- DI section still referring to `shared/di/` instead of per-module DI

### CHANGELOG gaps (from C)
- Does `CHANGELOG.md` exist? (Subagent B: missingOssDocs)
- How many unlogged commits since last tag?

### README gaps (compare B vs D)
- Features in Subagent D's `notableFeatures` that are NOT in README.md Features section
- Missing links to CHANGELOG.md or SECURITY.md

### OSS gaps (from B)
- Which files in `missingOssDocs` are absent?

---

## Phase 3: Confirm Scope

Present gap analysis to user with `AskUserQuestion`:

```
Gap Analysis Results:

Architecture: [N outdated module paths found in ARCHITECTURE.md]
CHANGELOG: [exists / missing — N unlogged commits]
README Features: [N implemented features not listed]
OSS docs: [SECURITY.md missing / CHANGELOG.md missing]
```

**Q: Proceed with these updates?**
- Yes, update everything found above *(Recommended)*
- Let me review first (show full diff preview)
- Skip specific section (specify which)

If user wants to skip a section, ask which one to skip.

---

## Phase 4: Update ARCHITECTURE.md + AGENTS.md

*(Skip if target = changelog, readme, or oss)*

Mark task "Update ARCHITECTURE.md + AGENTS.md" `in_progress`.

### 4a. Build new Module Structure tree

Using Subagent A results, construct the full module tree:

```
CivitDeck/
├── build-logic/              # Convention Plugins (civitdeck.kmp.library, civitdeck.android.application)
├── shared/                   # KMP coordinator — re-exports core modules via api()
├── core/
│   ├── core-domain/          # Domain layer: models, repository interfaces, use cases
│   ├── core-network/         # Data layer: Ktor client, DTOs, DtoMapper, NetworkModule
│   ├── core-database/        # Data layer: Room KMP entities/DAOs/migrations, DatabaseModule
│   └── core-ui/              # UI layer: shared Compose components, design tokens (ui/theme/)
├── feature/
│   ├── feature-search/       # Model search screen
│   ├── feature-detail/       # Model detail screen
│   ├── feature-gallery/      # Image gallery screen
│   ├── feature-creator/      # Creator profile screen
│   ├── feature-collections/  # Collections (saved model lists) screen
│   ├── feature-prompts/      # Prompt metadata + template library screen
│   ├── feature-settings/     # Settings screen
│   └── feature-comfyui/      # ComfyUI generation workflow screen
├── androidApp/               # Android app entry point, Navigation 3, DI wiring, ModelCard
└── iosApp/                   # iOS app entry point, SwiftUI navigation, DesignSystem
```

Fill in descriptions from actual Subagent A scan results.

### 4b. Update Layer Responsibilities section

Change path references in the Data Layer and Domain Layer sections:
- Old: `shared/data/` → New: `core/core-network/` (API) and `core/core-database/` (local)
- Old: `shared/domain/` → New: `core/core-domain/`

Do NOT change: Data Flow diagram, Key Design Decisions, CI/CD section.

### 4c. Update DI section

Replace:
```
- **Shared module** (`shared/di/`): Defines modules for API clients, repositories, and use cases
- **Android** (`androidApp/di/`): Extends shared modules with Android-specific bindings
- **iOS** (`iosApp/`): Initializes Koin in app entry point, resolves use cases
```

With:
```
- **core-network** (`core/core-network/.../di/NetworkModule`): Ktor client, API services
- **core-database** (`core/core-database/.../di/DatabaseModule`): Room DB, DAOs
- **core-domain** (`core/core-domain/.../di/DomainModule`): Repository bindings, use cases
- **shared** (`shared/src/commonMain/di/`): Re-exports core modules; ViewModelModule (SettingsViewModel)
- **Android** (`androidApp/.../CivitDeckApplication.kt`): Platform-specific ViewModels
- **iOS** (`shared/src/iosMain/di/KoinHelper.kt`): iOS use case accessors via `KoinHelper.shared.getXxx()`
```

### 4d. Update AGENTS.md

Find the module structure section in AGENTS.md (if any) and apply the same module tree update.
If AGENTS.md has no module list, skip this step.

Use `Edit` tool to make targeted replacements. Read the files first, then edit only the sections that changed.

Mark task `completed`.

---

## Phase 5: Generate/Update CHANGELOG.md

*(Skip if target = architecture, readme, or oss)*

Mark task "Generate/update CHANGELOG.md" `in_progress`.

### 5a. Check if CHANGELOG.md exists

If it exists: read it to find the latest version recorded.
If it doesn't exist: create from scratch.

### 5b. Classify commits from Subagent C

Group commits by category in Keep a Changelog format:
- **Added** — new features
- **Changed** — changes to existing functionality, refactors, dependency bumps
- **Fixed** — bug fixes
- **Removed** — removed features or files
- **Infrastructure** — CI, build system, lint, non-functional changes

Filter out commits that are already covered by the existing CHANGELOG.

### 5c. Write CHANGELOG.md

Format (Keep a Changelog 1.1.0 — https://keepachangelog.com/en/1.1.0/):

```markdown
# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- {commit subject} ({short hash})
- ...

### Changed
- ...

### Fixed
- ...

### Infrastructure
- {CI/lint/build changes}
- ...

## [1.0.0] - YYYY-MM-DD

...
```

Rules:
- Group commits by category within the `[Unreleased]` section
- Use the commit subject as-is, appended with `({short_hash})`
- Skip merge commits (already filtered by `--no-merges`)
- Infrastructure category goes last in each version block
- If a tag exists, create a section for each tagged version

Use `Write` to create the file at `/Users/rio/workspace/projects/CivitDeck/CHANGELOG.md`.

Mark task `completed`.

---

## Phase 6: Update README.md + README.ja.md

*(Skip if target = architecture, changelog, or oss)*

Mark task "Update README.md + README.ja.md" `in_progress`.

### 6a. Determine missing features

From Subagent D's `androidFeatures` and `notableFeatures`, identify shipped features not in current README Features section.

"Shipped" = both Android Screen AND iOS ViewModel exist for that feature.

Feature name mapping:
| Module | Display name (EN) | Display name (JA) |
|--------|-------------------|-------------------|
| feature-comfyui | **ComfyUI Integration** — queue management, workflow templates, LoRA/ControlNet support | **ComfyUI 連携** — キュー管理、ワークフローテンプレート、LoRA/ControlNet 対応 |
| feature-collections | **Collections** — organize models into named groups | **コレクション** — モデルを名前付きグループで整理 |
| feature-prompts (template) | **Prompt Templates** — built-in and user-created template library | **プロンプトテンプレート** — 組み込み＋ユーザー作成のテンプレートライブラリ |

### 6b. Update README.md Features section

Add missing feature bullet points to the `## Features` section in README.md.
Keep existing bullets unchanged. Append new ones before the closing paragraph.

### 6c. Add CHANGELOG.md link to README.md

In the `## Contributing` section (or after it), add:

```markdown
## Changelog

See [CHANGELOG.md](CHANGELOG.md) for a full list of changes.
```

Only add if CHANGELOG.md was created or already exists AND the link is not already present.

### 6d. Add SECURITY.md link to README.md

If SECURITY.md was created (Phase 7) or already exists, add to the Contributing section:

```markdown
For security issues, see [SECURITY.md](SECURITY.md).
```

Only add if not already present.

### 6e. Mirror changes in README.ja.md

Apply equivalent changes to README.ja.md:
- Add Japanese feature bullets
- Add CHANGELOG.md link (same path, Japanese label: `変更履歴`)
- Add SECURITY.md link (Japanese label: `セキュリティポリシー`)

### 6f. Update Roadmap reference (if ComfyUI is shipped)

README.md currently says: *"See the full Roadmap for planned features including ComfyUI integration..."*
If ComfyUI is now shipped, update this sentence to remove ComfyUI from the "planned" list.

Use `Edit` tool for targeted replacements. Read each file before editing.

Mark task `completed`.

---

## Phase 7: Create SECURITY.md

*(Skip if target = architecture, changelog, or readme)*

Mark task "Create SECURITY.md" `in_progress`.

Only run if SECURITY.md does NOT exist (check from Subagent B).

### Content

CivitDeck is a client-only app (local DB + API key are the main attack surface).

```markdown
# Security Policy

## Supported Versions

| Version | Supported |
|---------|-----------|
| Latest  | ✅        |
| Older   | ❌        |

CivitDeck is in active development. Only the latest release receives security fixes.

## Scope

This policy covers the CivitDeck Android and iOS applications.

**In scope:**
- Local database (Room KMP) — data leakage, unauthorized access
- API key storage — insecure storage of CivitAI API credentials
- Network requests — MITM, insecure TLS configuration
- Input handling — injection vulnerabilities in search/filter inputs

**Out of scope:**
- CivitAI's own servers or API (report those to Civitai Inc. directly)
- Issues in third-party libraries (report upstream)

## Reporting a Vulnerability

Please **do not** open a public GitHub Issue for security vulnerabilities.

Report via [GitHub Security Advisories](https://github.com/rioX432/CivitDeck/security/advisories/new):

1. Go to the Security tab → Advisories → "Report a vulnerability"
2. Describe the vulnerability, steps to reproduce, and potential impact
3. We aim to respond within 7 days

## What to Expect

- Acknowledgement within 7 days
- Regular updates on the fix timeline
- Credit in the CHANGELOG.md when the fix is released (unless you prefer to stay anonymous)
```

Use `Write` tool to create `/Users/rio/workspace/projects/CivitDeck/SECURITY.md`.

Mark task `completed`.

---

## Phase 8: Verify Internal Links

*(Always run when any file was modified)*

Check that all relative links in modified docs resolve to existing files.

### Links to verify

| Source file | Link target | Check |
|------------|-------------|-------|
| README.md | docs/ARCHITECTURE.md | `ls` |
| README.md | docs/CONTRIBUTING.md | `ls` |
| README.md | docs/ROADMAP.md | `ls` |
| README.md | CHANGELOG.md | `ls` (only if added) |
| README.md | SECURITY.md | `ls` (only if added) |
| README.md | LICENSE | `ls` |
| README.ja.md | same as above | `ls` |
| docs/CONTRIBUTING.md | ARCHITECTURE.md (relative) | resolve relative path |
| CHANGELOG.md | (no links to verify) | — |

For each broken link found:
1. Note the broken link in the report
2. If the target file was supposed to be created in this run but wasn't, create it now (or note as error)

---

## Phase 9: Summary Report

Mark all remaining tasks `completed`.

```
## /update-docs Complete

Target: {all | architecture | changelog | readme | oss}

### Files Modified
| File | Action |
|------|--------|
| docs/ARCHITECTURE.md | Updated module structure tree + DI section |
| AGENTS.md | Updated module list |
| CHANGELOG.md | Created ({N} commits logged) / Updated |
| README.md | Added {N} features, added CHANGELOG + SECURITY links |
| README.ja.md | Mirrored README.md changes |
| SECURITY.md | Created |

### Gap Analysis Results
| Area | Before | After |
|------|--------|-------|
| Architecture outdated modules | {N} | 0 |
| Unlogged commits | {N} | 0 |
| README missing features | {N} | 0 |
| Missing OSS files | {N} | 0 |

### Internal Links
All {N} verified links: ✓

### Skipped
{Any phases skipped and reason}
```

---

## Error Handling

| Situation | Action |
|-----------|--------|
| Subagent returns no data | Use fallback: read settings.gradle.kts directly |
| AGENTS.md has no module section | Skip AGENTS.md update, note in report |
| git log returns nothing | Note "no commits to log", skip CHANGELOG |
| Feature module exists but no ViewModel on iOS | Mark as Android-only, don't add to README as shipped |
| CHANGELOG.md already exists and is up to date | Note "already current", skip |
| SECURITY.md already exists | Skip creation, note in report |
| Broken internal link to non-created file | Report as warning, do not fail |
| Edit tool fails (pattern not found) | Read file again, check current content, retry with correct pattern |
