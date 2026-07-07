# UX Consolidation — working notes

One note per decision/lesson. Newest at the bottom.

- 2026-07-07: Emulator exploration done (screenshots in session scratchpad `shots/`).
  Key code-confirmed causes: recommendations render unconditionally above search results
  (`ModelGridSection.recommendationItems`); model card thumbnail = first image, no
  video/NSFW check, no blur (`ModelGridSection.modelItems`, `ModelCard`); NSFW settings
  switch only toggles Off<->All so `Soft` is dead on Android (`NsfwToggleRow`).
- 2026-07-07: core-domain commonTest exists but is NOT in the CI Android gate — adding
  `:core:core-domain:testAndroidHostTest` to CI together with Bet 2's new domain tests.
- 2026-07-07: APK bloat evidence: assets/ml/siglip2_vision_q4f16.onnx 54 MB + 4x
  libonnxruntime.so ~112 MB while FEATURE_SIMILARITY_SEARCH=false. Bet 6, attempted last.
- 2026-07-07: Emulator gotchas: screenshots are 573x1280 but the device is 1280x2856 —
  multiply screenshot coords by ~2.234 for taps; a stylus IME tutorial can swallow
  `mobile_type_keys` (use `adb -s emulator-5554 shell input text`); a physical Samsung
  device joined adb mid-session, so every adb call needs `-s emulator-5554`.
- 2026-07-07: Versions-section dead space + scroll trap on the detail screen is visible
  on-device but the cause is not yet identified in code (FilterChipRow is a plain Row —
  not it). Investigate during Bet 3.
