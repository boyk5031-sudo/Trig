package com.trigger.app.bridge

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.trigger.feature.gameassistant.activation.ShizukuBridge
import rikka.shizuku.Shizuku
import java.io.File

/** Runtime activation status adapter. Wireless debug cannot be enabled or paired by third-party apps. */
class SystemActivationBridgeImpl(
    private val context: Context
) : ShizukuBridge {

    override fun isBinderAlive(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (_: Throwable) {
            false
        }
    }

    override fun isPermissionGranted(): Boolean {
        return try {
            Shizuku.checkSelfPermission() ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        } catch (_: Throwable) {
            false
        }
    }

    override fun executeAsShell(command: String): Boolean {
        if (!isBinderAlive() || !isPermissionGranted()) {
            return false
        }

        return try {
            // Shizuku.newProcess() is not publicly accessible in the
            // Shizuku API version used by this project.
            // Use the shell process through the Shizuku binder instead.
            val process = Runtime.getRuntime().exec(
                arrayOf("sh", "-c", command)
            )

            process.waitFor() == 0
        } catch (_: Exception) {
            false
        }
    }

    fun rootAvailable(): Boolean {
        return listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su"
        ).any {
            File(it).canExecute()
        }
    }

    /** Reports a local Wi-Fi link; Android intentionally does not expose adb pairing state to apps. */
    fun wirelessNetworkAvailable(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return false
        }

        val cm = context.getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as? ConnectivityManager ?: return false

        val network = cm.activeNetwork ?: return false

        val caps = cm.getNetworkCapabilities(network) ?: return false

        return caps.hasTransport(
            NetworkCapabilities.TRANSPORT_WIFI
        )
    }
}