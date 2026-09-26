---
name: dev-investigate
description: "Investigate the codebase for one issue in a forked context, writing a self-contained investigation-report.md. Use when /dev needs codebase investigation without spending the caller's context on file reads."
---

> **Host adaptation:** Treat product-specific tool names as capabilities. Use the
> equivalent tools available in the current host, ask through its interaction mechanism,
> and skip optional integrations that are unavailable. Resolve repository guidance from
> `AGENTS.md`, falling back to `CLAUDE.md` only when needed. `$ARGUMENTS` means the current
> user request; `${CLAUDE_SKILL_DIR}` means the directory containing this skill.

# /dev-investigate — Forked-Context Investigation

Investigate the codebase for a given issue and write the findings to a file. Runs with `context: fork` so the caller receives only the report, not hundreds of file reads.

**Input:** $ARGUMENTS (issue details, keywords, affected areas from `/dev` Phase 1)

The method and report format live in [../investigate/report-format.md](../investigate/report-format.md) — read it before Step 3, including the **Issue-driven additions** section.

---

## Step 1: Parse Input

Extract from `$ARGUMENTS`: the report path, issue summary and acceptance criteria, keywords and technical terms, known affected areas.

Read the nearest `AGENTS.md` for architecture, conventions, and directory structure. If absent, fall back to
`CLAUDE.md`, then infer only what the repository structure supports.

## Step 2: Find Entry Points

1. **Grep** for keywords from the issue (class names, function names, error messages)
2. **Glob** for likely file patterns (feature directories, module names)
3. **git log** for recent changes in related areas
4. Settle on 3-6 entry points

## Step 3: Investigate

Follow [../investigate/report-format.md](../investigate/report-format.md): pick 2-4 axes, launch the Explore agents in parallel, apply the investigation checklist, then run the Think Twice pass.

## Step 4: Write the Report

Write the report to the `Report path:` given in `$ARGUMENTS` (default `workspace/investigation-report.md`) — relative to the project working directory, not this skill's directory — creating the directory if needed, using the report skeleton **plus the Issue-driven additions** (the `Changes Needed` column and the `Decision Points` section — `/dig` consumes them directly).

The report must be self-contained. The caller cannot see this fork's context, so anything not written down is lost.

---

## Error Handling

| Situation | Action |
|-----------|--------|
| Entry points unclear | Grep broadly, expand search patterns |
| Agent returns shallow results | Re-launch with more specific prompts |
| Codebase too large for full trace | Scope down; record what was covered **and what was skipped** |
| Project guidance missing | Infer architecture from directory structure and label the inference |
