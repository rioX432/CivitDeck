---
name: release
description: "Bump version, update CHANGELOG, tag, push — triggers release workflow"
argument-hint: "<version> (e.g. 2.1.0) or major | minor | patch"
user-invocable: true
disable-model-invocation: true
allowed-tools:
  - Bash(git add:*)
  - Bash(git commit:*)
  - Bash(git push:*)
  - Bash(git tag:*)
  - Bash(git diff:*)
  - Bash(git log:*)
  - Bash(git status)
  - Bash(gh run:*)
  - Glob
  - Grep
  - Read
  - Edit
  - AskUserQuestion
---

# /release — Version Bump & Release

Bump version across all platforms, update CHANGELOG.md, commit, tag, and push to trigger the release workflow.

**Arguments:** "$ARGUMENTS"

---

## Step 1: Determine Target Version

### 1a. Read current version

Read `androidApp/build.gradle.kts` and extract:
- `versionName` (e.g. `"2.0.0"`) — current semver
- `versionCode` (e.g. `4`) — current build number

### 1b. Calculate new version

Parse `$ARGUMENTS`:

- **Explicit version** (e.g. `2.1.0`): Use as-is. Validate semver format (X.Y.Z).
- **`patch`**: Increment Z (e.g. `2.0.0` → `2.0.1`)
- **`minor`**: Increment Y, reset Z (e.g. `2.0.0` → `2.1.0`)
- **`major`**: Increment X, reset Y and Z (e.g. `2.0.0` → `3.0.0`)
- **Empty or invalid**: Use `AskUserQuestion` to ask:
  - "What version? Enter a semver (e.g. 2.1.0) or type major/minor/patch"

New `versionCode` = current versionCode + 1.

### 1c. Validate

- New version must be greater than current version
- Format must match `X.Y.Z` where X, Y, Z are non-negative integers

---

## Step 2: Update Version in All Locations

### 2a. `androidApp/build.gradle.kts`

Use `Edit` to update:
- `versionCode = {old}` → `versionCode = {new}`
- `versionName = "{old}"` → `versionName = "{new}"`

### 2b. `desktopApp/build.gradle.kts`

Use `Edit` to update:
- `packageVersion = "{old}"` → `packageVersion = "{new}"`

### 2c. `iosApp/iosApp/Info.plist`

Use `Edit` to update:
- `<key>CFBundleShortVersionString</key>` followed by `<string>{old}</string>` → `<string>{new major.minor}</string>`
- `<key>CFBundleVersion</key>` followed by `<string>{old}</string>` → `<string>{new versionCode}</string>`

Note: CFBundleShortVersionString uses `X.Y` format (no patch), CFBundleVersion uses the versionCode integer.

---

## Step 3: Update CHANGELOG.md

### 3a. Read current CHANGELOG.md

Read the file and find the `## [Unreleased]` section.

### 3b. Transform Unreleased section

If the `[Unreleased]` section has content:
1. Keep `## [Unreleased]` header with empty content below it
2. Insert new version section between `[Unreleased]` and the previous version:

```markdown
## [Unreleased]

## [{new version}] - {YYYY-MM-DD}

{content that was under [Unreleased]}
```

If the `[Unreleased]` section is empty:
- Still create the version section header (it will be empty — that's OK, the release notes come from the section content)

### 3c. Update comparison links at bottom

Find the `[Unreleased]:` link and update:
- `[Unreleased]: https://github.com/rioX432/CivitDeck/compare/v{old}...HEAD`
  → `[Unreleased]: https://github.com/rioX432/CivitDeck/compare/v{new}...HEAD`
- Add new version link:
  `[{new}]: https://github.com/rioX432/CivitDeck/compare/v{old}...v{new}`

---

## Step 4: Confirm with User

Use `AskUserQuestion` to present:

```
Release v{new version} (versionCode: {new code})

Version updates:
  - androidApp: versionName={new}, versionCode={new code}
  - desktopApp: packageVersion={new}
  - iOS: CFBundleShortVersionString={X.Y}, CFBundleVersion={new code}

CHANGELOG: [Unreleased] content moved to [{new}] - {date}

Proceed with commit, tag, and push?
```

Wait for user confirmation. If denied, stop.

---

## Step 5: Commit, Tag, and Push

### 5a. Stage and commit

```bash
git add androidApp/build.gradle.kts desktopApp/build.gradle.kts iosApp/iosApp/Info.plist CHANGELOG.md
git commit -m "Bump version to {new version}"
```

No Co-Authored-By, no AI stamps.

### 5b. Create tag

```bash
git tag v{new version}
```

### 5c. Push

```bash
git push && git push --tags
```

This triggers the `.github/workflows/release.yml` workflow.

---

## Step 6: Verify

### 6a. Check workflow trigger

```bash
gh run list --workflow=release.yml --limit 1
```

### 6b. Output result

Print:
```
Release v{new version} initiated.

Tag: v{new version}
Workflow: {run URL or "check https://github.com/rioX432/CivitDeck/actions"}

The release workflow will build:
  - Android signed APK
  - macOS DMG
  - Windows MSI
  - Linux DEB

Artifacts will be attached to the GitHub Release when complete.
```

---

## Error Handling

| Situation | Action |
|-----------|--------|
| Invalid version format | Ask user to re-enter |
| New version <= current version | Warn and ask user to confirm or re-enter |
| CHANGELOG.md not found | Warn, skip changelog update, continue |
| Info.plist not found | Warn, skip iOS version update, continue |
| Git push fails | Report error, do not retry |
| Workflow not triggered | Print manual check URL |
