---
name: implement-guidance
description: "Implement one confirmed, bounded change in the current working tree: read before editing, follow existing patterns, work subtasks in dependency order, run each subtask's Verify step, and report changed files, verification output, assumptions, and blockers. Use when the change and its subtasks are already known; it does not investigate, review, commit, open PRs, or spawn agents."
argument-hint: "[change description or subtask list with Verify commands]"
user-invocable: true
allowed-tools:
  - Read
  - Grep
  - Glob
  - Edit
  - Write
  - Bash
---

# /implement-guidance — Bounded Implementation

Implement exactly the change handed to you, in the current working tree, and prove each step.

**Change:** $ARGUMENTS

This is one capability, not a workflow. It does not investigate the codebase broadly, resolve design choices,
create branches, run review, commit, push, open pull requests, or start other agents. Whoever called it — a person,
the standalone `/dev` wrapper, or a Control Plane — owns those steps. If the change cannot be done without one of
them, stop and report which one is missing.

## Inputs

- **The change**: a subtask list (each with a Verify command and dependencies, as `/decompose` produces) or a
  bounded request that fits one subtask.
- **Mode**: TDD when the caller says so, the issue has a `tdd` label, or the change is primarily about tests;
  otherwise standard.
- **Repository truth**: the nearest `AGENTS.md`, with `CLAUDE.md` as a host-specific fallback, for conventions and
  commands. Never guess a command the repository does not document.

Stop before editing and report `needs-investigation`, `needs-decision`, or `needs-decomposition` when the target
files are unknown, a design choice is still open, or the request does not fit a short ordered list of verifiable
steps. Do not start that work yourself.

## Implement

**Standard mode** (default):

```
LOOP for each subtask (in dependency order):
  1. Read the target code — never edit a file you have not read
  2. Implement the change (Edit/Write), following the surrounding code's patterns
  3. Run the subtask's Verify step and keep its output
  4. Mark the subtask done only when Verify shows its success signal
```

**TDD mode**:

```
LOOP for each subtask:
  1. Read the target code
  2. Write or update the test first
  3. Run it and confirm it fails for the expected reason (red)
  4. Implement the minimal code to pass
  5. Run it and confirm it passes (green)
  6. Refactor if needed, keeping it green
```

Guidelines:

- Keep the change minimal and inside the requested scope; note, do not fix, unrelated problems you see.
- Follow repository conventions and existing patterns over personal preference.
- A Verify step proves this subtask. Broader verification (affected modules, integration, full suite) belongs to
  the caller's verification profile; do not widen it here.

## Stop conditions

- The same Verify step fails 3 times after fixes → stop and report the failure with its output.
- An unexpected problem changes the plan (a missing API, a conflicting pattern, a needed design decision) → stop
  and report it. Ask only if a user is reachable in this session; otherwise record the question.
- The change needs files or behavior outside the requested scope → stop and report instead of widening it.

## Report

Return, in this order:

1. **Status**: `done`, `blocked`, or one of the `needs-*` values above
2. **Changed files**: path and one-line purpose each
3. **Verification**: per subtask, the exact command, exit code, and a short output excerpt with the success signal
4. **Assumptions**: each question, the choice made, and its evidence
5. **Blockers / follow-ups**: anything outside scope that the caller should schedule
