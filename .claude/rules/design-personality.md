# Design Personality — Beyond Material 3 Defaults

Material 3 defaults produce generic, template-like UIs. This rule defines how to deviate from M3 to create polished, distinctive interfaces.

**Philosophy: iOS HIG's "deference to content" — UI should not shout. Content is the star.**

## Token Overrides

### Typography
- **Do NOT use Roboto** as the display/body font — it looks like every other Android app
- Use a modern alternative: Inter, Pretendard, Plus Jakarta Sans, or project-specific brand font
- Tighten the M3 typescale from 13 styles to **5-7**:
  - Display (hero text only)
  - Headline (section headers)
  - Title (card/list headers)
  - Body (main content)
  - Label (captions, chips, buttons)
- Letter spacing: tighter than M3 defaults (-0.02em to 0em)
- Noto Sans JP for Japanese text (not system default)

### Color
- **Single accent color** for CTAs and links only — do NOT spread Primary across surfaces
- M3 Secondary/Tertiary: do NOT use — they add noise
- Surfaces: high-neutral palette (grays, not colored surfaces)
- Dark mode: `#121212` to `#1C1C1E` (not pure black `#000000`)
- Dynamic color (Material You wallpaper): opt-in per project, not default

### Corner Radius
- **Unified radius**: 12.dp for all components (cards, buttons, dialogs, sheets)
- Small elements (chips, badges): 8.dp
- Full round: FAB only
- Do NOT use M3's per-component radius variation (Small/Medium/Large/ExtraLarge)

### Shadows
- Do NOT use M3's default `tonalElevation` — it looks flat and cheap
- Use custom soft shadows:
  ```
  Modifier.shadow(
    elevation = 4.dp,
    shape = RoundedCornerShape(12.dp),
    ambientColor = Color.Black.copy(alpha = 0.08f),
    spotColor = Color.Black.copy(alpha = 0.04f)
  )
  ```
- Shadow = "feel it, don't see it"
- Consider ComposeShadowsPlus for advanced shadow control

### Ripple / Press Indication
- Do NOT use M3 default ripple (large expanding circle = "sticky" feel)
- Replace with alpha fade:
  ```
  indication = null  // or custom fade with alpha = 0.06f
  ```
- Pressed state: subtle background alpha change only

### Animation & Motion
- Do NOT use M3's `FastOutSlowInEasing` for UI transitions — it feels robotic
- Use iOS-inspired easing:
  ```
  val SmoothEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)
  ```
- Duration: 250-350ms (shorter than M3 defaults)
- Spring animations:
  ```
  spring(dampingRatio = 0.75f, stiffness = 400f)
  ```
- Shared element transitions: use Compose's stable API

### Spacing
- **8pt grid, strictly enforced** — only these values:
  - 4.dp / 8.dp / 12.dp / 16.dp / 24.dp / 32.dp / 48.dp
- No arbitrary values (no 10.dp, 14.dp, 20.dp)
- Section gaps: 24.dp or 32.dp
- List item spacing: 8.dp or 12.dp
- Screen horizontal padding: 16.dp (consistent across all screens)

### Content Density
- List row height: 48.dp (not M3's 56.dp)
- Card internal padding: 12.dp to 16.dp
- Embrace whitespace — it's part of the design, not wasted space

## Component Rules

### Navigation
- Bottom navigation: max 5 items, icons + labels always visible
- Top app bar: use `LargeTopAppBar` with collapse behavior for content screens
- No navigation drawer unless absolutely necessary (prefer bottom sheet)

### Cards
- Minimal elevation — use subtle border (`0.5.dp`, `Color.Black.copy(alpha = 0.08f)`) instead of shadow
- Or: background color differentiation only (no border, no shadow)
- Content-first: image/data prominent, chrome minimal

### Buttons
- Primary: filled, single accent color, 12.dp radius
- Secondary: outlined or text-only, never filled with secondary color
- Destructive: red accent, text-only or outlined (never large filled red button)

### Empty / Loading / Error States
- Empty: illustration + message + single CTA (not just text)
- Loading: skeleton shimmer (not spinner) for content areas
- Error: inline message + retry, not full-screen error

## Reference-Driven Design

When implementing a new screen:
1. Find 2-3 reference screenshots from polished apps (Mobbin, App Store, competitor)
2. Include screenshots in the prompt to Claude Code
3. Specify: "Match this visual density and spacing, adapt to our design tokens"
4. Do NOT say "make it look like X app" — say what specifically to match (spacing, hierarchy, density)

## What This Rule Does NOT Cover

- Brand identity (logo, brand colors, illustrations) → project-specific
- Platform-specific adaptations (iOS vs Android nav patterns) → mobile-conventions.md
- Accessibility requirements (touch targets, contrast) → mobile-conventions.md + ui-reviewer agent
