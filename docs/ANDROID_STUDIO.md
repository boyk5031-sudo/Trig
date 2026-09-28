# Open and run Trigger in Android Studio

## Required tooling

- Android Studio version with Android Gradle Plugin 8.7 support (a current stable release is recommended).
- JDK 21. In Android Studio, use **Settings/Preferences → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK** and select the bundled JBR/JDK 21.
- Android SDK Platform 35 from SDK Manager. Install platform/build tools requested by Gradle sync. For device testing, install Android 10/API 29 or newer system images; current project `minSdk` is 26.
- Gradle 8.9 for AGP 8.7.3.

## Import

1. Open the repository root (the directory containing `settings.gradle.kts`), not `app/` or a nested module.
2. Allow Gradle project sync and approve SDK component downloads from Google's Android SDK Manager.
3. If prompted, use JDK 21 and Gradle 8.9. The version catalog is `gradle/libs.versions.toml` and all modules are declared in root `settings.gradle.kts`.
4. If SDK discovery fails, create `local.properties` in the repository root with the local SDK location, for example `sdk.dir=/Users/<you>/Library/Android/sdk` on macOS or `sdk.dir=C\\:\\Users\\<you>\\AppData\\Local\\Android\\Sdk` on Windows. `local.properties` is ignored by Git; do not commit a machine-specific path.
5. Select the `app` run configuration and a device/emulator. The application module ID is `com.trigger.app`; the debug variant adds `.debug`.

## Gradle wrapper setup caveat

This checkout currently contains `gradle/wrapper/gradle-wrapper.properties`, but the wrapper JAR and launcher scripts are not present. The coding environment had no Java/Gradle and outbound downloads were unavailable, so the standard wrapper could not be generated here. Android Studio may report a missing wrapper JAR until it is bootstrapped once:

1. Install a system Gradle 8.9 and JDK 21 using an approved package manager or the official Gradle distribution.
2. From the repository root run `scripts/bootstrap_gradle_wrapper.sh`.
3. Review and commit the generated `gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.jar` to the session branch, then reopen/sync using the wrapper. Confirm the Gradle distribution checksum using the official Gradle release checksum before accepting a distribution in a controlled environment.

Until the wrapper is generated, configure Android Studio to use a local Gradle 8.9 installation if that option is available, or use `gradle` from the terminal. `scripts/verify_build.sh` prefers `./gradlew` and otherwise falls back to installed Gradle.

## Build, test, and run

- Sync/build debug: `gradle :app:assembleDebug` (or `./gradlew :app:assembleDebug` after wrapper bootstrap).
- Unit tests: `gradle :automation:engine:test :feature:gameassistant:test`.
- Room tests require a connected emulator/device: `gradle :core:database:connectedAndroidTest`.
- Baseline Profile collection requires a profileable physical device: `gradle :baselineprofile:connectedBenchmarkAndroidTest`.
- Full release validation: `scripts/verify_build.sh`.

On a device, first-run functionality may require separately installed/running Shizuku and user-granted overlay, DND, and microphone permissions. The Android emulator generally cannot validate real shell input injection, OEM battery policies, or production audio routing. See [RUNBOOK.md](RUNBOOK.md) before device testing.

## Release signing

Use the environment variables in [RELEASE.md](RELEASE.md) for a release keystore. Never put release passwords in `local.properties`, source control, or a shared run configuration. A debug install is signed with the Android debug key and is not a release artifact.
