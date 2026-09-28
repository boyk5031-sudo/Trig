package com.trigger.app.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.trigger.feature.gameassistant.service.GameOverlayService
import com.trigger.app.bridge.GameAssistantTouchBridgeImpl
import com.trigger.automation.engine.TriggerEngine
import com.trigger.automation.engine.ShizukuTouchInjector
import com.trigger.automation.engine.ShizukuInputEventSink

/** Starts and binds the foreground overlay with target metadata and the user's turbo preset. */
class GameServiceBinder(private val context: Context) {
    private var bound = false
    private var inputSink: ShizukuInputEventSink? = null
    private val connection = object : ServiceConnection { override fun onServiceConnected(name: ComponentName?, service: IBinder?) { bound=true }; override fun onServiceDisconnected(name: ComponentName?) { bound=false } }
    fun prepareActivation() { if(inputSink==null) { inputSink=ShizukuInputEventSink(context).also { ShizukuTouchInjector.installSink(it); it.connect() } }; GameOverlayService.touchDispatcher = GameAssistantTouchBridgeImpl(context, TriggerEngine(ShizukuTouchInjector())) }
    fun launch(packageName: String, label: String, turbo: Boolean) {
        prepareActivation()
        val intent=Intent(context,GameOverlayService::class.java).setAction(GameOverlayService.ACTION_START)
            .putExtra(GameOverlayService.EXTRA_TARGET_PACKAGE,packageName).putExtra(GameOverlayService.EXTRA_TARGET_LABEL,label)
            .putExtra(GameOverlayService.EXTRA_TURBO_ENABLED,turbo).putExtra(GameOverlayService.EXTRA_ORIENTATION,context.resources.configuration.orientation)
        if(Build.VERSION.SDK_INT>=26) context.startForegroundService(intent) else context.startService(intent)
        if(!bound) try { context.bindService(intent,connection,Context.BIND_AUTO_CREATE) } catch (_:Exception) { }
    }
    fun stop() { context.stopService(Intent(context,GameOverlayService::class.java)); ShizukuTouchInjector.clearSink(inputSink); inputSink?.close(); inputSink=null; if(bound) { try { context.unbindService(connection) } catch (_:IllegalArgumentException) {}; bound=false } }
}
