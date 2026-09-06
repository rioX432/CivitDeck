# update-docs — Scanner Agent Prompts

Launch these four subagents in parallel during Phase 1b. Each returns a structured list; none of them writes files.

## Contents
- Subagent A: Project Structure Scanner
- Subagent B: Existing Docs Scanner
- Subagent C: Git History Scanner
- Subagent D: Feature Inventory Scanner

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
