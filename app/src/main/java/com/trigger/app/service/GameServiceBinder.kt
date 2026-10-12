package com.trigger.app.service

import android.content.Context
import android.content.Intent
import android.os.Build
import com.trigger.feature.gameassistant.service.GameOverlayService
import com.trigger.app.bridge.GameAssistantTouchBridgeImpl
import com.trigger.automation.engine.TriggerEngine
import com.trigger.automation.engine.ShizukuTouchInjector
import com.trigger.automation.engine.ShizukuInputEventSink

/** Input transport is created on demand, not at app launch or on every rotation. */
class GameServiceBinder(context: Context) {
    private val app = context.applicationContext

    fun prepareActivation() {
        if (GameOverlayService.touchDispatcher != null) return
        val sink = ShizukuInputEventSink(app)
        val injector = ShizukuTouchInjector(sink)
        ShizukuTouchInjector.installSink(sink)
        GameOverlayService.touchDispatcher = GameAssistantTouchBridgeImpl(app, TriggerEngine(injector)) {
            injector.close()
            ShizukuTouchInjector.clearSink(sink)
            sink.close()
        }
        sink.connect()
    }

    fun launch(packageName: String, label: String, turbo: Boolean) {
        if (!turbo) return
        prepareActivation()
        val intent=Intent(app,GameOverlayService::class.java).setAction(GameOverlayService.ACTION_START)
            .putExtra(GameOverlayService.EXTRA_TARGET_PACKAGE,packageName).putExtra(GameOverlayService.EXTRA_TARGET_LABEL,label)
            .putExtra(GameOverlayService.EXTRA_TURBO_ENABLED,turbo).putExtra(GameOverlayService.EXTRA_ORIENTATION,app.resources.configuration.orientation)
        try {
            if(Build.VERSION.SDK_INT>=26) app.startForegroundService(intent) else app.startService(intent)
        } catch (failure: RuntimeException) {
            releaseIfIdle()
            throw failure
        }
    }

    // An active overlay intentionally outlives the Activity. Its onDestroy owns cleanup.
    fun releaseIfIdle() {
        if (!GameOverlayService.isRunning) {
            GameOverlayService.touchDispatcher?.close()
            GameOverlayService.touchDispatcher = null
        }
    }

    fun stop() {
        app.stopService(Intent(app,GameOverlayService::class.java))
        releaseIfIdle()
    }
}
