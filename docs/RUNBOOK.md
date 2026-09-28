# Trigger operational runbook

This guide covers local activation, permissions, touch troubleshooting, and support diagnostics. Menu names vary by Android/OEM release. Trigger cannot pair ADB, enable privileged settings, or grant itself special access; use Android settings and a trusted workstation.

## 1. First-run checklist

1. Install the signed Trigger build and launch it.
2. Review the pre-flight disclosure before opening each sensitive permission screen. Grant overlay, DND, microphone, or battery access only if you plan to use the corresponding feature. Usage access is optional in this build; foreground-game auto-detection is not currently wired.
3. Start Shizuku from its own app or have an authorized root setup available. Accept Trigger's Shizuku permission prompt.
4. Launch a game from Trigger. Start and stop the floating overlay explicitly; confirm that Android shows the persistent foreground-service notification while it is active.
5. Test a single tap or a harmless macro in a controlled app before using macros in a game. Do not use automation to violate a game's terms or interfere with other users.

## 2. Shizuku diagnostics (Android 14/15 included)

### Binder disconnected / user service not connected

1. Open Shizuku and inspect its service status. If stopped, start it using the method appropriate to your device (wireless debugging, USB/ADB, or root).
2. In Shizuku, open Authorized applications and remove/re-grant Trigger if its permission state is stale. Return to Trigger and retry; the app rechecks state when resumed.
3. If Shizuku reports a dead service, stop and restart Shizuku. Then force-stop and reopen Trigger. This recreates the Shizuku UserService connection and its input-service Binder proxy.
4. On Android 14/15, confirm Shizuku is allowed to run its foreground/service components. Disable OEM-specific background restrictions for Shizuku and Trigger only if needed. Avoid globally disabling Android security or battery protections.
5. Check that Shizuku and Trigger were installed for the same Android user/profile. Binder authorization does not cross work profiles automatically.
6. If the device was rebooted, wireless debugging authorization may need to be started again. Root-backed Shizuku may run as UID 0; ADB-backed Shizuku normally runs its UserService as shell UID 2000.
7. Export execution diagnostics with the system document picker and inspect the sanitized `SERVICE_DISCONNECTED` / `INJECTION_REJECTED` entries. Do not attach raw device logs containing unrelated personal information to a public issue.

### Wireless ADB pairing (Android 11+)

Use a trusted computer on the same private network. The app does not run `adb` or receive pairing codes.

1. Enable Developer options by tapping **Build number** repeatedly in the device's About screen.
2. Open **Developer options → Wireless debugging** and enable it. Keep the phone and workstation on the same trusted Wi-Fi network; avoid public networks.
3. Select **Pair device with pairing code** on the phone. Note the temporary pairing address/port and code.
4. On the workstation, run `adb pair <phone-ip>:<pairing-port>`, then enter the one-time code when prompted. The pairing port is not the connection port.
5. Return to the Wireless debugging page and note its main IP address/port. Run `adb connect <phone-ip>:<connection-port>` on the workstation. Verify with `adb devices`.
6. Open Shizuku and use its **Start via Wireless debugging** instructions. Approve any Android prompt, then confirm that Shizuku reports running.
7. Open Trigger, approve its Shizuku request, and retry the game overlay. Re-pair after revoking debugging authorizations, changing networks, or when Android rotates the debugging port.
8. When finished, disable Wireless debugging and revoke workstation authorizations if the workstation is shared or no longer trusted.

### Root / Magisk / KernelSU fallback

1. Confirm the device is actually rooted and that the root manager is installed and healthy. The presence of an `su` file alone does not prove that a privileged command will be granted.
2. Open Magisk/KernelSU's superuser list, launch Trigger, and approve the request only if you intend to use root mode. Revoke it from the root manager when no longer needed.
3. Check for a denial/timeout in the root manager's log. Verify the executable path supported by the device (`/system/bin/su`, `/system/xbin/su`, or vendor-specific path); do not copy random `su` binaries into system partitions.
4. If SELinux policy or a vendor ROM blocks input injection, do not disable SELinux globally. Use the Shizuku shell path or consult the device vendor/root-manager documentation.
5. A detectable `su` binary is only a capability hint. If authorization fails, return to Shizuku/ADB or disable the feature; do not assume root is active from detection alone.

## 3. Overlay and touch-injection troubleshooting

### Overlay does not appear / service stops

1. Recheck **Display over other apps** in Android app settings. OEM security dashboards may provide a second overlay switch.
2. Keep the foreground-service notification enabled. Android 13+ may also ask for notification permission; notification permission is separate from overlay permission.
3. Start the overlay while Trigger is visible. Android may prohibit starting a foreground service from the background.
4. For MIUI/HyperOS, One UI, ColorOS, and similar OEMs, inspect the vendor's Auto-start/Background activity setting and battery policy. Prefer per-app exemptions over disabling global battery management.
5. If the overlay disappears after screen lock or a game transition, relaunch it from Trigger and check vendor task-killer settings. Android process death can invalidate an overlay window; restarting the service is the supported recovery.
6. Stop the overlay from Trigger before force-stopping the app. This restores the prior DND filter and releases the overlay windows.

### Touch offset, rotation, notch, or display scaling

1. Verify device orientation, display size, font/display scaling, and the game's own aspect-ratio/full-screen settings. Change one variable at a time.
2. Confirm the macro coordinates use the same source-view coordinate space and dimensions that were used when recording. Tap locations are converted through `CoordinateTransformer`; do not paste coordinates from a different resolution without recalibration.
3. On devices with cutouts or edge-to-edge UI, compare safe-area/cutout insets and display rotation. Use the transform's `DisplayInsets` input in a device-specific integration where the app window's usable content excludes those insets.
4. Test landmarks at the center and all four corners in each supported rotation using a benign target. If the center is correct but edges drift, suspect scaling/aspect-ratio mismatch; if all points are translated, suspect status/navigation/cutout insets.
5. Avoid changing density with `wm density` on a daily-use device as a first-line fix. If changed for testing, record the original value and restore it after the test.
6. Confirm the device accepts shell `IInputManager` injection. Some vendor builds restrict injection despite Shizuku authorization; inspect `INJECTION_REJECTED` logs and test on a supported stock-like build.

### Audio/DND issues

- Microphone access is required only for the optional DSP. The current DSP is a simple local pitch-shifted monitor, not formant-preserving voice conversion and not guaranteed to feed a game's voice-chat microphone route.
- Use headphones to reduce feedback. Stop the DSP if audio loops, clips, or causes unacceptable latency. Revoke microphone access when not using it.
- DND access changes the device interruption filter. If the prior mode is not restored after an abnormal process kill, reopen Android's Do Not Disturb settings and restore the preferred mode manually.

## 4. Developer diagnostics

- Run `scripts/verify_build.sh` before proposing a release. It requires `./gradlew` or a system Gradle installation.
- Unit tests: `:automation:engine:test` and `:feature:gameassistant:test`.
- Room/device tests: `:core:database:connectedAndroidTest` on an emulator/device.
- Baseline profile: `:baselineprofile:connectedBenchmarkAndroidTest` on a physical/profileable benchmark device.
- Keep Shizuku/root credentials, pairing codes, signing keys, unredacted dumps, and user macro contents out of source control and issue reports.
