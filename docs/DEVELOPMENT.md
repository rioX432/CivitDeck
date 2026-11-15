# Development Guide

Start here to build CivitDeck or check a contribution. Commands run from the repository root unless stated otherwise. On Windows, use `gradlew.bat` in place of `./gradlew`.

## Toolchain

- **Git and JDK 17** on every development platform. CI uses Temurin 17. Check `java -version` and `./gradlew --version`; make sure your IDE's Gradle JDK matches your shell.
- **Gradle wrapper:** use the checked-in wrapper, currently Gradle 9.3.1, instead of installing a separate Gradle version. See [wrapper configuration](../gradle/wrapper/gradle-wrapper.properties).
- **Android:** install Android SDK Platform 36 and an Android Studio version compatible with the repository's Android Gradle Plugin (currently 9.1.1). Check the [official compatibility table](https://developer.android.com/studio/releases#android_gradle_plugin_and_android_studio_compatibility) before choosing an IDE version. Set the SDK location through Android Studio or a local, uncommitted `local.properties` file. A device/emulator running Android 10 (API 29) or newer is needed to launch the app.
- **iOS:** use an Apple Silicon Mac and the Xcode 26.x / Swift 6.2 toolchain used by CI, plus SwiftLint. CI selects the latest stable Xcode on its `macos-26` runner. Xcode 15 is too old for the Swift syntax in this source tree. The project configures `iosArm64` and `iosSimulatorArm64`; Intel simulator builds are not configured.
- **Desktop:** JDK 17 on macOS, Windows, or Linux. Android modules are configured in the root build, so keep the Android SDK available even when working on Desktop. Platform installers must be built on their corresponding operating system.

Dependency versions live in the [version catalog](../gradle/libs.versions.toml), and SDK levels are set in the [application](../build-logic/convention/src/main/kotlin/CivitDeckAndroidApplicationPlugin.kt) and [KMP library](../build-logic/convention/src/main/kotlin/CivitDeckKmpLibraryPlugin.kt) convention plugins. Use those files and [CI](../.github/workflows/ci.yml) when updating this guide.

## Clone

```bash
git clone https://github.com/rioX432/CivitDeck.git
cd CivitDeck
```

The first Gradle build downloads dependencies and may take longer. Local debug builds and unit tests do not need release signing credentials. Never commit API keys, keystores, or `local.properties`.

## Android

Choose an explicit distribution flavor to avoid an ambiguous install task:

```bash
# Build the standard GitHub-channel debug APK
./gradlew :androidApp:assembleGithubFullDebug

# Install on a connected device or running emulator
./gradlew :androidApp:installGithubFullDebug

# Build the FOSS flavor without ML dependencies or in-app self-update
./gradlew :androidApp:assembleFdroidDebug

# Shared Android host unit tests (no emulator required)
./gradlew :shared:testAndroidHostTest
```

Debug builds install as `com.riox432.civitdeck.debug` and appear as **CivitDeck Debug**.
They intentionally coexist with the release app and keep separate app data. GitHub Releases
use `com.riox432.civitdeck`; only `githubFullRelease` offers in-app self-update. A release APK
cannot update a debug install, so the debug build does not offer that flow. Do not change the
release application ID or remove the debug suffix to work around signing incompatibility.

To verify the resolved update flags and package IDs without building or signing APKs:

```bash
./gradlew :androidApp:generateGithubFullDebugBuildConfig \
  :androidApp:generateGithubFullReleaseBuildConfig \
  :androidApp:generateFdroidDebugBuildConfig \
  :androidApp:generateFdroidReleaseBuildConfig
python3 scripts/check_android_update_channels.py
python3 -m unittest discover -s scripts/tests -p 'test_android_update_channels.py' -v
```

Flavor definitions are in [androidApp/build.gradle.kts](../androidApp/build.gradle.kts). The `fdroid` build flavor is not itself evidence of an available store listing. Similarity search is experimental and off by default in `githubFull`; no feature flag is needed for ordinary contributions.

## Desktop

```bash
./gradlew :desktopApp:run
./gradlew :desktopApp:compileKotlinJvm
```

Run the real ZIP and shared lifecycle regressions with the same selection as Desktop CI:

```bash
./gradlew :shared:jvmTest --tests '*DatasetExportZipReadinessTest' --tests 'com.riox432.civitdeck.data.export.DatasetZipTransactionTest' --tests 'com.riox432.civitdeck.data.export.DatasetZipLifecycleTest'
```

The filesystem tests live in `shared/src/jvmTest`; the lifecycle tests live in `shared/src/commonTest` and also run on the JVM. A successful compile alone does not run these tests.

## iOS

Open the Xcode project and select an **arm64 iOS Simulator** to build and run:

```bash
open iosApp/iosApp.xcodeproj
```

The project builds its Kotlin/Native framework directly; CocoaPods is not required. To reproduce CI's lint and unsigned simulator build on a compatible Mac:

```bash
(cd iosApp && swiftlint --strict)
xcodebuild build \
  -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -destination 'generic/platform=iOS Simulator' \
  ARCHS=arm64 \
  ONLY_ACTIVE_ARCH=NO \
  CODE_SIGNING_REQUIRED=NO \
  CODE_SIGNING_ALLOWED=YES \
  EXPANDED_CODE_SIGN_IDENTITY=""
```

A physical-device build needs your own signing configuration. The simulator build does not verify device sharing, filesystem behavior, or an app's complete user flow.

### Unsigned IPA

The [release workflow](../.github/workflows/release.yml) builds an **unsigned** iOS device IPA for sideloading. It is not an App Store or TestFlight build, and it cannot be installed until you sign it yourself. Installation of this IPA has not been verified on a device; archive validation and a successful build do not show that it installs or launches.

To build and package it locally on the Xcode 26.x toolchain (use a fresh derived-data directory, because the packager refuses to overwrite an existing IPA or checksum):

```bash
bash -n scripts/package_unsigned_ipa.sh
python3 -m unittest discover -s scripts/tests -p 'test_unsigned_ipa.py' -v
(cd iosApp && swiftlint --strict)
xcodebuild build \
  -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Release \
  -destination 'generic/platform=iOS' \
  -derivedDataPath /tmp/civitdeck-ipa-review \
  CODE_SIGNING_ALLOWED=NO \
  CODE_SIGNING_REQUIRED=NO \
  CODE_SIGN_IDENTITY='' \
  CODE_SIGN_ENTITLEMENTS='' \
  DEVELOPMENT_TEAM='' \
  ONLY_ACTIVE_ARCH=NO
bash scripts/package_unsigned_ipa.sh \
  /tmp/civitdeck-ipa-review/Build/Products/Release-iphoneos \
  /tmp/civitdeck-ipa-review/CivitDeck-0.0.0-ios-unsigned.ipa 0.0.0 1.0.1
(cd /tmp/civitdeck-ipa-review && shasum -a 256 -c CivitDeck-0.0.0-ios-unsigned.ipa.sha256)
```

`scripts/package_unsigned_ipa.sh PRODUCTS_DIRECTORY OUTPUT_IPA VERSION BUILD` requires exactly one device-platform `.app` with a non-empty Mach-O executable. It copies the app into a temporary `Payload` directory, removes copied signatures and provisioning profiles, and writes `VERSION` and `BUILD` into the copy's `Info.plist`; the build output is not modified. It validates the archive before it creates the IPA and its `shasum -a 256` sidecar.

**Version numbers.** `CFBundleShortVersionString` and `CFBundleVersion` accept only numbers and dots, so both arguments must be three numeric components without leading zeros.

- A tag push needs a `vMAJOR.MINOR.PATCH` tag and produces `CivitDeck-MAJOR.MINOR.PATCH-ios-unsigned.ipa`. Other tag shapes, such as prereleases, fail the iOS job and therefore block the release.
- A manual run uses version `0.0.0` and names the file `CivitDeck-0.0.0-run<run number>.<attempt>-ios-unsigned.ipa`.
- The build number is derived from the workflow run number `n` as `(1 + n/10000).((n/100) % 100).(n % 100)` with integer division, so it increases with every run (run 1 is `1.0.1`).

**Manual artifact run.** Dispatching the workflow builds only the iOS IPA; the Android, Desktop and release jobs are skipped, and no GitHub Release is created or changed. The IPA and checksum are uploaded as the `ios-ipa` Actions artifact:

```bash
gh workflow run release.yml --ref master
gh run list --workflow release.yml --event workflow_dispatch --limit 1
gh run download <run-id> --name ios-ipa
shasum -a 256 -c CivitDeck-*-ios-unsigned.ipa.sha256
```

A tag push publishes a release only when the Android, macOS, Windows, Linux and iOS jobs all succeed.

**Signing and installing.** To install the IPA on your own device, re-sign it with your own Apple ID or Apple Developer account, using a sideloading tool such as AltStore or SideStore, or your own certificate and provisioning profile. Before describing an IPA as installation-tested, record the signing tool and version, the device model and iOS version, and a successful install and launch.

The unsigned packaging is adapted from bpawnzZ's fork commits [9530081](https://github.com/bpawnzZ/CivitDeck/commit/9530081948f330023a8d26cf401880f411281eac) and [1d24997](https://github.com/bpawnzZ/CivitDeck/commit/1d249974a610d8146215ae58594f79b4ed0fa6c0).

## Checks Before a Pull Request

1. Run tests for the modules and platforms you changed. For shared export changes, run both the shared Android host tests and the Desktop ZIP test selection above.
2. Run `./gradlew detekt` for Kotlin changes. Auto-correct is enabled; review the resulting diff before committing. Run SwiftLint for Swift changes.
3. Build the affected app. For UI changes, exercise the changed flow, including cancellation and repeated actions, and include screenshots in the PR.
4. State what passed, what failed, and what you could not run. Do not describe skipped platform jobs as verified builds.

### Full Android CI Command

This is the test, lint, and build command from [CI](../.github/workflows/ci.yml):

```bash
./gradlew \
  :shared:testAndroidHostTest \
  :core:core-database:jvmTest \
  :core:core-domain:testAndroidHostTest \
  :core:core-network:testAndroidHostTest \
  :core:core-data:testAndroidHostTest \
  :core:core-ml:testAndroidHostTest \
  :feature:feature-search:testAndroidHostTest \
  :feature:feature-detail:testAndroidHostTest \
  :feature:feature-gallery:testAndroidHostTest \
  :feature:feature-prompts:testAndroidHostTest \
  :feature:feature-creator:testAndroidHostTest \
  :feature:feature-externalserver:testAndroidHostTest \
  :feature:feature-comfyui:jvmTest \
  detekt \
  :androidApp:assembleDebug
```

KMP libraries use `testAndroidHostTest` with the current Android KMP plugin. `core-database` and `feature-comfyui` use JVM tests in this CI command because of their platform-specific test setup. Desktop and iOS checks are listed in their sections above.

CI selects jobs using changed paths. A documentation-only PR can legitimately skip app builds. The workflow files ending in `.template` are examples, not active checks; experimental UI smoke checks are not required gates.

## Where to Make a Change

- Put domain models and repository interfaces in `core/core-domain/src/commonMain`, network DTOs and clients in `core/core-network/src/commonMain`, and network/cache repository implementations in `core/core-data/src/commonMain`.
- Feature modules own shared ViewModels and feature use cases under `feature/feature-*/src/commonMain`. For example, [ModelSearchViewModel](../feature/feature-search/src/commonMain/kotlin/com/riox432/civitdeck/feature/search/presentation/ModelSearchViewModel.kt) is shared rather than duplicated per platform.
- `shared/src/commonMain` coordinates modules and contains shared integration code. Keep Android, JVM, and iOS implementation details in the corresponding platform source sets.
- Put portable tests in `src/commonTest` and JVM-only filesystem tests in `src/jvmTest`.

See [Architecture](ARCHITECTURE.md) for module boundaries and [Contributing](CONTRIBUTING.md) for the PR workflow.
