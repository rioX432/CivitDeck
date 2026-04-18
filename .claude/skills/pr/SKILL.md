---
name: pr
description: "Create a pull request for the current branch using the project's PR template"
user-invocable: true
disable-model-invocation: true
allowed-tools:
  - Read
  - Grep
  - Glob
  - Bash(git status)
  - Bash(git diff:*)
  - Bash(git log:*)
  - Bash(git push:*)
  - Bash(gh pr create:*)
  - Bash(gh issue view:*)
---

# /pr — Pull Request Creation

Create a pull request for the current branch.

## Steps

1. `git status` and `git diff` to understand all changes
2. Check if the current branch has a remote; push with `-u` if needed
3. `git log` and `git diff <base>...HEAD` to understand ALL commits
4. Look up the related GitHub Issue from branch name or commit messages
5. Read `.github/pull_request_template.md` if it exists
6. Generate changelog entry from commits (see below)
7. Create PR with `gh pr create`, include changelog entry in body

## Changelog Generation

From step 3 commits, generate a changelog entry categorized by type:

```
### Changelog
- **Added**: {new features}
- **Changed**: {modifications to existing features}
- **Fixed**: {bug fixes}
- **Removed**: {removed features}
```

Include this in the PR body after the description. If a `CHANGELOG.md` exists in the project root, prepend the entry under the `## [Unreleased]` section.

## Rules

- Title: `#{Issue} {concise description}` (under 70 chars)
- If no issue: omit the number
- Description: bullet points summarizing changes
- Other template sections: leave as-is
- No AI stamps, no Co-Authored-By
- Always set base branch explicitly
- Link issues with `Closes #XX` in body if applicable
