# Release and distribution guide

## Release prerequisites

1. Run the verification script from the repository root: `scripts/verify_build.sh` (or explicitly `./gradlew lint test verifyReleaseResources assembleRelease`). Run Android instrumentation tests on a device/emulator with `:core:database:connectedAndroidTest`; run the Baseline Profile generator on a supported profileable device.
2. Review the merged release manifest, especially the Shizuku provider, foreground-service declarations, launcher-only `<queries>`, and absence of `INTERNET` / `QUERY_ALL_PACKAGES`.
3. Verify permission disclosures, foreground-service notification/stop behavior, DND restoration, and system-picker export on API 26, 29, 33, 34, and 35 devices. A successful build is not a substitute for device validation.
4. Do not publish claims that voice conversion is formant-preserving or guaranteed to route into a game's voice-chat microphone. The current implementation is a basic local pitch-shift monitor.

## Signing and versioning

Set these secrets in a protected CI environment or local environment for the release build. Never commit keystores or passwords:

```text
TRIGGER_RELEASE_STORE_FILE=/secure/path/release.jks
TRIGGER_RELEASE_STORE_PASSWORD=...
TRIGGER_RELEASE_KEY_ALIAS=...
TRIGGER_RELEASE_KEY_PASSWORD=...
```

Gradle project properties with the same names are also accepted. Override version metadata with `-PreleaseVersionCode=100 -PreleaseVersionName=1.0.0`. Ensure the integer version code is monotonically greater than the last Play release. If no signing variables are set, the release variant can be built unsigned for CI validation; do not distribute it.

## Android App Bundle (AAB)

With the Gradle wrapper installed in the repository:

```bash
./gradlew :app:bundleRelease
```

Expected artifact: `app/build/outputs/bundle/release/app-release.aab` (variant/output names can change if flavors are introduced). Verify it with `bundletool validate --bundle=...` and inspect the final merged manifest. Upload the AAB to an internal/closed testing track before production; test upgrade, split delivery, overlay, Shizuku authorization, notification permission, and rollback behavior.

## R8 mapping and reproducibility

`app/build/outputs/mapping/release/mapping.txt` is required to de-obfuscate R8 stack traces. At each release:

1. Immediately archive `mapping.txt` with the exact AAB, version code/name, source commit SHA, AGP/Kotlin versions, and build provenance.
2. Store mapping files in access-controlled CI artifact storage or Play Console's deobfuscation upload; do not publish them in the app bundle or public repository.
3. Calculate and record a SHA-256 digest for both AAB and mapping file. Retain artifacts under the organization's retention/security policy; do not overwrite a version's mapping with a later build.
4. Confirm the R8 rules retain the AIDL stub/user-service entry point, serializer models, and reflected system-service names. Review any new `-keep` rule for unnecessary retention.

## Baseline Profile

The `:baselineprofile` module profiles cold app launch and common dashboard navigation. On a compatible physical/profileable device, generate and verify the profile using the AndroidX Baseline Profile tasks, for example:

```bash
./gradlew :baselineprofile:connectedBenchmarkAndroidTest
./gradlew :app:generateReleaseBaselineProfile
```

Task names can vary with AGP/Baseline Profile plugin versions. Confirm the generated profile is packaged in the release artifact and compare startup/frame metrics on the same device before and after changes. The current profile scenario does not grant special access or benchmark game-overlay injection.

## Store policy and Data Safety review

- Explain why overlay, microphone, DND, and optional usage access are requested in the in-app disclosure and store listing.
- Re-audit the release manifest and all runtime dependencies for network, analytics, or data collection changes before making a Data Safety declaration. The current app manifest does not request `INTERNET`, and app source contains no remote telemetry/upload path; this is a code audit result, not a blanket guarantee about Android, Shizuku, or store infrastructure.
- Usage access is not used for foreground-game monitoring in this build. Do not claim automatic usage-based launch detection until implemented and tested.
- Exported diagnostics use a user-selected SAF destination. Avoid asking for broad shared-storage access.

## Git handoff, commits, and tags

1. Confirm `git status` is clean, tests/builds passed, release notes and version metadata match, and the change is reviewed.
2. Commit focused release changes on the authorized feature branch and open a pull request into the protected `main` branch. Do not force-push or push directly to `main`; use the repository's review/CI/branch-protection policy.
3. After the PR is merged and the release commit is approved, an authorized release maintainer creates an annotated/signed tag on that exact main commit, for example `v1.0.0-release`, and pushes the tag. Verify the tag points to the release source SHA before uploading the AAB.
4. This Arena session is fixed to `arena/01a0e826-trig`; its agent must only commit/push that branch and must hand off via PR. It must not switch to or push `main`.

The current checkout does not include `gradlew` and this execution environment has no installed Gradle. Add/restore the project's Gradle wrapper (matching the AGP-compatible Gradle version) and run the full release validation before treating an AAB as distributable.
