# update-docs — Document Templates

Output shapes for Phases 4 through 7. Adapt section content to the project; keep the structure.

## Contents
- Phase 4: ARCHITECTURE.md
- Phase 5: CHANGELOG.md
- Phase 6: README.md
- Phase 7: Missing OSS docs (SECURITY.md, CONTRIBUTING.md, LICENSE)

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
