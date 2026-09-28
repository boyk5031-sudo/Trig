package com.trigger.automation.engine

import java.util.concurrent.atomic.AtomicBoolean

/** Small dispatch facade shared with the game assistant; scheduling is owned by feature controller. */
class TriggerEngine(private val injector: ShizukuTouchInjector) {
    private val holding = AtomicBoolean(false)
    fun tap(x: Int, y: Int): Boolean = injector.injectTap(x,y)
    fun down(x: Int,y: Int): Boolean { if (!holding.compareAndSet(false,true)) return false; return injector.injectTouchDown(x,y) }
    fun up(x: Int,y: Int): Boolean { holding.set(false); return injector.injectTouchUp(x,y) }
    fun stop(x: Int = 0,y: Int = 0) { if (holding.get()) up(x,y) }
}
