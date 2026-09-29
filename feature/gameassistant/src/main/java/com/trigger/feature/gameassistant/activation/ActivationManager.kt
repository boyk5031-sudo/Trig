package com.trigger.feature.gameassistant.activation

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import java.io.File

/** Host integration point for current and legacy Shizuku wrappers; no hidden API reflection. */
interface ShizukuBridge {
    fun isBinderAlive(): Boolean
    fun isPermissionGranted(): Boolean
    fun executeAsShell(command: String): Boolean
}

enum class ActivationMode { ROOT, SHIZUKU, SHIZUKU_LEGACY, WIRELESS_DEBUGGING, USB_ASSISTED, UNAVAILABLE }
data class ActivationState(val mode: ActivationMode, val ready: Boolean, val message: String)
enum class RequiredAccess { OVERLAY, BATTERY_EXEMPTION, DND, MICROPHONE, USAGE_STATS }

class ActivationManager(private val context: Context, private val shizuku: ShizukuBridge? = null, private val rootPaths:List<String> = listOf("/system/bin/su", "/system/xbin/su", "/sbin/su", "/system/sbin/su")) {
    fun detect(): ActivationState {
        if (rootAvailable()) return ActivationState(ActivationMode.ROOT, true, "Root shell available")
        val bridge = shizuku
        if (bridge != null) {
            try {
                if (bridge.isBinderAlive() && bridge.isPermissionGranted())
                    return ActivationState(ActivationMode.SHIZUKU, true, "Shizuku shell permission granted")
            } catch (_: LinkageError) {
                return ActivationState(ActivationMode.SHIZUKU_LEGACY, false, "Shizuku API mismatch; configure the legacy bridge")
            } catch (_: Exception) { }
        }
        return ActivationState(ActivationMode.WIRELESS_DEBUGGING, false, "Enable Shizuku, root, or wireless debugging")
    }

    fun rootAvailable(): Boolean = rootPaths.any { File(it).canExecute() }
    fun usbConnected(): Boolean = try {
        context.registerReceiver(null, android.content.IntentFilter("android.hardware.usb.action.USB_STATE"))?.getBooleanExtra("connected", false) == true
    } catch (_: Exception) { false }
    fun has(access: RequiredAccess): Boolean = when (access) {
        RequiredAccess.OVERLAY -> Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(context)
        RequiredAccess.BATTERY_EXEMPTION -> Build.VERSION.SDK_INT < 23 || (context.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(context.packageName)
        RequiredAccess.DND -> Build.VERSION.SDK_INT < 23 || (context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager).isNotificationPolicyAccessGranted
        RequiredAccess.MICROPHONE -> ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        RequiredAccess.USAGE_STATS -> try { val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager; appOps.checkOpNoThrow("android:get_usage_stats", android.os.Process.myUid(), context.packageName) == android.app.AppOpsManager.MODE_ALLOWED } catch (_: Exception) { false }
    }
    fun requiredAccess(): Map<RequiredAccess, Boolean> = RequiredAccess.entries.associateWith(::has)
    fun openDeveloperOptions() = context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    fun openOverlaySettings() = context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    fun openBatterySettings() = context.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    fun openDndSettings() = context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    fun openUsageSettings() = context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    fun openWirelessDebugging() = context.startActivity(Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    fun usbVerificationIntent(): Intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)

    /** Pair/connect are intentionally delegated to platform adb/Shizuku; Android apps cannot spawn adb. */
    fun wirelessDebuggingGuidance(): String = "On Android 11+, enable Wireless debugging in Developer options, pair with a trusted workstation, then start the host-side adb connect."
}
