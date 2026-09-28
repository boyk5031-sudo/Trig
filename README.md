# Trigger

Android multi-module game assistant and macro automation app. See [Android Studio setup](docs/ANDROID_STUDIO.md), [Architecture](docs/ARCHITECTURE.md), the [Operational Runbook](docs/RUNBOOK.md), and [Release Guide](docs/RELEASE.md) for development setup, system behavior, privacy boundaries, recovery procedures, and handoff requirements.

## Release validation

Run `scripts/verify_build.sh` to execute lint, unit tests, release-resource verification, and release assembly. The repository may be built with a Gradle wrapper (`./gradlew`) or an installed Gradle distribution. Configure a release keystore through Gradle properties or environment variables:

- `TRIGGER_RELEASE_STORE_FILE`
- `TRIGGER_RELEASE_STORE_PASSWORD`
- `TRIGGER_RELEASE_KEY_ALIAS`
- `TRIGGER_RELEASE_KEY_PASSWORD`

Never commit keystore files or signing secrets. Version values can be overridden with `-PreleaseVersionCode=... -PreleaseVersionName=...`.

## Tests and profiles

- `:automation:engine:test` exercises macro execution and display-coordinate transforms.
- `:feature:gameassistant:test` validates activation checks and bounded PCM DSP.
- `:core:database:connectedAndroidTest` validates Room persistence, cascades, and migration SQL on a device/emulator.
- `:baselineprofile:connectedBenchmarkAndroidTest` captures the dashboard and navigation startup profile on a benchmark device.

The Shizuku user service validates that it is running as shell/root and restricts Binder callers to the app UID provided by Shizuku. User-service events are accepted only as `MotionEvent`s in asynchronous injection mode. The execution monitor writes diagnostics only through Android's user-selected Storage Access Framework document picker.
