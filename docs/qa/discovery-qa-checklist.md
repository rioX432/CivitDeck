# Discovery QA — Pre-Release Checklist (issue #990)

Deterministic QA foundation for the discovery flow. **This is a PoC and is intentionally
NOT wired as a blocking CI gate** (repo norm — PoC uses local / half-automated checks). Run
it as a pre-release checklist.

## What ships as runnable, deterministic tests

| Check | Command | Notes |
|---|---|---|
| Semantic ranking golden set | `./gradlew :core:core-database:jvmTest` | `SemanticRankingGoldenTest` — labelled corpus + golden top-K over the real Fp16 + cosine path. |
| Fixture-served discovery (unit) | `./gradlew :core:core-network:testAndroidHostTest` | `CivitAiApiFixtureTest` — MockEngine serves recorded `DiscoveryFixtures` against the injected E2E base URL; asserts deterministic parse and that production endpoints never drift. |
| Desktop discovery smoke | `./gradlew :desktopApp:jvmTest` | `DesktopDiscoverySmokeTest` — Compose UI-test robot drives the unified search bar (Maestro doesn't cover JVM). |

## Base-URL-injectable E2E seam

- `CivitAiEndpoints` (core-network) is the single seam. Production defaults to civitai.com;
  `CivitAiApi` takes it (default `Production`), and Koin injects it explicitly.
- Android: the base URLs are `CIVITAI_BASE_URL` / `CIVITAI_TRPC_BASE_URL` BuildConfig fields,
  production by default. A **debug** build assembled with `-Pcivitdeck.e2eBaseUrl=<host>`
  overrides them to the fixture server; `CivitDeckApplication` reads them into
  `CivitAiEndpoints` and passes them to `initKoin(endpoints:)`. This was deliberately chosen
  over a full `environment` product flavor: a flavor also produces `e2eRelease` variants, so a
  signed release APK carrying the cleartext fixture URL could be built. Gating the override on
  the debug build type guarantees the fixture URL can never enter a release artifact.
- iOS Simulator: SKIE wiring is verifiable on the Simulator; to run the deterministic flow,
  pass a `CivitAiEndpoints` into `initKoin(endpoints:)` from an E2E scheme (Info.plist / launch
  arg) that points at `http://127.0.0.1:8080`. **Note:** the current iOS `iOSApp.swift` calls the
  no-endpoints `doInitKoin` overload (→ Production); add the E2E scheme when wiring the on-device
  run.

## Test tags / accessibility identifiers

Shared contract: `DiscoveryTestTags` (core-ui commonMain), mirrored on iOS as accessibility
identifiers. Discovery-critical nodes: `discovery_search_field`, `discovery_model_grid`,
`discovery_model_card`, `model_detail_root`, `model_favorite_button`,
`gallery_save_prompt_button`.

- Android + Desktop: search field, grid, and card carry the shared `testTag`s; detail root,
  favorite, and save-prompt are tagged on Android.
- iOS: currently mirrors `discovery_search_field`, `discovery_model_grid`, and
  `discovery_model_card`. The remaining three (`model_detail_root`, `model_favorite_button`,
  `gallery_save_prompt_button`) are tracked for the iOS UI-test wiring pass — see the manual
  checklist below.

## Maestro flows (Android)

Flows live in `.maestro/` (split, single-responsibility):
- `discovery_search_to_collection.yaml` — search → detail → save to collection.
- `gallery_save_prompt.yaml` — gallery → save prompt.

Run locally:

```bash
# 1. Serve recorded fixtures on :8080 so /api/v1/... returns DiscoveryFixtures deterministically.
#    (Any static server that maps the CivitAI paths to the recorded JSON works.)
# 2. Build + install the debug APK with the E2E base URL:
./gradlew :androidApp:assembleGithubFullDebug -Pcivitdeck.e2eBaseUrl=http://10.0.2.2:8080
adb install androidApp/build/outputs/apk/githubFull/debug/androidApp-githubFull-debug.apk
# 3. Run flows:
maestro test .maestro/
```

The workflow is `.github/workflows/maestro-smoke-test.yml.template` (manual trigger only —
rename to activate). **Do not turn it into a required check.**

## Manual, device-only checklist (cannot be automated here)

- [ ] iOS on-device Core ML: measure semantic-search inference speed, peak memory, and thermal
      state on a physical device. The ANE (Apple Neural Engine) is only exercised on device — the
      Simulator runs CPU/GPU fallbacks, so perf numbers there are not representative.
- [ ] Android on-device: run both `.maestro` flows against the fixture server.
- [ ] Confirm production builds resolve `CivitAiEndpoints.Production` (guarded by
      `CivitAiApiFixtureTest.productionEndpointsRemainCivitAi`).
- [ ] iOS: add accessibility identifiers for `model_detail_root`, `model_favorite_button`, and
      `gallery_save_prompt_button` when wiring the iOS UI-test target.
