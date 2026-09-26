# Design Personality — Project-grounded Compose UI

Distinctiveness comes from a coherent product point of view, not from replacing every Material default. Apply
this rule only after accessibility, the explicit brief, and the project's existing design system.

## Decision order

1. Accessibility and the user's task
2. Explicit product brief and brand requirements
3. Existing project tokens, components, and interaction patterns
4. Android and iOS platform conventions for the surface
5. Deliberate visual exploration

A font, color count, radius, shadow, spacing value, ripple, or easing curve is not wrong merely because another
choice is more fashionable. Flag it only when it breaks an earlier source in this order or creates measurable
inconsistency, ambiguity, or task friction.

## Establish a visual direction

Before introducing a new visual language, identify:

- the product purpose and intended feeling
- the content or action that should dominate attention
- the existing token and component vocabulary
- one or two intentional differentiators, such as typography, imagery, composition, or motion

Keep the rest restrained. Avoid both unmodified template defaults and arbitrary novelty.

## Tokens and components

- Express repeated typography, color, spacing, shape, elevation, and motion choices as project tokens.
- Reuse accessible Material 3 components when they satisfy the task. Customize them through the project theme
  before building bespoke controls.
- Preserve minimum target sizes, semantics, focus/pressed states, font scaling, contrast, and reduced-motion
  behavior when customizing.
- Use a spacing rhythm and radius family rather than one mandatory grid or radius. Exceptions are acceptable when
  the content or component geometry explains them.
- Dynamic color, shadows, borders, and surface tint are product decisions. Test them across light/dark themes and
  platform variants instead of banning or requiring them globally.

## Motion and feedback

- Motion should explain state change, hierarchy, continuity, or direct manipulation.
- Keep decorative motion subordinate to task completion and honor reduced-motion preferences.
- Every interactive control needs perceivable pressed, focused, disabled, loading, success, and error feedback as
  applicable. A custom indication must remain at least as discoverable as the platform default.

## Content states

Design loading, empty, error, offline, permission, and partial-content states as part of the flow. Choose skeleton,
spinner, inline feedback, illustration, or full-screen treatment according to expected duration and user action;
no single treatment is universally correct.

## Reference-driven work

Use references to communicate a property, not to copy a brand:

- Name what is relevant: hierarchy, density, content treatment, typography, motion, or navigation.
- Compare the reference with project tokens and platform behavior.
- Keep assets and interaction patterns original unless the user has rights and explicitly asks for faithful
  reproduction.

## Review boundary

This rule guides implementation and optional brand-fit critique. Accessibility requirements belong to the
platform conventions and the UI reviewer. A review finding must cite the project brief, token, component,
platform rule, or observed user impact that the code violates.
