# Performance audit and verification

## Status

These are source-confirmed defects and targeted fixes, **not measured speedups**.
The device-wide cause of the reported lag is still unconfirmed: no device model,
Android version, APK build type, Logcat, CPU trace or heap capture was supplied.
No APK was produced in this environment. The requested Gradle test/build command
failed before compilation because `./gradlew` is missing. There is also no Java,
system Gradle, Android SDK, adb, emulator, or Android Studio Profiler installed.
`git diff --check` passes. New regression tests have been written but not run.

## Findings and changes

| Path / trigger | Evidence in original source | Change |
|---|---|---|
| MainActivity / opening and rotating | Every creation called prepareActivation, registered Shizuku listeners and began an unbounded reconnect loop; there was no Activity cleanup | No activation on startup. Create transport only when requested, keep application context, reuse the dispatcher, release idle transport on Activity destruction and active transport when the overlay closes. Removed unnecessary binding to an overlay whose onBind returns null. |
| Shizuku unavailable | A failed ping scheduled another ping forever (up to every 15 seconds per connection) | Wait for Shizuku's binder-arrival callback; bound transient binding retries to five, ignore duplicate binds, remove listeners/callbacks on close. |
| Home / Schedule / Monitor retained on navigation | ViewModel init jobs lived until the back-stack entry was destroyed, not until the screen stopped being visible | Share flows only while lifecycle-aware UI subscribers exist. Home system queries/JSON reads and Monitor filtering run off main. Schedule refresh stays on IO. |
| Bottom tabs | SingleTop without popping to the start destination retained new copies when switching tabs | Standard save/restore tab navigation with popUpTo, preserving tab state without accumulating copies. |
| Games list | PackageManager queries and repeated loadLabel calls during sorting/rendering on main | Discover, label and sort on IO; reuse labels in grid cells. |
| Macro catalog | Constructor decoded/seeds preferences during initial navigation composition; reads/writes and asset import ran on main | Lazily instantiate on IO only when macros are used. Move catalog operations and step testing off main. Free Fire package remains `com.freefireth`. |
| Tested swipes | Each injector created an executor, but close never shut it down; Test never closed injectors | Shut down only internally owned executors, close test injectors on swipe completion, stop after injection rejection, close worker injectors in use/finally. |
| Scheduled macro runs | A fresh SupervisorJob was attached to the worker scope, but never completed; a finished run could leave its parent waiting for that child | Launch the execution directly in the supplied scope; completion no longer leaves an orphan parent job. |
| Empty/zero-delay repeats | No suspension/cancellation point in empty repeats; pause checks did not always check cancellation | Yield between repeats and explicitly check cancellation before each step. Macro steps/repeat counts are unchanged. |
| Execution logs | CopyOnWriteArrayList copied the buffer for each write/removal; regex patterns recompiled on each step | Synchronized bounded deque and cached regexes; keep log limits and redaction. Runtime's separate 1,000-entry snapshot is still bounded but copies on updates. |
| Log export | Document-provider IO and JSON export ran in activity-result callback on main | Export on IO. Cache time formatter; avoid duplicate timestamp-based Compose list keys on fast repeats. |
| Audio helper | read <= 0 immediately continued at audio priority; close joined up to 500ms | Exit on input/output errors, release in finally, unblock native IO without joining on main, disallow overlapping workers. This helper is not currently launched by the visible UI, so it is a latent defect, not evidence of the startup cause. |

An overlay remains alive when a game is in the foreground, as intended. It is not
stopped simply because the Activity backgrounds. The launch switch now respects
“Enable overlay” being off. Actual requested macros and WorkManager schedules
remain functional; only hidden UI observation is suspended. JVM targets remain 17.

## Other areas inspected

- App/feature manifests: one special-use foreground overlay; no app-owned boot
  receiver, always-on network sync, or explicit wake lock found. WorkManager
  schedules have a 15-minute minimum and optional charging constraints.
- Room DAO uses suspend/Flow methods and indexed relationships; no production
  allowMainThreadQueries. Catalog/schedule data actually use JSON preferences,
  which may need Room migration if users have very large catalogs (not migrated here).
- Compose lazy lists/grids, theme/cards, overlay drawing and resource files:
  no bitmap loading or continuous HUD invalidation. No UI redesign applied.
  Corrected the overlay spring's IntOffset type and obsolete Compose import found
  during static review; compilation is still required.
- FrameRateTracker, DSP and MemoryBooster have no active callers in the current UI.
  MemoryBooster still forces GC and its default executor has no close API; do not
  wire it into startup or recurring work. This inactive utility needs lifecycle
  design before integration, rather than being blamed for current startup lag.
- Auto-fire intentionally injects up to 60 taps/second while enabled. Test that
  path separately; fixed-rate catch-up under a slow Binder and shared Shizuku user
  service ownership between an active overlay and scheduled worker remain areas
  requiring device tests. No silent rate reduction or feature removal was made.

## Build on an Android development machine

Install JDK 17, Android SDK Platform 35/build tools and Gradle 8.9 (the version
pinned in gradle/wrapper/gradle-wrapper.properties). Configure ANDROID_HOME or
local.properties. This checkout lacks gradlew, gradlew.bat and the wrapper JAR;
restore the trusted wrapper files or run the installed Gradle directly:

```sh
gradle :automation:engine:testDebugUnitTest :feature:gameassistant:testDebugUnitTest
gradle :app:lintDebug :app:assembleDebug
```

New `ResourceLifecycleTest` checks owned drag-thread shutdown, no orphan job after
completion, and cancellation of a huge empty repeat. Existing macro-order,
cancellation, timeout, log-redaction and DSP tests must continue to pass.
APK, if successful: `app/build/outputs/apk/debug/app-debug.apk` (package
`com.trigger.app.debug`). Release builds use `com.trigger.app`; release signing
must be configured locally. Do not share signing secrets.

## Required before/after device verification

Use the same physical phone, Android version, thermal state, build variant,
permissions, catalog and schedules for baseline `c2df183` and this change. Use a
separate clone/worktree if comparing revisions. Do not compare debug to release.

1. Record model/RAM/Android, build variant, and whether Shizuku, overlay, auto-fire,
   DSP or schedules are active. Keep an idle system baseline with this app stopped.
2. Capture Android Studio System Trace/Perfetto and CPU sampling: cold launch,
   60 seconds idle on Home, 20 rounds across tabs, background for 2 minutes, resume.
   Compare main-thread stalls, frame-time percentiles/jank, process CPU, runnable
   threads, scheduling and Binder activity. Launch with Shizuku stopped as well
   as available; no startup reconnect loop should appear.
3. Memory Profiler: compare PSS/heap/thread count after 20 rotations and repeated
   opening/closing, then after idle GC. No linear Activity, ServiceConnection or
   executor growth should remain. Test 20 swipe previews and cancellation midway.
4. Enable overlay and launch a game; navigate/rotate/background the Activity. The
   overlay and requested auto-fire must continue. Close it and confirm windows,
   notification, input resources and auto-fire stop. Repeat with permissions revoked.
5. Test auto-fire at 1/12/60 CPS separately and inspect both app and system_server
   CPU. Stop immediately if system input becomes unresponsive. Compare with the
   overlay alone; intentional high-rate input is not an idle baseline.
6. Run a short scheduled macro: verify WorkManager reaches completion (not stuck
   RUNNING), repeats correctly and releases resources on cancellation. Test it
   while the overlay is active to check shared Shizuku interactions.
7. Test macro save/import (including Free Fire), rapid navigation after saving,
   log export, permission settings round-trips, game launch with overlay off,
   foreground/background, force stop/reopen and process recreation.
8. If integrating DSP, simulate microphone revocation/read errors and repeated
   start/close: no spinning audio-priority thread or overlapping recordings.

Useful adb captures (substitute the package for your APK):

```sh
adb shell am force-stop com.trigger.app.debug
adb shell am start -W -n com.trigger.app.debug/com.trigger.app.MainActivity
adb shell dumpsys gfxinfo com.trigger.app.debug framestats
adb shell dumpsys meminfo com.trigger.app.debug
adb shell dumpsys activity services com.trigger.app.debug
adb shell dumpsys cpuinfo
adb logcat -d -v threadtime > trigger-logcat.txt
```

Use Battery Historian/Android battery stats over a longer controlled idle and
active interval; short startup traces cannot prove battery-life improvement.
Review logs/traces for personal information before sharing them. Acceptance needs
measured improvements with functionality/stability intact; no measurements or
numerical performance claims are available yet.
