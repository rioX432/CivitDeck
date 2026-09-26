# UX Audit Reference

Use only the sections relevant to the selected mode and flow.

## Contents

- Finding quality and precedence
- UX lenses
- WCAG 2.2 AA checks
- Platform checks

## Primary standards

- [WCAG 2.2 Recommendation](https://www.w3.org/TR/WCAG22/)
- [What's New in WCAG 2.2](https://www.w3.org/WAI/standards-guidelines/wcag/new-in-22/)
- [WCAG 2.5.8 Target Size (Minimum)](https://www.w3.org/WAI/WCAG22/Understanding/target-size-minimum)
- [Apple accessibility guidance](https://developer.apple.com/design/human-interface-guidelines/accessibility)
- [Android accessibility guidance](https://developer.android.com/guide/topics/ui/accessibility)

Check the current primary source when a criterion, exception, or platform recommendation determines severity.

## Finding quality and precedence

Judge against sources in this order:

1. User goal and explicit brief
2. Project design system, tokens, components, and established flows
3. Platform conventions
4. General usability and visual-design heuristics
5. Reviewer preference — context only, never a defect by itself

Good feedback is specific, explains user impact, names its evidence, proposes a bounded alternative, acknowledges
what works, and matches the product's maturity. Preserve effective patterns while fixing the problem.

Classify evidence:

- **observed** — reproduced in the running product or measured directly
- **likely** — supported by code or a screenshot but not exercised
- **unverified** — plausible, but required evidence is unavailable

## UX lenses

### First impression

- Is the screen's purpose clear within two seconds?
- Does attention land on the intended content or primary action?
- Is the next step obvious without recalling prior instructions?

### Nielsen's ten heuristics

1. **Visibility of system status** — async progress, selection, save state, network state, completion feedback.
2. **Match with the real world** — user language, familiar concepts, locale-aware formats, natural order.
3. **User control and freedom** — back, cancel, undo, escape, and recovery from accidental actions.
4. **Consistency and standards** — terminology, interaction, platform patterns, and design-system use.
5. **Error prevention** — constraints, validation, confirmation proportional to consequence, safe defaults.
6. **Recognition over recall** — visible choices, labels, history, suggestions, and contextual guidance.
7. **Flexibility and efficiency** — shortcuts or bulk actions only where repeated use justifies them.
8. **Aesthetic and minimalist design** — clear hierarchy and relevant content, not a specific visual taste.
9. **Error recognition and recovery** — plain explanation, retained input, retry, and a viable next action.
10. **Help and documentation** — contextual help for genuinely unfamiliar or complex tasks.

Also inspect task entry, information architecture, trust and reassurance, empty/default states, copy/CTA outcome,
and whether destructive or irreversible effects are clear before commitment.

## WCAG 2.2 AA checks

Use the normative criterion and its exceptions when assigning a violation. This list is a route, not a substitute
for the standard.

### Perceivable

- 1.1.1 text alternatives for meaningful non-text content
- 1.2.x captions and alternatives for relevant time-based media
- 1.3.1/1.3.2 semantic structure and meaningful reading sequence
- 1.3.4 orientation and 1.3.5 input purpose
- 1.4.1 information not conveyed by color alone
- 1.4.3 text contrast: 4.5:1 normal, 3:1 large text
- 1.4.10 reflow and 1.4.11 non-text contrast
- 1.4.12 text spacing and 1.4.13 hover/focus content

### Operable

- 2.1.1 keyboard operation and 2.1.2 no keyboard trap
- 2.2.x timing controls where a time limit exists
- 2.3.1 flashing limits
- 2.4.1 bypass blocks; 2.4.3 focus order; 2.4.7 focus visible
- 2.4.11 focus not obscured (minimum)
- 2.5.1 pointer gestures; 2.5.2 pointer cancellation; 2.5.3 label in name
- 2.5.7 dragging movements have a non-drag alternative
- 2.5.8 target size (minimum): 24 by 24 CSS px or sufficient spacing, subject to equivalent, inline,
  user-agent-control, essential, and other normative exceptions

Do not present 44 by 44 CSS px as the WCAG AA minimum. It is the enhanced AAA target in 2.5.5 and may still be
a useful product target. Keep Apple 44pt and Android 48dp guidance labeled as platform guidance.

### Understandable

- 3.1.1 page language and 3.1.2 language of parts
- 3.2.1/3.2.2 predictable changes on focus and input
- 3.2.3 consistent navigation; 3.2.4 consistent identification; 3.2.6 consistent help
- 3.3.1 error identification; 3.3.2 labels/instructions; 3.3.3 error suggestions; 3.3.4 error prevention
- 3.3.7 redundant entry and 3.3.8 accessible authentication (minimum)

### Robust

- 4.1.2 programmatic name, role, value
- 4.1.3 status messages exposed without moving focus

### Test methods

Record which were actually run:

- existing automated accessibility suite
- keyboard-only traversal and focus visibility
- TalkBack, VoiceOver, or NVDA traversal
- computed contrast measurement
- 200% text zoom and 400% reflow where applicable
- reduced motion and system text-size settings
- error, validation, loading, empty, and status-message behavior

## Platform checks

### Android / Compose

- Semantics and meaningful labels; decorative images excluded appropriately
- 48dp recommended interactive target or documented alternative
- Font scaling, TalkBack order, edge-to-edge and insets
- Project theme/tokens before generic Material defaults
- Loading, empty, error, offline, and permission states

### iOS / SwiftUI

- Accessibility label/value/hint and traits appropriate to the control
- 44pt recommended hit target or documented alternative
- Dynamic Type, VoiceOver order, safe areas, Reduce Motion
- Native navigation and recovery expectations where they serve the task
- Loading, empty, error, offline, and permission states

### React Native

- `accessibilityLabel`, `accessibilityRole`, state/value, and decorative image handling
- Platform target guidance, Dynamic Type/font scaling, screen-reader order
- Safe areas, keyboard avoidance, platform back behavior, reduced motion
- Avoid fixed heights that fail with translated or enlarged text

### Web

- Native semantic elements before ARIA; links for navigation and buttons for actions
- Associated labels, descriptions, errors, autocomplete, and status announcements
- Dialog focus entry/containment/return, escape behavior, and background inertness
- Keyboard access, skip mechanism where repeated blocks justify it, visible unobscured focus
- Responsive reflow, zoom, text spacing, pointer alternatives, and target-size exceptions
- Use the project's component and token system; a hardcoded value is a finding only when it causes inconsistency,
  bypasses a deliberate token contract, or harms the audited outcome
