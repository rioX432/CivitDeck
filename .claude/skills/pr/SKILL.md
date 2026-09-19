---
name: pr
description: "Open a GitHub pull request for the current branch using the project PR template, with issue linking and a generated changelog. Use when the user asks to open a PR, or as the PR step of /dev once the branch is committed and reviewed. Requires the gh CLI."
user-invocable: true
allowed-tools:
  - Read
  - Grep
  - Glob
  - Bash(git status)
  - Bash(git diff:*)
  - Bash(git log:*)
  - Bash(git add:*)
  - Bash(git commit:*)
  - Bash(git push:*)
  - Bash(gh pr create:*)
  - Bash(gh issue view:*)
---

# /pr — Pull Request Creation

Create a pull request for the current branch. This is the only skill that opens PRs; `/dev` hands its PR step here.

## Steps

1. `git status` — stop if there are uncommitted changes that belong to this work; they must be committed first. `workspace/` holds `/dev` scratch artifacts; ignore it
2. Resolve the base branch (the caller's base, otherwise the remote default branch — do not assume `main`)
3. `git log {base}..HEAD` and `git diff {base}...HEAD` to understand ALL commits
4. Look up the related GitHub Issue from branch name or commit messages
5. Read `.github/pull_request_template.md` if it exists
6. Generate the changelog entry from the commits (see below). If a `CHANGELOG.md` exists in the project root, prepend the entry under `## [Unreleased]` and commit it (`git add CHANGELOG.md` + a one-line commit) so the PR contains it
7. Push the branch (`git push -u origin {branch}` if it has no upstream)
8. Create the PR with `gh pr create --base {base}`, changelog entry included in the body

## Changelog Generation

From step 3 commits, generate a changelog entry categorized by type:

```
### Changelog
- **Added**: {new features}
- **Changed**: {modifications to existing features}
- **Fixed**: {bug fixes}
- **Removed**: {removed features}
```

Include this in the PR body after the description.

## Rules

- Title: `#{Issue} {concise description}` (under 70 chars)
- If no issue: omit the number
- Description: bullet points summarizing changes
- Other template sections: leave as-is
- No AI stamps, no Co-Authored-By
- Always set base branch explicitly
- Link issues with `Closes #XX` in body if applicable
- When called from `/dev`, add its recorded assumptions (autonomous-mode defaults) to the description so the reviewer sees them
