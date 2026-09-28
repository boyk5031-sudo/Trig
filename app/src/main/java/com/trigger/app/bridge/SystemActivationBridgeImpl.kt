package com.trigger.app.bridge

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.trigger.feature.gameassistant.activation.ShizukuBridge
import rikka.shizuku.Shizuku
import java.io.File

/** Runtime activation status adapter. Wireless debug cannot be enabled or paired by third-party apps. */
class SystemActivationBridgeImpl(private val context: Context) : ShizukuBridge {
    override fun isBinderAlive(): Boolean = try { Shizuku.pingBinder() } catch (_: Throwable) { false }
    override fun isPermissionGranted(): Boolean = try { Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED } catch (_: Throwable) { false }
    override fun executeAsShell(command: String): Boolean = try {
        if (!isBinderAlive() || !isPermissionGranted()) return false
        Shizuku.newProcess(arrayOf("sh","-c",command),null,null).waitFor() == 0
    } catch (_: Exception) { false }
    fun rootAvailable(): Boolean = listOf("/system/bin/su","/system/xbin/su","/sbin/su").any { File(it).canExecute() }
    /** Reports a local Wi-Fi link; Android intentionally does not expose adb pairing state to apps. */
    fun wirelessNetworkAvailable(): Boolean {
        val cm=context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network=cm.activeNetwork ?: return false
        val caps=cm.getNetworkCapabilities(network) ?: return false
        return Build.VERSION.SDK_INT >= 23 && caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
}
