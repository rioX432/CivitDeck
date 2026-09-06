# UX Audit Reference — Detailed Criteria

Detailed evaluation criteria for the four analysis agents in SKILL.md Phase 2.

## Contents
- Nielsen's 10 Usability Heuristics
- WCAG 2.2 AA Checklist (Key Items)
- Platform-Specific Checks

---

## Nielsen's 10 Usability Heuristics

### 1. Visibility of System Status
- Loading indicators present for async operations
- Progress feedback for multi-step processes
- Clear indication of current state (selected tab, active page)
- Network/offline status communicated

### 2. Match Between System and Real World
- Labels use user-familiar language (not technical jargon)
- Icons are recognizable and standard
- Information organized in natural/logical order
- Date/time/currency formats match user locale

### 3. User Control and Freedom
- Back/undo actions available and easy to find
- Cancel option for destructive processes
- Easy navigation to previous state
- No dead-end screens

### 4. Consistency and Standards
- Same action = same result across screens
- Consistent terminology throughout
- Standard platform patterns used (navigation, gestures)
- Visual consistency (colors, fonts, spacing)

### 5. Error Prevention
- Confirmation dialogs for destructive actions
- Input validation before submission
- Disabled states for invalid actions
- Smart defaults that reduce input errors

### 6. Recognition Rather Than Recall
- Options visible (not hidden behind gestures)
- Recent/suggested items shown
- Labels on icons (not icon-only)
- Search and filter available for long lists

### 7. Flexibility and Efficiency of Use
- Shortcuts for power users
- Customizable frequent actions
- Batch operations where applicable
- Quick actions (long press, swipe)

### 8. Aesthetic and Minimalist Design
- No irrelevant information on screen
- Visual hierarchy guides the eye
- Adequate whitespace
- Content-to-chrome ratio is high

### 9. Help Users Recognize, Diagnose, and Recover from Errors
- Error messages in plain language (not error codes)
- Specific problem identification
- Constructive suggestion for resolution
- Retry/recovery action available

### 10. Help and Documentation
- Onboarding for first-time users
- Contextual help available
- FAQ/help accessible from the app
- Tooltips for complex features

---

## WCAG 2.2 AA Checklist (Key Items)

### Perceivable
- [ ] Text alternatives for images (alt text / contentDescription / accessibilityLabel)
- [ ] Captions for video/audio content
- [ ] Color contrast: 4.5:1 for normal text, 3:1 for large text
- [ ] Content readable without color (don't use color alone to convey info)
- [ ] Text resizable to 200% without loss of content

### Operable
- [ ] All functionality available via keyboard/switch access
- [ ] Focus order is logical (top-to-bottom, left-to-right)
- [ ] Focus indicator visible
- [ ] Touch target minimum: 44x44pt (iOS) / 48x48dp (Android) / 24x24px (web)
- [ ] No time limits (or user can extend)
- [ ] Skip navigation available (web)

### Understandable
- [ ] Language of page declared
- [ ] Input purpose identifiable (autocomplete attributes)
- [ ] Error identified and described in text
- [ ] Labels associated with form inputs

### Robust
- [ ] Valid markup / proper component usage
- [ ] Name, role, value available to assistive tech
- [ ] Status messages available to screen readers

---

## Platform-Specific Checks

### Android (Jetpack Compose)

| Check | What to look for | Severity |
|-------|-----------------|----------|
| contentDescription | `Image`, `Icon`, `IconButton` without contentDescription | Warning |
| Touch target | `Modifier.clickable` area < 48.dp | Warning |
| Material3 | Custom components where M3 equivalent exists | Suggestion |
| Theme usage | Hardcoded colors instead of MaterialTheme.colorScheme | Warning |
| Font scaling | Fixed `sp` sizes that don't scale with system settings | Warning |
| Preview | `@Preview` missing for screen Composables | Suggestion |
| Loading state | No loading indicator during data fetch | Warning |
| Error state | Missing error UI for failed operations | Warning |
| Empty state | No message when list is empty | Suggestion |
| Navigation | Inconsistent back handling | Warning |

### iOS (SwiftUI)

| Check | What to look for | Severity |
|-------|-----------------|----------|
| accessibilityLabel | `Image`, `Button` without .accessibilityLabel | Warning |
| Touch target | `.frame()` < 44pt for tappable elements | Warning |
| Dynamic Type | `.font(.system(size:))` instead of `.font(.body)` etc. | Warning |
| Safe areas | Content behind notch/home indicator | Critical |
| SF Symbols | Custom icons where SF Symbols equivalent exists | Suggestion |
| Navigation | Non-standard navigation patterns | Warning |
| Loading state | No ProgressView during async operations | Warning |
| Error state | Missing error alert/view | Warning |
| Empty state | No ContentUnavailableView for empty lists | Suggestion |

### Web (React/Vue/Svelte)

| Check | What to look for | Severity |
|-------|-----------------|----------|
| Semantic HTML | `div` with onClick instead of `button` | Warning |
| Alt text | `img` without `alt` attribute | Warning |
| ARIA | Interactive custom components without ARIA roles | Warning |
| Focus management | Modal/dialog without focus trap | Warning |
| Form labels | `input` without associated `label` | Warning |
| Responsive | No media queries or container queries | Suggestion |
| Design tokens | Hardcoded hex colors instead of CSS variables/tokens | Suggestion |
| Loading state | No skeleton/spinner during data fetch | Warning |
| Error boundary | Missing error boundary for async components | Warning |
