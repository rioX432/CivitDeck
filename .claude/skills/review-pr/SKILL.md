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
4. **Check architecture compliance**:
   - Shared logic in `commonMain`, not in platform modules
   - DTOs separate from domain entities
   - ViewModels in platform modules, not shared
   - Correct use of design tokens (no hardcoded colors/spacing)
5. **Check platform patterns**:
   - Android: Coil usage, Compose best practices, detekt compliance
   - iOS: CachedAsyncImage (not third-party), feature-based structure, SwiftLint compliance
6. **Check code quality**:
   - Run `./gradlew detekt` if Kotlin files changed
   - Run `cd iosApp && swiftlint --strict` if Swift files changed
7. **Output structured review**: Provide findings organized by severity (blocking, suggestion, nit).
