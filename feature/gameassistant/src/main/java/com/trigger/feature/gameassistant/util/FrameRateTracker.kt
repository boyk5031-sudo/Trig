package com.trigger.feature.gameassistant.util

import android.view.Choreographer
import java.util.ArrayDeque

/** Main-thread Choreographer FPS sampler over a rolling one-second window. */
class FrameRateTracker(private val onFps: (Float) -> Unit) : Choreographer.FrameCallback {
    private val samples = ArrayDeque<Long>(); private var active = false
    fun start() { if (!active) { active=true; Choreographer.getInstance().postFrameCallback(this) } }
    fun stop() { active=false; Choreographer.getInstance().removeFrameCallback(this); samples.clear() }
    override fun doFrame(frameTimeNanos: Long) { if (!active) return; samples.addLast(frameTimeNanos); val cutoff=frameTimeNanos-1_000_000_000L; while(samples.isNotEmpty() && samples.first < cutoff) samples.removeFirst(); onFps(if(samples.size<2) 0f else (samples.size-1)*1_000_000_000f/(samples.last-samples.first).coerceAtLeast(1)); Choreographer.getInstance().postFrameCallback(this) }
}
