package com.trigger.feature.gameassistant.util

import com.trigger.feature.gameassistant.service.TouchDispatcher
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.roundToLong

data class CpsTelemetry(val current: Int, val peak: Int, val average: Float, val totalTaps: Long)
/** Adapter expected to be backed by the host TriggerEngine/ShizukuTouchInjector. */
class TriggerMacroController(private val dispatcher: TouchDispatcher) : AutoCloseable {
    private val executor = Executors.newSingleThreadScheduledExecutor(); private val active = AtomicBoolean(false)
    private val count = AtomicLong(); private val started = System.nanoTime(); @Volatile private var peak = 0
    private var task: ScheduledFuture<*>? = null
    @Synchronized fun startAutoFire(x: Float, y: Float, cps: Int) { stop(); active.set(true); val period=(1_000_000_000L/cps.coerceIn(1,60)); task=executor.scheduleAtFixedRate({ if(active.get()) try { dispatcher.tap(x,y); count.incrementAndGet() } catch (_: Exception) { stop() } },0,period,TimeUnit.NANOSECONDS) }
    fun hold(x: Float,y: Float) { if(active.compareAndSet(false,true)) try { dispatcher.startHold(x,y) } catch (_: Exception) { active.set(false) } }
    fun multiTap(x: Float,y: Float,taps: Int, intervalMs: Long) { val n=taps.coerceIn(1,100); executor.execute { repeat(n) { if(Thread.currentThread().isInterrupted) return@execute; dispatcher.tap(x,y); count.incrementAndGet(); if(it<n-1) Thread.sleep(intervalMs.coerceAtLeast(0)) } } }
    @Synchronized fun stop() { active.set(false); task?.cancel(true); task=null; try { dispatcher.stop() } catch (_: Exception) { } }
    fun telemetry(): CpsTelemetry { val total=count.get(); val elapsed=(System.nanoTime()-started)/1e9; val avg=if(elapsed>0) total/elapsed else 0.0; val current=if(task?.isDone==false) 1 else 0; peak=maxOf(peak,current); return CpsTelemetry(current,peak,avg.toFloat(),total) }
    override fun close() { stop(); executor.shutdownNow() }
}
