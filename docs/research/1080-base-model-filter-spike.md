# SPIKE #1080 — Base model filter that follows CivitAI's live base model list

Status: research only (no implementation). Decision note for owner review on the PR.
Follow-up implementation issues are filed only after this document merges.
Code references are to `origin/master` at `015654b4` (2026-10-03). CivitAI server references
are to `civitai/civitai` main at `6cfb0e4a` (2026-10-02).
API evidence is from live, unauthenticated calls on **2026-10-03 12:43 UTC** (`/api/v1/enums`
`Date: Sat, 03 Oct 2026 12:43:23 GMT`, `cache-control: max-age=0, private, no-cache`).

## Decision

| # | Question | Chosen option | One-line reason |
|---|---|---|---|
| 1 | Source of truth and fallback | **Live `GET /api/v1/enums`: `ActiveBaseModel` as the main list, `BaseModel` minus `ActiveBaseModel` as a collapsed "Older" group.** Offline or on error: last cached copy (any age), else a snapshot bundled in the app. | CivitAI's own docs say to fetch these lists live, and 22 of the 34 values outside `ActiveBaseModel` still return models, so hiding them would make those models unreachable again. |
| 2 | Caching | **Existing `LocalCacheDataSource`, key `civitai:enums`, pinned, 24 h TTL; the age is checked whenever the search screen starts or the picker opens, and a missing or older copy is refetched.** No new table. | The list is one ~3 KB JSON blob; the cache already has TTL and stale-read; pinning keeps it through the 7-day sweep and size eviction, and the bundled snapshot covers "Clear cache". |
| 3 | Domain type | **`data class BaseModel(val apiValue: String)` (not an enum, not a `value class`) plus a `BaseModelCatalog` that classifies a value as active, retired or unknown.** Every selected value is always shown; retired and unknown values get a visible marker. | Keeps the type name and `apiValue` used by every caller; Kotlin/Native does not export inline (value) classes to Swift, and iOS reads `selectedBaseModels` directly. |
| 4 | Filter UI | **One rule on Android, iOS and Desktop: the filter shows the selected values plus a "Choose base models" button; the picker has a search field, a "Selected" section, "Current" (API order) and a collapsed "Older" section; the selection is applied once when the picker closes.** | 78 + 34 chips cannot fit a sheet; `/enums` exposes no families to group by and its order already keeps families together, so search plus API order needs no curated data. |
| 5 | Saved filters and backups | **No database migration. Change only the mapper so that every stored value is kept.** | The column and the backup field already store `apiValue` strings, none of the 112 live values contains the `,` separator, and the only loss happens in `SavedSearchFilterRepositoryImpl.toDomain`. |
| 6 | Implementation split | **7 follow-up issues** (4 shared, 1 Android, 1 iOS, 1 Desktop), each about 5 files or fewer (F1: 6 small files). See §6. | Type, bundled catalog, live refresh, ViewModel and each platform UI can be reviewed and merged on their own. |
| 7 | `ModelType` | **Yes, later, as its own issue after this work merges.** | The same `/enums` response carries `ModelType` (23 values vs 14 in the app), but `ModelType` is also used by three DTO mappers, favorites and collections storage and recommendations, so it is a larger change than a filter list. |

Coverage today: of CivitAI's 100 most downloaded models this month, **1** has a latest version
whose base model the current 7-value filter can select. With `ActiveBaseModel` all 100 base
model values are selectable, and 99 of the 100 models are found through the filter (the 100th is
tagged `Other`, a value for which the API filter returns nothing).

---

## Current state (verified)

App (paths under the repository root, `015654b4`):

- `BaseModel` is a 7-value enum: SD 1.5, SDXL 1.0 (shown as "SDXL"), Pony, Flux.1 D, Flux.1 S,
  SD 2.1, SVD — `core/core-domain/src/commonMain/kotlin/com/riox432/civitdeck/domain/model/BaseModel.kt:3-11`.
- Android renders all enum entries as chips in a `FlowRow` —
  `androidApp/src/main/kotlin/com/riox432/civitdeck/ui/search/FilterSheetSection.kt:336-357`
  (`BaseModel.entries` at `:347`).
- iOS renders a hard-coded list of the same 7 as one horizontal chip row —
  `iosApp/iosApp/Features/Search/SearchFilterConstants.swift:5`,
  `iosApp/iosApp/Features/Search/ModelSearchScreen.swift:404-412`. `BaseModel` is aliased in
  `iosApp/iosApp/SharedTypeAliases.swift:8`, and the Swift ViewModel keeps a
  `Set<BaseModel>` (`iosApp/iosApp/Features/Search/ModelSearchViewModel.swift:16,63`).
- **Desktop has no base model filter at all.** `DesktopFilterBar` takes `onBaseModelToggled`
  but never uses it (`@Suppress("UnusedParameter")`) —
  `desktopApp/src/jvmMain/kotlin/com/riox432/civitdeck/ui/search/DesktopFilterBar.kt:23,31`.
  The issue text assumed Desktop shows the 7 values; it does not.
- Every chip tap calls `refresh()` immediately —
  `feature/feature-search/src/commonMain/kotlin/com/riox432/civitdeck/feature/search/presentation/ModelSearchViewModel.kt:304-312`.
  With a long list that means one search request per tap.
- The query passes `apiValue` straight through: `SearchPageLoader.kt:76` →
  `GetModelsUseCase.kt:19,30` → `ModelRepositoryImpl.kt:48` (cache key) and `:62` (request) →
  `CivitAiApi.kt:28,48` (one `baseModels=` parameter per value).
- Saved filters store `apiValue` strings joined with `,`
  (`core/core-database/.../entity/SavedSearchFilterEntity.kt:18`, written at
  `SavedSearchFilterRepositoryImpl.kt:67`). Backups copy that string unchanged
  (`core/core-database/.../backup/BackupDto.kt:117`, `BackupMappers.kt:45,134`).
  Loading drops every value that is not an enum entry (`SavedSearchFilterRepositoryImpl.kt:38-44`),
  so a stored filter whose values are all unknown silently becomes "any base model".
- Cache: `LocalCacheDataSource` (`core/core-database/.../LocalCacheDataSource.kt`) offers
  `getCached(key, ttlMillis)` (`:9`), `getCachedIgnoringTtl` (`:23`), `putCache` (`:27`, which
  also deletes unpinned entries older than 7 days, `:37` + `CachedApiResponseDao.kt:20-21`) and
  `pinForOffline` (`:40`). Settings → Clear cache deletes everything, pinned or not
  (`ClearCacheUseCase` → `CacheRepositoryImpl.kt:8` → `deleteAll`).
- Prior art for cache-with-fallback: `ModelRepositoryImpl.getCachedWithFallback`
  (`core/core-data/.../repository/ModelRepositoryImpl.kt:37-38`).

CivitAI (live API and source):

- `GET https://civitai.com/api/v1/enums` returns `ModelType` (23), `ModelFileType` (17),
  `ActiveBaseModel` (**78**), `BaseModel` (**112**), `BaseModelType` (4). On 2026-09-26 the audit
  saw 76 and 109: **the list grew by 2 active and 3 total values in one week.**
  Every `ActiveBaseModel` value is also in `BaseModel`.
- The docs: `baseModels` on `GET /models` is "string or string[]", allowed values "See GET /enums
  (BaseModel)" (https://developer.civitai.com/site/reference/models); the enums page says
  `BaseModel` is "the comprehensive historical catalog", `ActiveBaseModel` "the subset of
  `BaseModel` that Civitai's on-site generation currently supports", and "Always fetch live values
  rather than baking them into clients" (https://developer.civitai.com/site/reference/enums).
- The source disagrees with the docs on what "active" means: `/api/v1/enums` returns
  `activeBaseModels`, defined as base model records that are not `hidden`, the list used by
  "selection UIs (e.g., model version upsert form)"
  (https://github.com/civitai/civitai/blob/6cfb0e4ab589ecaaa572909eb6ca5d7d2af08ea8/src/pages/api/v1/enums.ts,
  https://github.com/civitai/civitai/blob/6cfb0e4ab589ecaaa572909eb6ca5d7d2af08ea8/packages/civitai-shared/src/basemodel.constants.ts
  lines 4351-4353 and 4971-4981). So "active" means "a creator can still pick it for a new
  upload", not "has models". Both arrays follow the order of `baseModelRecords`, which keeps
  families together (all `Flux.*`, all `SD *`, all `Wan Video *`).
- CivitAI's own grouped picker data (`baseModelSelectData`, grouped by ecosystem family, same file
  lines 3893-3907) is **not** exposed by `/enums`.

Probes against `GET /api/v1/models?limit=…&baseModels=…` (no API key, 2026-10-03):

| Probe | Result |
|---|---|
| `Krea 2`, `Anima`, `MiniMax H3` | 200, models returned |
| Unknown value `NotAModel` | 200, **0 items** (no error) |
| `NotAModel` + `Pony` | 200, Pony models — values are OR-ed, an unknown value next to a valid one is ignored |
| `sdxl 1.0` (wrong case) | 200, 0 items — matching is case-sensitive |
| All 78 `ActiveBaseModel` values, `limit=1` | 77 return models; `Other` returns 0 |
| All 34 values in `BaseModel` but not `ActiveBaseModel`, `limit=1` | 22 return models (e.g. `SVD XT`, `Wan Video`, `SDXL 1.0 LCM`, `Stable Cascade`, `SD 2.1 768`); 12 return 0 (`SD 3`, `SD 3.5`, `SD 3.5 Large`, `SD 3.5 Large Turbo`, `SD 3.5 Medium`, `SDXL Turbo`, `SVD`, `PolyGen`, `Tripo`, `Hunyuan3D`, `Pixal3D`, `Trellis.2`) |
| Top 100, `sort=Most Downloaded`, `period=Month`, base model of latest version | Krea 2 41, Anima 24, Illustrious 10, MiniMax H3 9, ZImageTurbo 6, Qwen 2.1 5, Qwen 2 / SDXL 1.0 / Flux.2 Klein 9B / ZImageBase / Other 1 each |

The app's `SVD` is in `BaseModel` but no longer in `ActiveBaseModel`, and it returns 0 models
today. A saved filter with only `SVD` therefore shows an empty result; with the decisions below it
shows the empty result together with a "retired" marker instead of being widened to all models.

---

## 1. Source of truth and fallback

**Chosen: `ActiveBaseModel` is the main list; `BaseModel` minus `ActiveBaseModel` is an "Older"
group; fallback order is fresh cache → stale cache → bundled snapshot.**

Options considered:

| Option | Verdict |
|---|---|
| `ActiveBaseModel` only | Rejected: 22 retired values still find models (`Wan Video`, `SVD XT`, `SDXL 1.0 LCM` …); those models would stay unreachable, which is the bug this spike is about. |
| `BaseModel` only, flat | Rejected: 112 flat entries, 12 of which find nothing, mixed in with current ones. |
| Curated in-app list | Rejected: it goes stale exactly like the enum (2 new active values in the last week). |
| **Mixed: active first, the rest in "Older"** | Chosen. Both come from the same live response; no curation. |

- The picker does not hide values that return no models (`Other`, `SD 3.5`, …). Finding that out
  costs one request per value; the empty-result state already explains it.
- Bundled snapshot: the two arrays from the 2026-10-03 response, as a Kotlin constant in
  `core-data` with the response date in a comment. It is only used when there has never been a
  successful fetch, or after "Clear cache" while offline. It does not need refreshing on every
  release; the live fetch covers drift.
- The request goes through the existing `CivitAiApi` client. `/enums` is public; whether the
  client's API key header is attached does not change the response.

## 2. Caching

**Chosen: `LocalCacheDataSource`, key `civitai:enums`, pinned with `pinForOffline`, TTL 24 h.**

- Read: `getCached(key, 24 h)`; if null, fetch; on fetch failure use `getCachedIgnoringTtl(key)`;
  if still null, use the bundled snapshot. Same shape as `ModelRepositoryImpl.getCachedWithFallback`.
- Write: `putCache(key, json)` then `pinForOffline(key)`, so the entry survives the 7-day sweep in
  `putCache` and `evictToSize` (`putCache` keeps an existing pin on later writes). "Clear cache"
  still removes it; the next check refetches, and the bundled snapshot covers the gap.
- Refresh trigger: every time the search screen starts observing the catalog **and** every time
  the base model picker opens, the repository checks the age of the cache entry and fetches if it
  is missing or older than 24 h (one request in flight at a time). The repository keeps no
  in-memory "already fetched" flag, so a process that stays alive for days, a first attempt made
  offline, and a "Clear cache" (which deletes the entry) all lead to a fetch at the next check.
  Not at app launch (no extra request on cold start), not on a timer. A failed refresh is logged
  and keeps the current list; it never shows an error.
- TTL 24 h: the list changed twice in a week, so a new base model becomes selectable within a
  day at the cost of one ~3 KB request per day.
- A new Room table was rejected: it needs a schema version bump and migration for a single JSON
  blob that `cached_api_responses` already stores, with TTL and stale-read already implemented.

## 3. Domain type

**Chosen: `data class BaseModel(val apiValue: String)` with `displayName` = `apiValue`, plus
`BaseModelCatalog(active: List<BaseModel>, retired: List<BaseModel>)` with
`statusOf(BaseModel): Active | Retired | Unknown`.**

- Keeping the name `BaseModel` and the `apiValue` property leaves `ModelSearchQuery`,
  `GetModelsUseCase`, `ModelRepositoryImpl`, `SearchPageLoader`, `SavedSearchFilter`,
  `SharedTypeAliases.swift` and both saved-filter sheets unchanged.
- Not a `value class`: Kotlin/Native lists inline classes as "Unsupported" in Swift and
  Objective-C (https://kotlinlang.org/docs/native-objc-interop.html), and iOS reads
  `selectedBaseModels` and calls `onBaseModelToggled(baseModel:)` directly.
- Not a bare `String`: the type keeps call sites readable and gives one place for status lookup.
- Labels are the API strings (`SDXL 1.0` instead of today's `SDXL`). One rule for 112 values,
  and they match what CivitAI shows on model pages and in `ModelVersion.baseModel`.
- Display of non-active selected values (all platforms):
  - Retired (in `BaseModel` but not `ActiveBaseModel`): listed under "Older"; when selected, its chip carries a
    "retired" marker.
  - Unknown (in neither list, e.g. a value CivitAI removed, or one from a newer backup): shown only
    in "Selected" with an "unknown" marker; the user can deselect it.
  - Both are still sent to the API unchanged. The marker describes the value's place in the
    catalog, not whether CivitAI will find models: 22 retired values still return models, an
    unknown value alone returns none and is ignored next to a valid one (see the probe table),
    and a value added to CivitAI after the last refresh is shown as unknown until the next
    refresh. Sending them matches what the user sees selected; dropping them would silently widen
    the search.
- Swift `Set<BaseModel>` equality relies on Kotlin `equals`/`hashCode` being exported as
  `isEqual:`/`hash` for a data class. Inferred, not yet verified in this spike; follow-up F1's
  iOS build and a manual toggle check cover it.

## 4. Filter UI

**Chosen rule, identical on Android, iOS and Desktop:**

1. The filter area's "Base model" row shows the selected values as removable chips and a
   "Choose base models" button (with the count when something is selected). Nothing selected
   reads "Any".
2. The button opens a picker: Android bottom sheet, iOS sheet, Desktop dialog.
3. Picker content, top to bottom:
   - A search field, case-insensitive substring match on the label (`wan` finds all 13 Wan values).
   - "Selected": current selection, including retired and unknown values with their markers.
   - "Current": `ActiveBaseModel` in API order, multi-select with checkmarks.
   - "Older": the rest of `BaseModel` in API order, collapsed by default, expanded automatically
     while a search is active.
4. Selection is local to the picker and applied once when the picker closes ("Done"), with a
   "Clear" action. One `refresh()` per change of selection instead of one per tap.

Rejected:

- Ordering by popularity: needs at least one request per value (112) or data the API does not give.
- Grouping by family: `/enums` has no family field; an in-app family map goes stale the same way
  the enum did.
- Alphabetical: workable, but code-point order puts `Flux 3 Video` before `Flux.1 D` and
  locale-aware sorting differs between Kotlin and Swift; the API order is identical on every
  platform for free, and search makes the order matter less.
- Keeping all chips inline (Android `FlowRow` / iOS horizontal row): 78 chips is several screens
  on a phone and unreachable on the iOS single row.

## 5. Saved filters and backups

**Chosen: no migration; the mapper keeps every non-blank value.**

- `SavedSearchFilterRepositoryImpl.toDomain` becomes `split(",").filter { it.isNotBlank() }
  .map(::BaseModel)`; `toEntity` stays `joinToString(",") { it.apiValue }`.
- The `,` separator is safe for the current data: 0 of the 112 live values contain a comma. A
  value with a comma would be ambiguous both in the stored string and in the model-search cache
  key (`ModelRepositoryImpl.kt:48` joins with `,` too), so the catalog (F2/F3) drops any value
  containing `,` and logs it. Such a value can then never be selected, and no stored string can
  become ambiguous. A storage format change is only needed if CivitAI actually ships one.
- Backups need no change: `BackupDto.selectedBaseModels` and `BackupMappers` already copy the raw
  string. Restoring an old backup with `SVD` shows `SVD` as a retired selection.
- No stored data has been lost so far: the enum has had the same 7 values since it was added
  (#59, `14aa7301`), so the app could only ever save those, and all 7 are still in `BaseModel`.
- Remaining limit (unchanged by this work): an older app version restores a newer backup's raw
  string intact (`BackupMappers.kt:134`), but drops unknown values when it converts the row to the
  domain model, i.e. when the filter is listed or applied. Saving the applied filter again creates
  a new row without those values; the original row stays in the database and in later backups,
  which are written from the database rows (`BackupRepositoryImpl.kt:89`).

## 6. Implementation split

Order is the dependency order. Each issue is one PR and passes the sizing gate in
`.claude/skills/issue/SKILL.md` (Step 3): single outcome, about 5 files and well under 300 lines,
one proof, no open decisions (all settled above), independently mergeable. F1 is the one issue at
6 files: the type change has to compile on iOS in the same PR and the kept values have to be
visible on both platforms, so the Android and both iOS files cannot move out; the whole change is
roughly 60 lines.

| # | Title | Files (expected) | Depends on | Done when |
|---|---|---|---|---|
| F1 | Keep base model values outside the built-in list in saved filters | `core/core-domain/.../domain/model/BaseModel.kt` (enum → `data class`; `DEFAULT_OPTIONS` = today's 7 values; `filterOptions(selected)` = defaults plus any selected value not in them), `feature/feature-search/.../data/repository/SavedSearchFilterRepositoryImpl.kt`, `feature/feature-search/src/commonTest/.../SavedSearchFilterRepositoryImplTest.kt`, `androidApp/.../ui/search/FilterSheetSection.kt` and `iosApp/iosApp/Features/Search/ModelSearchScreen.swift` (render `filterOptions(selected)` so a kept value is visible and can be deselected), `iosApp/iosApp/Features/Search/SearchFilterConstants.swift` (drop the enum-case list) | — | `./gradlew :feature:feature-search:testAndroidHostTest` passes with a test that loads `"Krea 2,SVD"` and gets both values back; the iOS CI build succeeds |
| F2 | Provide a bundled CivitAI base model catalog through a repository | `core/core-domain/.../domain/model/BaseModelCatalog.kt` (new), `core/core-domain/.../domain/repository/BaseModelCatalogRepository.kt` (new), `core/core-data/.../data/repository/BaseModelCatalogRepositoryImpl.kt` (new, snapshot of 2026-10-03), `core/core-data/.../di/CoreDataModule.kt`, `core/core-data/src/commonTest/.../BaseModelCatalogRepositoryImplTest.kt` (new) | F1 | `./gradlew :core:core-data:testAndroidHostTest` passes with tests for active / retired / unknown classification and for dropping a value that contains `,` |
| F3 | Refresh the base model catalog from CivitAI `/api/v1/enums` with a 24 h pinned cache | `core/core-network/.../data/api/CivitAiApi.kt` (`getEnums`), `core/core-network/.../data/api/dto/EnumsResponse.kt` (new), `core/core-data/.../data/repository/BaseModelCatalogRepositoryImpl.kt` (new constructor deps `CivitAiApi`, `LocalCacheDataSource`, `Json`), `core/core-data/.../di/CoreDataModule.kt` (pass them), `core/core-data/src/commonTest/.../BaseModelCatalogRepositoryImplTest.kt` | F2 | `./gradlew :core:core-data:testAndroidHostTest` passes with MockEngine tests for fresh fetch, stale cache on failure, snapshot when nothing is cached |
| F4 | Expose the base model catalog and a one-shot selection apply in the search ViewModel | `feature/feature-search/.../domain/usecase/ObserveBaseModelCatalogUseCase.kt` (new), `feature/feature-search/.../presentation/SearchUseCases.kt` (`SearchFilterUseCases`), `feature/feature-search/.../di/SearchModule.kt`, `feature/feature-search/.../presentation/ModelSearchViewModel.kt` (`baseModelCatalog` in UI state, `onBaseModelsApplied(Set<BaseModel>)` with one refresh), `feature/feature-search/src/commonTest/.../ModelSearchViewModelTest.kt` | F2 | `./gradlew :feature:feature-search:testAndroidHostTest` passes with a test that applying 3 values triggers exactly one model request |
| F5 | Android: choose base models from CivitAI's list with a searchable picker | `androidApp/.../ui/search/FilterSheetSection.kt`, `androidApp/.../ui/search/BaseModelPickerSheet.kt` (new), `androidApp/.../ui/search/ModelSearchScreen.kt`, `androidApp/src/main/res/values/strings.xml` | F4 | `./gradlew :androidApp:assembleDebug detekt` succeeds, and on a device picking `Krea 2` shows Krea 2 models |
| F6 | iOS: choose base models from CivitAI's list with a searchable picker | `iosApp/iosApp/Features/Search/ModelSearchScreen.swift`, `iosApp/iosApp/Features/Search/BaseModelPickerView.swift` (new), `iosApp/iosApp/Features/Search/ModelSearchViewModel.swift`, `iosApp/iosApp/Features/Search/SearchFilterConstants.swift` (remove `baseModelOptions`), `iosApp/iosApp.xcodeproj/project.pbxproj` | F4 | `swiftlint --strict` and the CI `xcodebuild` succeed, and on a simulator picking `Krea 2` shows Krea 2 models |
| F7 | Desktop: add the base model filter with the searchable picker | `desktopApp/.../ui/search/DesktopFilterBar.kt` (use the today-unused callback), `desktopApp/.../ui/search/DesktopBaseModelPicker.kt` (new), `desktopApp/.../ui/search/DesktopSearchScreen.kt` | F4 | `./gradlew :desktopApp:compileKotlinJvm detekt` succeeds, and in `:desktopApp:run` picking `Krea 2` shows Krea 2 models |

Notes on the split:

- F2 has no visible effect on its own (no caller yet). The proof is its unit test, the same as
  data-layer issues such as #1077. F5–F7 need only F4, which needs only F2, so the UI can ship on
  the bundled list even if F3 slips.
- F1 alone already fixes the silent loss of values in saved filters: the 7 chips stay, and a
  saved value outside them appears as an extra selected chip that can be deselected.
- F5–F7 are independent of each other and touch no shared file.
- `.../` above stands for `src/commonMain/kotlin/com/riox432/civitdeck` (or
  `src/main/kotlin/...` / `src/jvmMain/kotlin/...` for the apps).

## 7. `ModelType`

Yes: `ModelType` (23 values from `/enums` vs 14 in the app, missing for example `DoRA`,
`TextEncoder`, `UNet`, `CLIPVision`, `Detection`, `LLM`) should reuse the same catalog fetch and
picker pattern later, as a separate issue, because unknown types are mapped to `Other` in
`DtoMapper.kt:187` and the enum is also used outside the filter (TensorArt and HuggingFace DTO
mappers, `FavoriteRepositoryImpl`, `CollectionRepositoryImpl`, `CreatorFollowRepositoryImpl`,
`GetRecommendationsUseCase`).

---

## Points for owner review

None of these blocks the follow-up issues; each has a recommendation already applied above.

1. **Show retired values at all?** Recommended: yes, in a collapsed "Older" group (22 of 34 still
   find models). Alternative: `ActiveBaseModel` only, simpler picker, those models unreachable.
2. **Labels.** Recommended: use the API strings (`SDXL 1.0`). Alternative: keep a small alias map
   for the few values that have a shorter common name; it needs upkeep.
3. **Desktop.** Recommended: add the filter (F7) since Desktop has none today. Alternative: leave
   Desktop without a base model filter and drop F7.
