package com.trigger.automation.engine

import rikka.shizuku.Shizuku
import java.io.IOException
import java.util.concurrent.Executors

/** Shell-UID input bridge. The app must have Shizuku permission; no hidden framework APIs used. */
class ShizukuTouchInjector {
    @Synchronized fun injectTap(x: Int, y: Int): Boolean = runInput("input tap ${x.coerceAtLeast(0)} ${y.coerceAtLeast(0)}")
    @Synchronized fun injectTouchDown(x: Int, y: Int): Boolean = runInput("input touchscreen swipe ${x.coerceAtLeast(0)} ${y.coerceAtLeast(0)} ${x.coerceAtLeast(0)} ${y.coerceAtLeast(0)} 60000")
    @Synchronized fun injectTouchUp(x: Int, y: Int): Boolean = runInput("input tap ${x.coerceAtLeast(0)} ${y.coerceAtLeast(0)}")
    private fun runInput(command: String): Boolean {
        return try {
            val process = if (try { Shizuku.pingBinder() && Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED } catch (_: Throwable) { false }) {
                Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
            } else {
                Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            }
            val code = process.waitFor(); code == 0
        } catch (_: SecurityException) { false } catch (_: IOException) { false } catch (_: InterruptedException) { Thread.currentThread().interrupt(); false }
    }
}
