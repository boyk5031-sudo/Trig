package com.trigger.automation.engine

import android.content.Context
import android.view.InputEvent

/** Concrete InputEventSink forwarded over AIDL to the Shizuku shell user-service. */
class ShizukuInputEventSink(context: Context) : InputEventSink, AutoCloseable {
    val connection=ShizukuServiceConnection(context)
    override fun inject(event: android.view.MotionEvent): Boolean = connection.inject(event)
    override fun isConnected(): Boolean = connection.state == ShizukuServiceConnection.State.CONNECTED
    fun connect(): Boolean = connection.connect()
    override fun close() = connection.close()
}
