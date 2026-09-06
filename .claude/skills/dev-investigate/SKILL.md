---
name: dev-investigate
description: "Investigate the codebase for one issue in a forked context, writing a self-contained investigation-report.md. Use when /dev needs codebase investigation without spending the caller's context on file reads."
context: fork
# The caller reads investigation-report.md immediately after invoking this skill.
# Forked skills default to background: true, which would let that read race the fork.
background: false
allowed-tools:
  - Read
  - Glob
  - Grep
  - Bash(git log:*)
  - Bash(git diff:*)
  - Bash(git blame:*)
  - Bash(find:*)
  - Agent
  - ToolSearch
---

# /dev-investigate — Forked-Context Investigation

Investigate the codebase for a given issue and write the findings to a file. Runs with `context: fork` so the caller receives only the report, not hundreds of file reads.

**Input:** $ARGUMENTS (issue details, keywords, affected areas from `/dev` Phase 1)

The method and report format live in [../investigate/report-format.md](../investigate/report-format.md) — read it before Step 3, including the **Issue-driven additions** section.

---

## Step 1: Parse Input

Extract from `$ARGUMENTS`: issue summary and acceptance criteria, keywords and technical terms, known affected areas.

Read `CLAUDE.md` for architecture, conventions, and directory structure.

## Step 2: Find Entry Points

1. **Grep** for keywords from the issue (class names, function names, error messages)
2. **Glob** for likely file patterns (feature directories, module names)
3. **git log** for recent changes in related areas
4. Settle on 3-6 entry points

## Step 3: Investigate

Follow [../investigate/report-format.md](../investigate/report-format.md): pick 2-4 axes, launch the Explore agents in parallel, apply the investigation checklist, then run the Think Twice pass.

## Step 4: Write the Report

Write `investigation-report.md` in the current working directory, using the report skeleton **plus the Issue-driven additions** (the `Changes Needed` column and the `Decision Points` section — `/dig` consumes them directly).

The report must be self-contained. The caller cannot see this fork's context, so anything not written down is lost.

---

## Error Handling

| Situation | Action |
|-----------|--------|
| Entry points unclear | Grep broadly, expand search patterns |
| Agent returns shallow results | Re-launch with more specific prompts |
| Codebase too large for full trace | Scope down; record what was covered **and what was skipped** |
| CLAUDE.md missing | Infer architecture from directory structure |
