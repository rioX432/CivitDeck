---
name: review-pr
description: Review a GitHub pull request for CivitDeck
user-invocable: true
disable-model-invocation: true
allowed-tools: Bash, Read, Grep, Glob, Task
---

# PR Code Review

Review the specified pull request (number or URL as argument).

## Steps

1. **Get PR info**: Run `gh pr view <number> --json number,title,body,baseRefName,headRefName,files` to get PR metadata and changed files.
2. **Checkout the branch**: Run `git fetch origin <headRefName> && git checkout <headRefName>` to get the actual code.
3. **Read changed files**: Read each changed file in full to understand context — don't rely on diff alone.
4. **Launch platform-specific reviewers**: Based on changed file paths, use the Task tool to launch the appropriate reviewer subagents **in parallel**:
   - `.kt` files under `shared/` → `kmp-reviewer` agent
   - `.kt` files under `androidApp/` → `android-reviewer` agent
   - `.swift` files → `ios-reviewer` agent
   - Only launch reviewers for platforms that have changes. Pass the list of changed files to each reviewer.
5. **Run lint checks**:
   - Run `./gradlew detekt` if Kotlin files changed
   - Run `cd iosApp && swiftlint --strict` if Swift files changed
6. **Aggregate results**: Combine findings from all reviewers and lint checks. Output a structured review organized by severity (Critical / Important / Suggestion) with `file:line` references.
