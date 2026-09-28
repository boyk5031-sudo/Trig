package com.trigger.app.bridge

import android.content.Context
import android.view.View
import com.trigger.automation.engine.CoordinateTransformer
import com.trigger.automation.engine.TriggerEngine
import com.trigger.feature.gameassistant.service.TouchDispatcher

/** Converts View-local input locations to physical pixels before dispatch through TriggerEngine. */
class GameAssistantTouchBridgeImpl(context: Context, private val engine: TriggerEngine) : TouchDispatcher {
    private val transformer = CoordinateTransformer(context.applicationContext)
    @Volatile private var lastX = 0; @Volatile private var lastY = 0
    fun tapFromView(view: View, x: Float, y: Float): Boolean {
        val point = transformer.toPhysical(x,y,view.width,view.height); lastX=point.x.toInt(); lastY=point.y.toInt(); return engine.tap(lastX,lastY)
    }
    override fun tap(x: Float, y: Float) { lastX=x.toInt(); lastY=y.toInt(); engine.tap(lastX,lastY) }
    override fun startHold(x: Float, y: Float) { lastX=x.toInt(); lastY=y.toInt(); engine.down(lastX,lastY) }
    override fun stop() { engine.stop(lastX,lastY) }
}
