---
name: tech-debt
description: "Scan codebase for technical debt and create GitHub Issues for high-severity findings"
user-invocable: true
disable-model-invocation: true
allowed-tools:
  - Glob
  - Grep
  - Read
  - Agent
  - Bash(gh issue create:*)
  - Bash(gh issue list:*)
  - Bash(git log:*)
  - TaskCreate
  - TaskUpdate
  - TaskList
  - AskUserQuestion
---

# /tech-debt — Technical Debt Scanner

Scan the codebase for technical debt and refactoring opportunities, then create GitHub Issues for actionable items.

## Steps

1. Scan the codebase for the patterns listed below
2. For each finding, assess severity (High / Medium / Low) and effort (S / M / L)
3. Group findings by category
4. For High severity items, create GitHub Issues using `gh issue create`
5. Output a summary report

## Detection Patterns

### Code Smells
- Duplicated code (3+ similar blocks)
- Long functions (50+ lines)
- Large files (500+ lines)
- Deep nesting (4+ levels)
- God classes (too many responsibilities)
- Dead code (unused functions, imports, variables)
- TODO/FIXME/HACK comments left in code

### Architecture
- Circular dependencies between modules
- Layer violations (check CLAUDE.md for architecture boundaries)
- Missing abstractions (concrete classes where interfaces should be)
- Inconsistent patterns across similar features

### Performance
- N+1 queries
- Missing database indexes on frequently queried columns
- Unbounded lists without pagination
- Heavy computation on main thread
- Memory leaks (retain cycles, leaked references)

### Testing
- Public functions without unit tests
- Test files with no assertions
- Missing edge case coverage (null, empty, boundary values)

### Dependencies
- Outdated dependencies (check against latest stable versions)
- Deprecated API usage
- Security vulnerabilities in dependencies (if audit tools available)

## Issue Creation

For each High severity finding:
```bash
gh issue create \
  --title "Tech Debt: {concise description}" \
  --body "{detailed description with file locations and suggested fix}" \
  --label "tech-debt"
```

## Output Format

```
## Tech Debt Report

**Scanned:** N files
**Findings:** X High, Y Medium, Z Low
**Issues created:** N

### High Severity
| # | Category | File | Description | Effort | Issue |
|---|----------|------|-------------|--------|-------|

### Medium Severity
| # | Category | File | Description | Effort |
|---|----------|------|-------------|--------|

### Low Severity
(summary only)
```
