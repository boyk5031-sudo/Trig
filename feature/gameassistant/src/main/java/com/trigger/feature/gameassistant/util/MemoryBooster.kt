package com.trigger.feature.gameassistant.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Debug
import java.util.concurrent.Executor
import java.util.concurrent.Executors

data class MemoryReport(val beforeMb: Long, val afterMb: Long, val estimatedFreedMb: Long, val candidates: Int)
/** Best-effort trim: Android only permits apps to kill their own package's processes. */
class MemoryBooster(context: Context, private val executor: Executor = Executors.newSingleThreadExecutor()) {
    private val app = context.applicationContext
    fun boost(callback: (MemoryReport) -> Unit) = executor.execute {
        val am = app.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        fun used(): Long { val info = ActivityManager.MemoryInfo(); am.getMemoryInfo(info); return (info.totalMem-info.availMem)/(1024*1024) }
        val before=used(); val own = app.packageName
        val candidates = try { am.runningAppProcesses.orEmpty().count { it.processName != own && it.importance >= ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND } } catch (_: SecurityException) { 0 }
        // Third-party packages cannot legally be terminated by a normal app. Trim this app's
        // background processes only; do not use hidden APIs or claim system-wide reclaimed RAM.
        try { if (Build.VERSION.SDK_INT >= 19) am.killBackgroundProcesses(own) } catch (_: SecurityException) { }
        System.gc(); val after=used(); callback(MemoryReport(before,after,(before-after).coerceAtLeast(0),candidates))
    }
}
