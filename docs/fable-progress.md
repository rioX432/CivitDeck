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
- 2026-07-07: NSFW pipeline verified against the live API (user asked to double-check;
  suspicion confirmed on three axes): (1) /models model-version images carry a NUMERIC
  browsing-level bitmask (PG=1..Blocked=32) but the mapper only matched bits 1/2/4/8 —
  XXX(16)/Blocked(32)/masks fell through to None (explicit content classified safe);
  (2) omitting the /models nsfw param makes the API strip non-PG images → NSFW models
  rendered as broken cards (empty images), so "All" must send nsfw=true; (3) omitting
  the /images nsfw param now returns PG-only → "All" showed SFW-only galleries; must
  send nsfw=X. Curl evidence: 20 nsfw=true models contained 397 level-16 and 3 level-32
  images; /images without param returned browsingLevel=1 only.
- 2026-07-07: LazyVerticalGrid anchors by key when items are inserted at index 0 —
  late-loading recommendation rows landed invisibly ABOVE the viewport (looked like
  "recommendations broken" for ~30 min of debugging). Fix: follow insertions only when
  the user is at the very top. Debug trick that found it: temporary Logger.w of
  section count proved state was correct while the screen showed nothing.
- 2026-07-07: detekt TooManyFunctions threshold 30 hit on ModelSearchViewModel when
  adding onNsfwFilterLevelSelected — merged the two init preference observers into
  one observePreferences() instead of suppressing.
- 2026-07-07: `./gradlew :androidApp:installDebug` with the user's physical phone on
  wireless adb installed to ALL devices ("Installed on 3 devices") — always prefix
  ANDROID_SERIAL=emulator-5554. Flag the accidental install in the PR.
- 2026-07-07: Detail-screen "Versions gap" root cause: in a plain Row, the first chip
  that doesn't fully fit is measured with the tiny leftover width, its label wraps one
  character per line, and the chip grows ~800 px tall (a11y showed it as an 819px-tall
  CheckBox — M3 FilterChip's semantics role). Fixed by making FilterChipRow
  horizontally scrollable, which also makes version 4+ reachable at all.
- 2026-07-07: Bet 4 verified against a mock ComfyUI server (python http.server on host,
  reachable from the emulator via 10.0.2.2:8188, /queue + /system_stats + /history
  endpoints): guided setup connects, hub card shows green Online + Generate/Queue/
  Outputs, queue screen reads the mock. Good technique for future remote-flow testing.
- 2026-07-07: Bet 6: AGP left a stale 278 MB APK when only removing entries — always
  `:androidApp:clean` before before/after size comparisons. Clean debug APK: 275.6 MB ->
  126.7 MB. Runtime check confirmed the designed fallback fires
  ("SigLIP-2 vision asset missing" warning, no crash) and embedOnBrowse no-ops — which
  also stops downloading+embedding every browsed model's thumbnail for a disabled
  feature.
- 2026-07-11: User directives mid-run: (1) every improvement must land on ALL of
  Android/iOS/Desktop, not Android-only; (2) top pain point = list->detail transition
  breaks because the detail hero re-loads the image. Added the cross-platform parity
  section + Bet 7 to the plan.
- 2026-07-11: Bet 7 root causes found by frame-stepping a screen recording (ffmpeg
  fps=10 extraction): the shared element morphed as a GRAY box. Two independent bugs:
  (a) detail carousel loads full-res `image.url` with a shimmer loading slot — fixed
  with Coil's placeholderMemoryCacheKey(image.thumbnailUrl()) + rendering
  SubcomposeAsyncImageContent() in the loading slot when a placeholder painter exists;
  (b) RecommendationRow/CreatorProfileScreen passed RAW `?.url` as the nav thumbnail
  arg while the card cached `thumbnailUrl()` (450px) — placeholder missed the cache.
  Grid path was correct all along; only carousel/creator paths were broken. Verified
  by re-recording both paths: image now stays painted through the whole morph.
- 2026-07-11: Coil recipe notes: default memory-cache key = URL string only when the
  request has no transformations (size is NOT in the key), so explicit
  .memoryCacheKey(url) on grid requests + .placeholderMemoryCacheKey(url) on detail
  requests match deterministically even with different .size().
- 2026-07-11: Desktop inventory surprises: DesktopSearchScreen AND DesktopDiscoveryScreen
  had an INVERTED local NSFW filter (level All stripped NSFW models; level Off showed
  everything) layered on top of the shared VM/use-case filtering — deleted both, the
  shared layer already handles it. DesktopModelCard never called ModelCardLayout's
  onError so the multi-candidate thumbnail fallback silently didn't work on Desktop.
- 2026-07-11: Desktop GUI verification: `screencapture -l <CGWindowID>` works without
  accessibility permission (window-id lookup via a swift -e CGWindowList snippet);
  CGEventPost keystrokes also post fine. BUT the user was actively using the machine
  (focus stealing is hostile) — verified the search screen visually, stopped there,
  and left hub/detail/settings as compile-verified. Kill `:desktopApp:run` when done.
- 2026-07-11: CI gate now includes :core:core-domain:testAndroidHostTest and
  :core:core-network:testAndroidHostTest (added during this PR) — derive the gate from
  ci.yml, the old memory list is stale.
