---
name: ux-audit
description: "Comprehensive UI/UX audit: heuristic evaluation, accessibility, visual analysis, platform guidelines, and improvement proposals — then create GitHub Issues"
argument-hint: "[URL, 'mobile', or specific screen/module to audit]"
user-invocable: true
disable-model-invocation: true
allowed-tools:
  - Read
  - Glob
  - Grep
  - Agent
  - WebSearch
  - AskUserQuestion
  - Bash(gh issue create:*)
  - Bash(gh issue list:*)
  - Bash(gh label create:*)
  - Bash(gh label list:*)
  - TaskCreate
  - TaskUpdate
  - TaskList
  - mcp__playwright__browser_navigate
  - mcp__playwright__browser_snapshot
  - mcp__playwright__browser_take_screenshot
  - mcp__playwright__browser_resize
  - mcp__playwright__browser_click
  - mcp__playwright__browser_close
  - mcp__playwright__browser_tabs
  - mcp__playwright__browser_evaluate
  - mcp__mobile-mcp__mobile_take_screenshot
  - mcp__mobile-mcp__mobile_list_elements_on_screen
  - mcp__mobile-mcp__mobile_click
  - mcp__mobile-mcp__mobile_click_on_screen_at_coordinates
  - mcp__mobile-mcp__mobile_swipe_on_screen
  - mcp__mobile-mcp__mobile_type_text
  - mcp__mobile-mcp__mobile_press_button
  - mcp__mobile-mcp__mobile_get_device_info
---

# /ux-audit — UI/UX Comprehensive Audit

Analyze the app's UI/UX quality, detect issues, propose incremental improvements, and file GitHub Issues.

**Target:** $ARGUMENTS (URL for web, `mobile` for emulator/simulator visual audit, or specific module/screen)

**This is a long-running skill.** Use TaskCreate to track phases.

## Analysis Mode Detection

Detect the analysis mode from `$ARGUMENTS` and available tools:

| Condition | Mode | Method |
|-----------|------|--------|
| URL provided + Playwright MCP available | **Web Visual** | Screenshots + A11y tree + axe-core |
| URL provided + no Playwright | **Web Code** | HTML/JSX/TSX static analysis |
| `mobile` + mobile-mcp available + emulator/simulator running | **Mobile Visual** | mobile-mcp screenshots + UI element dump |
| Kotlin/Swift project + no mobile-mcp | **Mobile Code** | Compose/SwiftUI code static analysis |
| Specific module/screen specified | **Scoped** | Analyze only the specified area |

---

## Phase 1: Screen Inventory

**Goal:** Build a list of screens to audit.

### Web Visual mode
1. Read CLAUDE.md for dev server command and app structure
2. If URL not provided, start dev server
3. Use Playwright to discover pages (navigate sitemap, router config, or nav links)

### Mobile Visual mode
1. Use `mobile_get_device_info` to confirm emulator/simulator is connected
2. Use `mobile_take_screenshot` to capture the current screen
3. Use `mobile_list_elements_on_screen` to get UI element hierarchy
4. Navigate through the app using `mobile_click` and `mobile_press_button` (Back)
5. Build screen list by exploring the app's navigation structure
6. For each screen: capture screenshot + dump UI elements

### Web Code / Mobile Code mode
1. Glob for UI files:
   - Web: `**/*.tsx`, `**/*.jsx`, `**/*.vue`, `**/*.svelte`
   - Android: `**/*Screen.kt`, `**/*Activity.kt`, `**/*Fragment.kt`, `**/ui/**/*.kt`
   - iOS: `**/*View.swift`, `**/*Screen.swift`, `**/*ViewController.swift`
2. Group by feature/module

### Output
Present screen list to user for confirmation:
```
Found N screens:
1. HomeScreen (src/ui/home/HomeScreen.kt)
2. SettingsScreen (src/ui/settings/SettingsScreen.kt)
...
```

**→ AskUserQuestion: Confirm screen list. Add/remove screens?**

---

## Phase 2: Multi-Agent Analysis

Launch **4 agents in parallel** (model: sonnet). Each agent analyzes all screens.

See `${CLAUDE_SKILL_DIR}/reference.md` for detailed criteria per agent.

### Agent A: Heuristic Evaluation
```
Evaluate the UI against Nielsen's 10 Usability Heuristics.

For Web Visual mode: analyze screenshots + accessibility tree.
For Mobile Visual mode: analyze screenshots + UI element dump from mobile-mcp.
For Code mode: analyze UI code structure and patterns.

For each screen, check all 10 heuristics (see reference.md for details).
Output: [screen] heuristic_number severity — finding — suggestion
Severity: Critical / Warning / Suggestion
```

### Agent B: Accessibility Audit
```
Check WCAG 2.2 AA compliance.

For Web Visual mode:
  1. Use browser_evaluate to inject and run axe-core:
     const script = document.createElement('script');
     script.src = 'https://cdn.jsdelivr.net/npm/axe-core@4/axe.min.js';
     // If CDN blocked by CSP, note it and fall back to code analysis
  2. Run axe.run() and collect results
  3. Also check: touch target sizes, focus order, color contrast visually

For Mobile Visual mode (mobile-mcp):
  1. Use mobile_list_elements_on_screen to dump all UI elements
  2. Check each element for:
     - contentDescription / accessibilityLabel presence
     - Bounds size (width/height >= 48dp Android, 44pt iOS)
     - Focusable/clickable attributes
  3. Use mobile_take_screenshot + Claude Vision for visual contrast check
  4. Test screen reader traversal: navigate elements sequentially

For Code mode: (see reference.md for platform-specific checks)
  - Android: contentDescription, clickable size >= 48dp, semantic elements
  - iOS: accessibilityLabel, frame >= 44pt, Dynamic Type support
  - Web: alt text, ARIA roles, semantic HTML, form labels

Output: [screen] wcag_criterion severity — finding — fix
```

### Agent C: Visual & Layout Check
```
Check for visual issues and layout consistency.

For Web Visual mode:
  1. Capture at 3 viewports: desktop(1280), tablet(768), mobile(375)
  2. Compare screenshots across viewports for responsive issues
  3. Check: element overflow, text truncation, image aspect ratio, spacing consistency

For Mobile Visual mode (mobile-mcp):
  1. Capture screenshot of each screen
  2. Analyze with Claude Vision for:
     - Element overlap or clipping
     - Text truncation without ellipsis
     - Image aspect ratio distortion
     - Inconsistent spacing/alignment
  3. Compare portrait vs landscape (rotate device if supported)

For Code mode:
  - Hardcoded dimensions (px instead of dp/sp/rem/%)
  - Missing responsive breakpoints or adaptive layouts
  - Inconsistent spacing/padding values
  - Missing dark mode support (if theme system exists)

Output: [screen] severity — finding — suggestion
```

### Agent D: Platform Guidelines
```
Check compliance with platform design guidelines.

For Android (Compose):
  - Material Design 3 component usage (vs custom components)
  - Proper use of MaterialTheme (colorScheme, typography, shapes)
  - Navigation patterns (NavHost, TopAppBar, BottomNavigation)
  - Edge-to-edge display handling

For iOS (SwiftUI):
  - HIG compliance (navigation, tab bars, safe areas)
  - SF Symbols usage where appropriate
  - Dynamic Type support (.font(.body) not .font(.system(size:)))
  - SwiftUI lifecycle best practices

For Web:
  - Semantic HTML usage
  - Consistent design system/token usage
  - Standard interaction patterns (forms, navigation, modals)

Output: [screen] guideline severity — finding — suggestion
```

---

## Phase 3: Improvement Proposals

**Goal:** Generate actionable, incremental improvement proposals from Phase 2 findings.

### Rules
1. **Never propose a complete UI redesign** — only incremental changes
2. **Preserve existing functionality** — improvements must not break current behavior
3. **Each proposal specifies blast radius** — which screens/components are affected
4. **Ranked by impact/effort ratio** — quick wins first

### For each proposal:
```
| # | Current State | Proposed Change | Impact | Blast Radius | Effort |
|---|--------------|-----------------|--------|-------------|--------|
| 1 | No loading state on HomeScreen | Add skeleton loading | High | HomeScreen only | S |
| 2 | Touch targets 32dp on SettingsScreen | Increase to 48dp min | High | SettingsScreen | S |
```

---

## Phase 4: Report & Issue Filing

### 4a. Present Report

```
## UX Audit Report

**Target:** {app}
**Mode:** {Web Visual / Mobile Code / ...}
**Screens analyzed:** N
**Date:** YYYY-MM-DD

### Summary
| Category | Critical | Warning | Suggestion | Total |
|----------|----------|---------|------------|-------|
| Heuristic | N | N | N | N |
| Accessibility | N | N | N | N |
| Visual/Layout | N | N | N | N |
| Platform Guidelines | N | N | N | N |

### Critical (must fix)
| # | Category | Screen | Description | Proposed Fix |
|---|----------|--------|-------------|-------------|

### Warning (should fix)
...

### Suggestion (nice to have)
...

### Improvement Proposals (ranked)
| # | Current | Proposed | Impact | Scope | Effort |
|---|---------|----------|--------|-------|--------|
```

### 4b. AskUserQuestion

**→ Which findings should become GitHub Issues?**
- All Critical + Warning (recommended)
- All findings
- Let me select
- None (report only)

### 4c. Create GitHub Issues

For each selected finding:
```bash
gh issue create \
  --title "UX: {description}" \
  --body "$(cat <<'EOF'
## Problem
{description}

## Screen
{screen name and file path}

## Current State
{what it looks like now}

## Proposed Fix
{incremental improvement}

## Impact
{who is affected, severity}

## References
- WCAG 2.2: {criterion if applicable}
- {Platform guideline if applicable}
EOF
)" \
  --label "ux,{severity}"
```

---

## Severity Classification

| Severity | Criteria |
|----------|----------|
| **Critical** | Accessibility blocker (screen reader can't access), app unusable on certain devices, WCAG A violation |
| **Warning** | Poor usability (confusing flow, missing feedback), WCAG AA violation, guideline deviation |
| **Suggestion** | Improvement opportunity (better patterns exist), minor inconsistency, polish |

---

## Error Handling

| Situation | Action |
|-----------|--------|
| Playwright MCP not available | Switch to Code analysis mode, note in report |
| axe-core CDN blocked by CSP | Fall back to code-based a11y checks, note in report |
| Dev server won't start | Ask user for URL or switch to Code mode |
| No UI files found | Report error, suggest checking $ARGUMENTS |
| `gh issue create` fails | Output issue content as text for manual creation |
