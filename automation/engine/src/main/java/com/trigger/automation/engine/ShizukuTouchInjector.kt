package com.trigger.automation.engine

import android.view.InputDevice
import android.view.MotionEvent
import java.util.LinkedHashMap
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.math.max

/** A transport implemented by a Shizuku user-service running as shell (UID 2000).
 * Its implementation must forward MotionEvent through IInputManager.injectInputEvent(event, ASYNC).
 * Keeping Binder transport separate avoids invoking hidden input APIs from the app UID.
 */
fun interface InputEventSink { fun inject(event: MotionEvent): Boolean; fun isConnected(): Boolean = true }
private object DisconnectedInputEventSink : InputEventSink { override fun inject(event: MotionEvent)=false; override fun isConnected()=false }
data class PointerState(val pointerId: Int, var x: Float, var y: Float, val downTime: Long)

/** Thread-safe pointer lifecycle; each emitted event has consistent pointer arrays and timing. */
class ShizukuTouchInjector(
    private val sink: InputEventSink = installedSink ?: DisconnectedInputEventSink,
    private val executor: Executor = Executors.newSingleThreadExecutor()
) : AutoCloseable {
    private val pointers = LinkedHashMap<Int, PointerState>()
    private var gestureDownTime = 0L
    private var closed = false
    private val supportedIds = 0..9
    companion object { @Volatile private var installedSink: InputEventSink? = null; fun installSink(sink: InputEventSink) { installedSink = sink }; fun clearSink(sink: InputEventSink? = null) { if(sink==null || installedSink===sink) installedSink=null } }

    fun injectTouchDown(x: Int, y: Int): Boolean = injectTouchDown(0,x.toFloat(),y.toFloat())
    fun injectTouchUp(x: Int, y: Int): Boolean = injectTouchUp(0,x.toFloat(),y.toFloat())
    fun injectTap(x: Int, y: Int): Boolean = injectTap(0,x.toFloat(),y.toFloat())

    fun isConnected(): Boolean = sink.isConnected()
    @Synchronized fun activePointers(): Map<Int, PointerState> = pointers.mapValues { it.value.copy() }

    fun injectTouchDown(pointerId: Int, x: Float, y: Float): Boolean = synchronized(this) {
        if (closed || pointerId !in supportedIds || pointerId in pointers) return false
        if (pointers.size >= 10) return false
        val now = SystemClockCompat.uptimeMillis()
        if (pointers.isEmpty()) gestureDownTime = now
        pointers[pointerId] = PointerState(pointerId, x, y, gestureDownTime)
        val action = if (pointers.size == 1) MotionEvent.ACTION_DOWN else MotionEvent.ACTION_POINTER_DOWN or ((pointers.size - 1) shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
        dispatchLocked(action, now)
    }

    fun injectMove(pointerId: Int, x: Float, y: Float): Boolean = synchronized(this) {
        if (closed) return false
        val state = pointers[pointerId] ?: return false
        state.x=x; state.y=y
        dispatchLocked(MotionEvent.ACTION_MOVE, SystemClockCompat.uptimeMillis())
    }

    fun injectTouchUp(pointerId: Int, x: Float? = null, y: Float? = null): Boolean = synchronized(this) {
        if (closed) return false
        val state = pointers[pointerId] ?: return false
        if (x != null) state.x=x; if (y != null) state.y=y
        val index=pointers.keys.indexOf(pointerId)
        val action=if (pointers.size == 1) MotionEvent.ACTION_UP else MotionEvent.ACTION_POINTER_UP or (index shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
        val result=dispatchLocked(action,SystemClockCompat.uptimeMillis())
        pointers.remove(pointerId)
        if (pointers.isEmpty()) gestureDownTime=0L
        result
    }

    fun injectTap(pointerId: Int, x: Float, y: Float): Boolean {
        val down=injectTouchDown(pointerId,x,y)
        val up=injectTouchUp(pointerId,x,y)
        return down && up
    }

    fun injectDrag(pointerId: Int, startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long, frameIntervalMs: Long = 8): Boolean {
        if (!injectTouchDown(pointerId,startX,startY)) return false
        val start=SystemClockCompat.uptimeMillis(); val duration=durationMs.coerceAtLeast(1)
        // Frame scheduling is asynchronous; callers may invoke injectMove themselves for custom pacing.
        executor.execute {
            try {
                var elapsed=0L
                while (elapsed < duration) {
                    elapsed=minOf(duration,elapsed+frameIntervalMs.coerceAtLeast(1))
                    val f=elapsed.toFloat()/duration
                    injectMove(pointerId,startX+(endX-startX)*f,startY+(endY-startY)*f)
                    val wait=(start+elapsed-SystemClockCompat.uptimeMillis()).coerceAtLeast(0)
                    if(wait>0) Thread.sleep(wait)
                }
            } catch (_: InterruptedException) { Thread.currentThread().interrupt() }
            finally { injectTouchUp(pointerId,endX,endY) }
        }
        return true
    }

    private fun dispatchLocked(action: Int, eventTime: Long): Boolean {
        if (pointers.isEmpty()) return false
        val properties=Array(pointers.size) { MotionEvent.PointerProperties() }
        val coords=Array(pointers.size) { MotionEvent.PointerCoords() }
        pointers.values.forEachIndexed { i, p -> properties[i].apply { id=p.pointerId; toolType=MotionEvent.TOOL_TYPE_FINGER }; coords[i].apply { x=p.x; y=p.y; pressure=1f; size=1f } }
        val event=MotionEvent.obtain(gestureDownTime,eventTime,action,pointers.size,properties,coords,0,0,1f,1f,0,0,InputDevice.SOURCE_TOUCHSCREEN,0)
        return try { sink.inject(event) } finally { event.recycle() }
    }

    @Synchronized override fun close() { if(closed) return; closed=true
        while(pointers.isNotEmpty()) { val id=pointers.keys.last(); dispatchLocked(if(pointers.size==1) MotionEvent.ACTION_UP else MotionEvent.ACTION_POINTER_UP or ((pointers.size-1) shl MotionEvent.ACTION_POINTER_INDEX_SHIFT),SystemClockCompat.uptimeMillis()); pointers.remove(id) }
    }
}
private object SystemClockCompat { fun uptimeMillis()=android.os.SystemClock.uptimeMillis() }
