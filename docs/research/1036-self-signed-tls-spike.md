# SPIKE #1036 — How "Accept self-signed certificates" applies to all ComfyUI traffic

Status: research only (no implementation). Decision note for owner review on the PR.
Follow-up implementation issues are filed only after this document merges.
Code references are to `origin/master` at `2d9bba9a` (2026-09-26).

## Decision

| # | Question | Chosen option | One-line reason |
|---|---|---|---|
| 1 | How the client is chosen | **Look up the connection on every call.** The URL and the trust setting come from the same database row. | Repositories already read the active row before every call. Rebuilding singletons still needs a holder object, and closing an old client cancels polling and WebSocket calls that are still running. |
| 2 | Which call sites follow the flag | **Every request that goes to the ComfyUI host** (11 groups, see §2). Left out: ntfy, Civitai Link, LAN scan, Desktop, dataset export, dead code. | The flag belongs to the server connection, so anything that talks to that server must obey it. Anything that talks to another host must not. |
| 3 | iOS | **Add trust through Ktor Darwin `handleChallenge` plus a delegate on the image `URLSession`.** Keep the toggle on iOS. | Ktor 3.4.3 has this API and it also covers WebSocket tasks. App Transport Security is already off (`NSAllowsArbitraryLoads`). Hiding the toggle would leave iOS users only the certificate-profile workaround. |
| 4 | What "accept" means | **Trust on first use (TOFU):** after the user confirms it, store the SHA-256 fingerprint of the server's certificate and accept only that exact certificate. | Trusting any certificate on every ComfyUI request lets any attacker on the network path intercept traffic without the user noticing. A pin blocks that after the first confirmation. |
| 5 | Split | **16 follow-up issues** (8 shared, 4 Android, 4 iOS), each ≤5 production files. See §5. | TOFU needs storage, trust inside each HTTP engine, per-call selection, and a confirmation screen. Each of these is a separate layer that can be reviewed and merged on its own. |

The trust decision (§4) is a security choice for the owner, so §4 lists the trade-offs, not
only the mechanism. If the owner picks "trust any certificate" instead, §5 shows which issues
are dropped.

---

## Current state (verified)

- The flag is stored per connection:
  `core/core-database/.../entity/ComfyUIConnectionEntity.kt:17` (added by `Migration43to44.kt:13`),
  domain `core/core-domain/.../model/ComfyUIConnection.kt:12`.
- Only the onboarding tester reads it:
  `feature/feature-comfyui/.../data/repository/ComfyUIConnectionTesterImpl.kt:32-34`. For each
  test it picks `named("comfyui-selfsigned")` or `named("comfyui")` and builds a temporary
  `ComfyUIApi` (wired at `ComfyUIModule.kt:106-112`).
- The "self-signed" client trusts **every** certificate. Android and JVM install an
  `X509TrustManager` whose checks are empty (`ComfyUIHttpClientFactory.android.kt:34-56`,
  `.jvm.kt:31-50`), and OkHttp also accepts every hostname (`.android.kt:55`). The iOS
  `actual` ignores the flag (`ComfyUIHttpClientFactory.ios.kt:17-24`).
- `ComfyUIApi` and `ComfyUIWebSocketApi` are singletons built on the normal client
  (`core/core-network/.../di/NetworkModule.kt:52-55`). `ComfyUIApi` stores its base URL in a
  mutable field (`ComfyUIApi.kt:32-47`). Before each call, a repository reads the active row and
  sets that field: `ensureApiConfigured()` at `ComfyUIGenerationRepositoryImpl.kt:131-136`,
  `ComfyUIQueueRepositoryImpl.kt:68-73` and `ComfyUIHistoryRepositoryImpl.kt:115-120`.
  `ComfyUIConnectionRepositoryImpl.testConnection` sets it from its argument (`:51-60`).
- The settings "Test" button (`ComfyUIConnectionRepositoryImpl.testConnection`) and the
  plugin's `connect()` (`ComfyUIWorkflowPlugin.kt:63-71`, which calls the same method) run on
  that singleton, so they ignore the flag too. Only onboarding honours it.
- Latent bug: `ComfyHubRepositoryImpl.importToServer` (`:38-42`) calls `submitPrompt` without
  setting the base URL, so it posts to whichever server was set last. It posts to `""` if no
  base URL has been set yet.
- Result: a server that passes the onboarding test fails every real call with a TLS error on
  Android. On iOS the test fails as well, because the Darwin client never trusts the certificate.

---

## 1. How the client is chosen

**Chosen:** look up the connection on every call. **Reason:** the URL and the trust setting
then come from the same database row, which removes the shared mutable base URL. The
repositories already read the active row before each call.

| Option | Verdict | Why |
|---|---|---|
| A. Per call: read the connection row, return a temporary `ComfyUIApi` bound to that row's URL **and** an HttpClient for that row's trust (the tester's pattern, `ComfyUIConnectionTesterImpl.kt:21-34`) | **Chosen** | The URL and trust cannot disagree. Races on the shared `_baseUrl` disappear (for example, queue polling and a history load setting it at the same time while the user switches connections). The ComfyHub bug above is fixed as a side effect. HttpClients stay long-lived (cached per trust setting), so connection pools are reused. Only the thin `ComfyUIApi` wrapper is created per call. |
| B. Rebuild the singletons when the active connection changes | Rejected | Repositories get `ComfyUIApi` through their constructors (`ComfyUIModule.kt:119-121`), so they would keep the old instance unless a holder object is added. That holder is option A in another form. Closing the old client cancels in-flight queue polling (`ComfyUIQueueRepositoryImpl.kt:25-43`) and WebSocket progress. It also adds a timing gap between "connection switched" and "client rebuilt". |

Design of option A:

- `core/core-network/.../api/comfyui/ComfyUIHttpClientFactory.kt` gains a trust type:
  `ComfyUIServerTrust.System`, or `ComfyUIServerTrust.PinnedLeaf(expectedSha256: String?)`
  (§4). It also gains a factory `createComfyUIHttpClient(trust, timeoutConfig)`, implemented
  per platform in `ComfyUIHttpClientFactory.{android,jvm,ios}.kt`.
- `feature/feature-comfyui/.../data/ComfyUIApiProvider.kt` (new) is where every
  connection-aware call site gets its client:
  - `suspend fun forActive()` reads `dao.getActive()` once. It throws the same
    `DomainException.ConnectionException("No active ComfyUI connection")` as
    `ensureApiConfigured()` today. It returns a `ComfyUIApi` and a `ComfyUIWebSocketApi` bound
    to that row.
  - `fun forConnection(connection)` does the same for a connection that is passed in
    (settings "Test", foreground WebSocket progress).
  - `suspend fun forUrl(url)` is for call sites that only have a URL. It matches the URL's
    `scheme://host:port` against the stored connections and falls back to `System` trust.
  - It keeps a cache of pinned HttpClients keyed by fingerprint. The shared `named("comfyui")`
    client is used whenever trust is `System`.
- A connection uses `PinnedLeaf` only when `useHttps && acceptSelfSigned`. All other
  connections use `System`, which is today's behaviour for real calls.
- The `ComfyUIApi` / `ComfyUIWebSocketApi` singletons (`NetworkModule.kt:54-55`) are removed
  after the last consumer migrates. `named("comfyui")` stays, because Civitai Link
  (`NetworkModule.kt:61-62`), the LAN scan (`ComfyUIModule.kt:115`) and ntfy
  (`ComfyUIModule.kt:196-202`) still use it.
- Image loaders only receive a URL and cannot suspend when choosing a fetcher (Coil's
  `Fetcher.Factory.create` is not a suspend function). They use a snapshot map from
  host:port to fingerprint. The snapshot is collected from `ComfyUIConnectionDao.observeAll()`
  and started when Koin starts. If the snapshot is not loaded yet, the image fails the same way
  it does today. The next request succeeds.
- Two saved connections can share a host:port (for example, one with the toggle on and one
  with it off). `forUrl` and the snapshot then use the active row if it matches. Otherwise they
  use a pin only when all matching rows that have one store the same fingerprint. With
  different pins and no active match, the lookup falls back to `System` trust, which fails
  closed against a self-signed server instead of guessing which pin applies.

Not changed by this decision: generation polling still follows the connection that is
currently active (`ComfyUIGenerationRepositoryImpl.kt:54-73`). If the user switches connections
in the middle of a generation, polling moves to the new server, as it does today. Tying a job
to the connection it was submitted on is a separate issue.

---

## 2. Which call sites follow the flag

**Chosen:** every request whose target is the ComfyUI server of a connection follows that
connection's trust setting. Traffic to any other host keeps system trust. **Reason:** the flag
describes one server's certificate. Using it for any other host would weaken trust where the
user never asked for it.

| Call site | Where | Follows? | How / why |
|---|---|---|---|
| Generation HTTP (`/object_info`, `/prompt`, `/history/{id}`, `/interrupt`, `/upload/image`, `/view` URL building) | `ComfyUIGenerationRepositoryImpl.kt:32-136` | Yes | `provider.forActive()` |
| Foreground WebSocket progress | `ComfyUIGenerationRepositoryImpl.kt:81-104` → `ComfyUIWebSocketApi.kt:89-107` | Yes | Uses `provider.forConnection(connection)` with the connection the job was submitted on (`GenerationExecutionDelegate.kt:106`). The Ktor WebSocket runs on the same engine client, so the pin applies to `wss://`. |
| Queue polling and cancel | `ComfyUIQueueRepositoryImpl.kt:25-73` | Yes | `provider.forActive()` |
| History and `/view` URLs | `ComfyUIHistoryRepositoryImpl.kt:23-43,115-120` | Yes | `provider.forActive()` |
| ComfyHub "import to server" | `ComfyHubRepositoryImpl.kt:38-42` | Yes | `provider.forActive()` (also fixes the missing base URL) |
| Settings "Test" and system stats | `ComfyUIConnectionRepositoryImpl.kt:51-60`, `FetchSystemStatsUseCase.kt:16`, `ComfyUISettingsViewModel.kt:127-133`; also `ComfyUIWorkflowPlugin.kt:67` | Yes | `provider.forConnection(connection)` |
| Onboarding tester | `ComfyUIConnectionTesterImpl.kt:31-52` | Yes | Builds a one-off pinned client and records the certificate the server presents (§4) |
| Save generated image | `SaveGeneratedImageUseCase.kt:18-21` (bound to `named("comfyui")` at `ComfyUIModule.kt:153`) | Yes | `provider.forUrl(url)` |
| Android background monitor WebSocket | `androidApp/.../service/GenerationMonitorService.kt:45,113-127` (receives only `baseUrl`/`wsScheme` extras from `BackgroundMonitorStarterImpl.kt`) | Yes | `provider.forUrl(baseUrl)`. The `BackgroundMonitorStarter` interface stays unchanged. |
| Android Coil `/view` images | `CivitDeckApplication.kt:108-122`, which loads `CivitAsyncImage` in `ComfyUIResultSection.kt:162`, `ComfyUIHistoryScreen.kt:249`, `ComfyUIOutputDetailScreen.kt:199`, `MaskEditorScreen.kt:152` and the image viewer | Yes | A custom Coil `Fetcher.Factory<Uri>` added before the default one. For a pinned host:port it uses `OkHttpNetworkFetcherFactory(callFactory = { pinnedClient })`. For every other URI it returns `null`, so CivitAI images keep the default fetcher. |
| iOS images and "save to Photos" | `ImageURLSession.shared` (`CachedAsyncImage.swift:102-116`), used by `ComfyUIHistoryView.swift:87`, `ComfyUIGenerationView.swift:398`, `ComfyUIOutputDetailView.swift:132,345`, `MaskEditorView.swift:53`, and by the zoom viewer that the output detail opens (`ComfyUIOutputDetailView.swift:277` → `ZoomableImageView.swift:179`; `ImageViewerScreen.swift:160` uses the same session) | Yes | Add a session delegate that forwards server-trust challenges for pinned host:port pairs to the shared Kotlin evaluator (§3) |
| ntfy subscription | `NtfySubscriptionService` (`ComfyUIModule.kt:196-202`) | **No** | Out of scope for this issue. It talks to the ntfy server, not to ComfyUI. |
| Civitai Link | `CivitaiLinkApi` on `named("comfyui")` (`NetworkModule.kt:61-62`) | **No** | It talks to Civitai's Link service, not to the ComfyUI host. |
| LAN scan | `ServerDiscoveryRepositoryImpl.kt:59-60` | **No** | It probes `http://ip:8188` over plain HTTP, so there is no TLS. |
| Desktop | `DesktopComfyUIConnectionSection.kt:97` always passes `acceptSelfSigned = false`. Desktop shows only a count of generated images (`DesktopComfyUIGenerationSection.kt:72`). | **No** (not planned) | Desktop has no toggle. The shared JVM `actual` gets pinned trust for free. However, Ktor CIO calls `verifyHostnameInCertificate(config.serverName, …)` whenever `serverName` is set (ktor-network-tls `TLSClientHandshake`). A Desktop toggle would therefore need extra work for certificates whose names do not match. |
| Dataset images copied from ComfyUI history | URL stored by `ComfyUIHistoryViewModel.kt:120-126`, downloaded for export by `ExportRepositoryImpl.kt:53` with the default (CivitAI) client | **No** (export) | The stored `/view` URL lives longer than the connection. The Coil and iOS image paths above still display it while the connection exists. Export should copy the image bytes when the image is added, which is a dataset concern. |
| Legacy `ComfyUIRepositoryImpl` | `feature/feature-comfyui/.../ComfyUIRepositoryImpl.kt` | **No** | Not registered in Koin; only tests reference it. |

---

## 3. iOS

**Chosen:** add trust in the Darwin engine with `handleChallenge`, and use the same evaluator
in a delegate on the image `URLSession`. Keep the iOS toggle. **Reason:** the API exists in
the Ktor version the project uses. It covers HTTP and WebSocket tasks. App Transport Security
does not block it here, because `Info.plist:49-54` sets `NSAllowsArbitraryLoads = true`.

Verified:

- Ktor 3.4.3 `DarwinClientEngineConfig.handleChallenge(block: ChallengeHandler)`, where
  `ChallengeHandler = (NSURLSession, NSURLSessionTask, NSURLAuthenticationChallenge,
  completionHandler) -> Unit`. `KtorNSURLSessionDelegate` calls it from
  `URLSession:task:didReceiveChallenge:completionHandler:` for every task type. That includes
  `NSURLSessionWebSocketTask`, so `wss://` is covered. With no handler set, the delegate uses
  `PerformDefaultHandling`.
- Apple, "Performing manual server trust authentication": after your own check, accept with
  `completionHandler(.useCredential, URLCredential(trust: serverTrust))`. For a domain that App
  Transport Security protects, you "cannot loosen server trust requirements".
- The comment in `ComfyUIHttpClientFactory.ios.kt:17-24` says a bypass is "complex to
  implement via K/N cinterop". That is out of date. Ktor ships a Kotlin/Native challenge
  handler (`io.ktor.client.engine.darwin.certificates.CertificatePinner`) that does the same
  Security-framework calls.

Mechanism for the implementation issue:

1. Put one evaluator in `core-network` `iosMain`. It handles only
   `NSURLAuthenticationMethodServerTrust` challenges. It takes the leaf certificate from the
   `SecTrust`, applies SHA-256 to its DER bytes (`SecCertificateCopyData` + CommonCrypto
   `CC_SHA256` from `platform.CoreCrypto`), compares the result with the pin, and records the
   fingerprint the server presented.
2. On a match, answer `NSURLSessionAuthChallengeUseCredential` with
   `NSURLCredential.credentialForTrust(serverTrust)`.
3. On a mismatch, or when no pin is stored, answer
   `NSURLSessionAuthChallengeCancelAuthenticationChallenge`. Never answer
   `PerformDefaultHandling` for a pinned connection. The default handling would accept a
   different certificate that a public CA signed, which breaks the pin.
4. Ktor's own `CertificatePinner` is **not** used as-is. It pins public-key (SPKI) hashes, which
   do not match the fingerprint users see (§4). On success it also passes
   `challenge.proposedCredential` instead of a credential built from the trust object. Whether
   that accepts a chain the system does not trust is unverified.
5. `ImageURLSession` in Swift gets a `URLSessionDelegate`. For server-trust challenges on a
   host:port that has a pin, it calls the Kotlin evaluator (exposed through
   `KoinHelperComfyUI.kt`). For all other challenges it uses default handling, so CivitAI
   images keep system trust.

Constraint to record: this works only while App Transport Security does not apply to ComfyUI
hosts. If `NSAllowsArbitraryLoads` is ever tightened, ComfyUI hosts need an explicit exception.

Rejected: hiding the toggle on iOS. Today the toggle already does nothing on iOS, which is the
current bug. The only way around it is installing a configuration profile and enabling full
trust under Settings › General › About › Certificate Trust Settings. That is heavy friction for
the core use case (controlling a home ComfyUI from the phone).

---

## 4. What "accept" means

**Chosen:** trust on first use. The pin is the SHA-256 of the leaf certificate's DER bytes,
confirmed by the user and stored on the connection. **Reason:** it stops silent interception
after the first connection. The value is the same one
`openssl x509 -noout -fingerprint -sha256` prints on the server, so the user can compare the
two by eye.

| Option | What it protects against | Cost | Verdict |
|---|---|---|---|
| Trust any certificate for that connection (today's tester behaviour, extended to all traffic) | Passive sniffing only. Anyone on the network path (shared Wi-Fi, a remote setup through port forwarding) can present their own certificate and read or change prompts and images without the user noticing. | Smallest: no storage and no new screens | Rejected |
| **TOFU pin on the leaf certificate's SHA-256** | Active interception after the first confirmation | One new column, one confirmation screen and one review entry per platform | **Chosen** |
| TOFU pin on the public-key (SPKI) hash | Same, and the pin survives re-issuing a certificate with the same key | The value does not match what `openssl x509 -fingerprint` or a browser shows, so users cannot check it. Self-signed certificates are usually regenerated with a new key anyway. | Rejected |
| No in-app trust; rely on the OS trust store | Full certificate-chain validation | Apps targeting API 24+ ignore CAs that the user installed unless a network security config opts in. iOS needs a profile install plus a full-trust switch. | Rejected as the main path. It stays the answer for private CAs that rotate often (below). |

Behaviour:

- **Capture.** When `acceptSelfSigned && useHttps` and no pin is stored, the trust manager
  (Android/JVM) or the challenge handler (iOS) **rejects** the handshake and records the leaf
  fingerprint the server presented. No request body is ever sent over a certificate that has
  not been confirmed. The onboarding test reports a new failure cause, `CertificateUnconfirmed`,
  with that fingerprint. The same component therefore acts as both the capture probe and the
  enforcer, and no separate probe is needed.
- **Confirm.** Onboarding shows the fingerprint as colon-separated uppercase hex (the `openssl`
  format), together with a hint to compare it on the server. When the user taps "Trust", the
  test runs again with the pin and the connection is saved with it. The fingerprint is stored as
  64 lowercase hex characters.
- **Enforce.** A pinned connection accepts only an exact match on the leaf certificate. For
  that connection only, hostname and validity-date checks are skipped. The pin is tied to this
  connection's host:port, so an attacker would need the pinned certificate's private key. An
  expired but unchanged self-signed certificate keeps working, because it is still the same
  identity. A certificate that a public CA signed but that is not the pinned one is **rejected**.
  A user who moves the server to a real certificate turns the toggle off.
- **Change.** A mismatch is reported as `CertificateChanged`, carrying the presented
  fingerprint. It never falls back silently. The user reviews and re-confirms through the same
  onboarding step, which ComfyUI settings can open for a saved connection.
- **Edit.** When a saved connection is edited and its hostname, port and `useHttps` stay the
  same, the pin is kept. Changing any of them clears the pin, because the pin identifies one
  endpoint.
- **Image cache.** Coil (memory and disk) and the iOS `URLCache` behind `ImageURLSession`
  key images by URL only. When a pin is confirmed, changed or cleared, the cached ComfyUI
  images for that host:port are evicted, so an image fetched under the old trust is not shown
  as if it came from the newly confirmed server. This belongs to A3 and I3.
- **Existing rows.** Rows with `acceptSelfSigned = true` and no pin get `CertificateUnconfirmed`
  on their first call. This is not a regression: those calls already fail with a TLS error today.
- **Backup.** The pin is not added to the backup format. `BackupMappers.kt:57-63,143-149`
  already drops `useHttps` and `acceptSelfSigned`, so after a restore the user confirms the
  certificate again.

Trade-offs for the owner:

- If an attacker is present on the very first connection, the user sees the attacker's
  fingerprint. They only notice if they compare it with the server's value. The UI makes that
  comparison easy but cannot require it.
- Servers that present a different leaf certificate after each renewal ask for re-confirmation
  every time. For example, an internal certificate authority that issues short-lived
  certificates does this. Users with such a setup should install their root certificate on the
  device or put ComfyUI behind a tunnel that serves a publicly trusted certificate. A publicly
  trusted certificate needs no toggle at all.
- The pin is public information (a fingerprint, not a secret), so it is stored in plain text in
  Room.

---

## 5. Split into implementation issues

Rules applied: ≤5 production files per issue (tests and the generated Room schema JSON are not
counted), one outcome, one proof command, Android and iOS in separate issues, and each issue
builds and passes tests on its own (split moves 3 "Architecture layer" and 2 "Vertical slice"
in `.claude/skills/issue/splitting.md`).

**File these only after this spike merges. Implement them only after the open issues that touch
the same files have merged:** #1057 and #1060 (tester and `ConnectionFailureCause`), #1032
(`ComfyUIApi` handling of non-2xx responses), #1021 and #1022 (generation repository and
WebSocket API). Room migration numbers are assigned at implementation time, because other
branches also add migrations.

Proof commands come from the CI config (`.github/workflows/ci.yml`); each must print
`BUILD SUCCESSFUL`, or `** BUILD SUCCEEDED **` for `xcodebuild`. "iOS build" means the CI
command:
`xcodebuild build -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' ARCHS=arm64 ONLY_ACTIVE_ARCH=NO CODE_SIGNING_REQUIRED=NO CODE_SIGNING_ALLOWED=YES EXPANDED_CODE_SIGN_IDENTITY=""`.

### Shared (KMP)

| # | Title | Files (expected) | Done when | Depends on |
|---|---|---|---|---|
| S1 | Store a confirmed TLS certificate fingerprint on each ComfyUI connection | `ComfyUIConnectionEntity.kt` (new nullable `tlsCertSha256`), `Migration{N}to{N+1}.kt` (new; `N` is the database version on master when the issue starts), `CivitDeckDatabase.kt` (version + `addMigrations`), `ComfyUIConnection.kt` (domain field), `ComfyUIConnectionRepositoryImpl.kt` (mappers; keep the pin when hostname/port/`useHttps` are unchanged, clear it otherwise) | `./gradlew :feature:feature-comfyui:jvmTest :core:core-database:jvmTest detekt` → `BUILD SUCCESSFUL`. Repository tests prove the fingerprint round-trips, survives a rename, and is cleared by a port change. | — |
| S2 | Accept only the pinned certificate in the Android/JVM ComfyUI HTTP client | `ComfyUIHttpClientFactory.kt` (new `ComfyUIServerTrust` + factory next to the existing trust-all function), `.android.kt` and `.jvm.kt` (trust manager that pins the leaf certificate and records the presented fingerprint; OkHttp accepts any hostname for pinned clients only), `.ios.kt` (new `actual` that keeps system trust; see I1) | `./gradlew :core:core-network:testAndroidHostTest :core:core-network:compileKotlinIosSimulatorArm64 :desktopApp:compileKotlinJvm detekt` → `BUILD SUCCESSFUL`. A test of the Android trust manager with a fixed PEM test certificate shows: match is accepted; mismatch and missing pin are rejected, and the presented fingerprint is recorded. The test goes in a new `androidHostTest` source set of core-network (precedent: `core/core-ml/src/androidHostTest`), because `commonTest` cannot build an `X509Certificate`. core-network has no `jvmTest`, so the JVM `actual` is only compile-checked through `:desktopApp:compileKotlinJvm`. | S1 |
| S3 | Capture the server certificate fingerprint in the onboarding test and save it once trusted | `ConnectionTestResult.kt` (`CertificateUnconfirmed` and `CertificateChanged` causes + presented fingerprint on `Failure`), `ComfyUIConnectionTesterImpl.kt` (one-off pinned client per test), `ConnectionOnboardingViewModel.kt` (`onTrustCertificate()`, which tests again and saves with the pin; `onReviewCertificate(saved)`, which re-tests a saved connection), `ComfyUIModule.kt` (tester wiring), `ConnectionOnboardingScreen.kt` (entries in the exhaustive `when` only, reusing the existing TLS string; without them the new causes break the Android build, so this issue could not merge on its own) | `./gradlew :feature:feature-comfyui:jvmTest :core:core-domain:testAndroidHostTest detekt :androidApp:assembleDebug` → `BUILD SUCCESSFUL`. VM tests: unconfirmed → trust → saved with the pin; changed → `CertificateChanged` with the new fingerprint. | S1, S2 |
| S4 | Resolve the ComfyUI URL and trust per call for generation and image saving | `ComfyUIApiProvider.kt` (new: `forActive`, `forConnection`, `forUrl`, pinned-client cache, host:port snapshot with the collision rule from §1), `ComfyUIModule.kt`, `ComfyUIGenerationRepositoryImpl.kt` (HTTP + WebSocket; remove `ensureApiConfigured`), `SaveGeneratedImageUseCase.kt` | `./gradlew :feature:feature-comfyui:jvmTest detekt` → `BUILD SUCCESSFUL`. Provider tests: the active pinned row yields a pinned client and that row's URL; a non-self-signed row yields the shared client; no active row → `ConnectionException`; the host:port snapshot returns the pin for a pinned host, nothing for a CivitAI host, and nothing when two rows on the same host:port hold different pins and neither is active. | S2 |
| S5 | Resolve the ComfyUI URL and trust per call for queue, history and ComfyHub import | `ComfyUIQueueRepositoryImpl.kt`, `ComfyUIHistoryRepositoryImpl.kt`, `ComfyHubRepositoryImpl.kt` (constructor type changes; the positional `get()` calls in `ComfyUIModule.kt` resolve without edits) | `./gradlew :feature:feature-comfyui:jvmTest detekt` → `BUILD SUCCESSFUL`. Includes a ComfyHub test showing the import posts to the active row's URL. | S4 |
| S6 | Resolve trust per call for the settings connection test and system stats | `ComfyUIConnectionRepositoryImpl.kt` (`testConnection` uses `provider.forConnection`), `FetchSystemStatsUseCase.kt`, `ComfyUIConnectionTesterImpl.kt` (call site), `ComfyUIModule.kt` | `./gradlew :feature:feature-comfyui:jvmTest detekt :androidApp:assembleDebug` → `BUILD SUCCESSFUL`. Repository test: testing a pinned connection from settings uses the pinned client and that connection's URL. | S4, S5 |
| S7 | Remove the `ComfyUIApi`/`ComfyUIWebSocketApi` singletons | `NetworkModule.kt` (drop the two `single` registrations) | `./gradlew :feature:feature-comfyui:jvmTest :shared:testAndroidHostTest detekt :androidApp:assembleDebug` → `BUILD SUCCESSFUL`, and `git grep -n "single { ComfyUIApi\|single { ComfyUIWebSocketApi\|ComfyUIWebSocketApi by inject"` prints nothing. The project has no Koin module verification test, so a leftover positional `get()` would only fail at runtime. Manual: the app starts and ComfyUI settings, generation and history open on Android and iOS. | S6, A2 |
| S8 | Delete the trust-all ComfyUI HTTP client | `ComfyUIHttpClientFactory.kt`, `.android.kt`, `.jvm.kt`, `.ios.kt` (remove the Boolean `expect`/`actual` and `createComfyUIHttpClientWithSelfSignedTls`), `NetworkModule.kt` (remove `named("comfyui-selfsigned")`) | `./gradlew :core:core-network:testAndroidHostTest :core:core-network:compileKotlinIosSimulatorArm64 :desktopApp:compileKotlinJvm detekt :androidApp:assembleDebug` → `BUILD SUCCESSFUL`, and `git grep -n "TrustAllX509TrustManager\|comfyui-selfsigned"` prints nothing | S3, S6 |

### Android

| # | Title | Files (expected) | Done when | Depends on |
|---|---|---|---|---|
| A1 | Show the server certificate fingerprint and a "Trust this certificate" action in Android onboarding | `ConnectionOnboardingScreen.kt`, `res/values/strings.xml` | `./gradlew :androidApp:assembleDebug detekt` → `BUILD SUCCESSFUL`. Manual: onboarding against an HTTPS server with a self-signed certificate shows the same fingerprint as `openssl x509 -noout -fingerprint -sha256`; "Trust" saves the connection. | S3 |
| A2 | Use the connection's certificate pin in the Android background generation monitor | `GenerationMonitorService.kt` (`provider.forUrl(baseUrl)` instead of the injected singleton) | `./gradlew :androidApp:assembleDebug detekt` → `BUILD SUCCESSFUL`. Manual: with the app in the background, the progress notification advances against a pinned server. | S4 |
| A3 | Load ComfyUI images from pinned servers in Coil on Android | `CivitDeckApplication.kt` (register the factory in `components`), `ComfyUIPinnedFetcherFactory.kt` (new, `androidApp`; a thin wrapper that asks the S4 host:port snapshot and returns `null` when it has no pin), `ComfyUIHttpClientFactory.android.kt` (expose the pinned `OkHttpClient` builder), `ComfyUIImageCacheEvictor.kt` (new, `androidApp`; evicts Coil memory and disk entries for a host:port when its pin changes, §4) | `./gradlew :androidApp:assembleDebug :core:core-network:testAndroidHostTest detekt` → `BUILD SUCCESSFUL`. The host:port → pin decision is unit-tested in S4; `androidApp` has no plain `test` source set, so the factory itself is checked by the build and by hand. Manual: history thumbnails render from a pinned server; CivitAI images still load; after re-confirming a changed certificate, thumbnails are fetched again. | S4 |
| A4 | Open the certificate review from Android ComfyUI settings for the active connection | `ComfyUIStatusSection.kt` (a "Review certificate" button next to Test on pinned connections), `ComfyUISettingsScreen.kt` (pass the new callback into `StatusSection`), `NavRoutes.kt` (`ConnectionOnboardingRoute` gains an optional saved-connection id), `CreateNavEntries.kt` (push the route with the id and call `onReviewCertificate`), `res/values/strings.xml` | `./gradlew :androidApp:assembleDebug detekt` → `BUILD SUCCESSFUL`. Manual: after the server certificate is regenerated, settings → "Review certificate" shows the new fingerprint, and trusting it restores generation. | A1 |

### iOS

| # | Title | Files (expected) | Done when | Depends on |
|---|---|---|---|---|
| I1 | Accept only the pinned certificate in the iOS Darwin ComfyUI client | `ComfyUIHttpClientFactory.ios.kt` (`handleChallenge`), `ComfyUIServerTrustEvaluator.kt` (new, `core-network` `iosMain`; public so Swift can call it) | `./gradlew :core:core-network:compileKotlinIosSimulatorArm64 detekt` → `BUILD SUCCESSFUL` and iOS build → `** BUILD SUCCEEDED **`. Manual on the simulator: generation and WebSocket progress work against a pinned server; a different certificate is rejected. | S2, S4 |
| I2 | Show the server certificate fingerprint and a "Trust this certificate" action in iOS onboarding | `ConnectionOnboardingView.swift`, `ConnectionOnboardingViewModel.swift`, `Localizable.xcstrings` | `cd iosApp && swiftlint --strict` exits 0 and iOS build → `** BUILD SUCCEEDED **`. Manual: same fingerprint check as A1. | S3, I1 |
| I3 | Load ComfyUI images and "save to Photos" from pinned servers on iOS | `CachedAsyncImage.swift` (delegate on `ImageURLSession`; evict `URLCache` entries for a host:port when its pin changes, §4), `KoinHelperComfyUI.kt` (expose the evaluator and the host:port lookup) | `cd iosApp && swiftlint --strict` exits 0 and iOS build → `** BUILD SUCCEEDED **`. Manual: history thumbnails, the pinch-zoom viewer opened from output detail, and output save work from a pinned server; CivitAI images still load. | I1, S4 |
| I4 | Open the certificate review from iOS ComfyUI settings for the active connection | `ComfyUISettingsView.swift`, `ComfyUISettingsViewModel.swift`, `Localizable.xcstrings` | `cd iosApp && swiftlint --strict` exits 0 and iOS build → `** BUILD SUCCEEDED **`. Manual: same as A4. | I2 |

Implementation order: S1 → S2 → S3 → A1 → S4 → S5 → S6 → A2 → S7 → A3 → A4 → I1 → I2 →
I3 → I4 → S8. Android users can confirm a certificate after A1 and generate after S4. iOS users
can after I1 and I2.

Sizing gate (`.claude/skills/issue/SKILL.md` Step 3), checked for every row:

1. Single outcome: each title names one behaviour. S7 and S8 are the two cleanup steps
   (singletons, then the trust-all client), each with a `git grep` proof.
2. Bounded: at most 5 production files each, all named above.
3. Single proof: each has one command, taken from CI, with its success line.
4. No open decisions: all decisions are made in §1–§4 of this document.
5. Independently mergeable: each builds on its own. S1, S2, S4, S5, S6 and S7 are
   architecture-layer steps whose effect is shown by tests or by the build. Every other row
   has a visible effect on its platform.

If the owner picks "trust any certificate" in §4, drop S1, S3, A1, A4, I2, I4 and S8 (the
trust-all client stays), and drop the pin logic in S2 and I1. The remaining issues (S2 as a
trust type only, per-call selection S4–S7, A2, A3, I1, I3) still apply.

---

## Review status

Codex cross-review was attempted on 2026-09-26 and could not run (usage limit reached).
Following the fallback in `.claude/rules/behavior.md`, the design was checked against the
official sources below and against existing codebase patterns (the tester's per-call
`ComfyUIApi`). The owner review on this PR is the remaining gate.

## Sources

- Ktor 3.4.3 `DarwinClientEngineConfig.kt` (`handleChallenge`, `ChallengeHandler`): https://github.com/ktorio/ktor/blob/3.4.3/ktor-client/ktor-client-darwin/darwin/src/io/ktor/client/engine/darwin/DarwinClientEngineConfig.kt
- Ktor 3.4.3 `KtorNSURLSessionDelegate.kt` (challenge routing for all task types): https://github.com/ktorio/ktor/blob/3.4.3/ktor-client/ktor-client-darwin/darwin/src/io/ktor/client/engine/darwin/KtorNSURLSessionDelegate.kt
- Ktor 3.4.3 Darwin `CertificatePinner.kt` (SPKI pins, `validateTrust`, `proposedCredential`): https://github.com/ktorio/ktor/blob/3.4.3/ktor-client/ktor-client-darwin/darwin/src/io/ktor/client/engine/darwin/certificates/CertificatePinner.kt
- Ktor 3.4.3 CIO TLS hostname check (`TLSClientHandshake.kt`): https://github.com/ktorio/ktor/blob/3.4.3/ktor-network/ktor-network-tls/jvm/src/io/ktor/network/tls/TLSClientHandshake.kt
- Apple, Performing manual server trust authentication: https://developer.apple.com/documentation/foundation/performing-manual-server-trust-authentication
- Coil 3 network (`OkHttpNetworkFetcherFactory(callFactory = …)`): https://coil-kt.github.io/coil/network/
- Coil 3 `Fetcher.Factory.create` (`null` = let another factory try): https://coil-kt.github.io/coil/api/coil-core/coil3.fetch/-fetcher/-factory/
- Coil 3 changelog (components added by hand take precedence over service-loaded ones): https://coil-kt.github.io/coil/changelog/
- Android network security configuration (the default configuration for apps targeting API 24+ trusts only system CAs): https://developer.android.com/privacy-and-security/security-config
