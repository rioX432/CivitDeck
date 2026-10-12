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
