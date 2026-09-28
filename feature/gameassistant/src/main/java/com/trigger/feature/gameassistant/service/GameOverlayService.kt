package com.trigger.feature.gameassistant.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.setContent
import androidx.lifecycle.LifecycleService
import com.trigger.feature.gameassistant.hud.CrosshairOverlayView
import com.trigger.feature.gameassistant.ui.GameAssistantOverlayUI
import com.trigger.feature.gameassistant.util.TriggerMacroController
import com.trigger.feature.gameassistant.util.GameDndController
import android.view.View
import java.util.concurrent.atomic.AtomicBoolean

/** Host injects a TriggerEngine-backed dispatcher; keeps touch execution out of the overlay UI. */
interface TouchDispatcher { fun tap(x: Float, y: Float); fun startHold(x: Float, y: Float); fun stop() }
class GameOverlayService : LifecycleService() {
    companion object { const val ACTION_STOP = "com.trigger.gameassistant.STOP"; const val ACTION_START = "com.trigger.gameassistant.START"; const val ACTION_SET_AUTO_FIRE = "com.trigger.gameassistant.AUTO_FIRE"; const val ACTION_SET_DND = "com.trigger.gameassistant.DND"; const val EXTRA_DND_ENABLED = "dnd_enabled"; const val EXTRA_AUTO_FIRE_ENABLED = "auto_fire_enabled"; const val EXTRA_AUTO_FIRE_CPS = "auto_fire_cps"; const val EXTRA_TARGET_PACKAGE = "target_package"; const val EXTRA_TARGET_LABEL = "target_label"; const val EXTRA_TURBO_ENABLED = "turbo_enabled"; const val EXTRA_ORIENTATION = "orientation"; const val CHANNEL = "game_assistant"; @Volatile var touchDispatcher: TouchDispatcher? = null; @Volatile var isRunning: Boolean = false
        private set
        @Volatile var autoFireEnabled: Boolean = false
            private set
        @Volatile var dndEnabled: Boolean = false
            private set }
    private lateinit var wm: WindowManager
    private var dock: View? = null
    private var panel: View? = null
    private var hud: CrosshairOverlayView? = null
    private val expanded = AtomicBoolean(false)
    private var macro: TriggerMacroController? = null
    private lateinit var dndController: GameDndController
    override fun onCreate() { super.onCreate(); wm = getSystemService(WINDOW_SERVICE) as WindowManager; dndController=GameDndController(this); createChannel(); startForeground(44, notification()) }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) { stopSelf(); return START_NOT_STICKY }
        if(Build.VERSION.SDK_INT>=23 && !android.provider.Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY }
        showDock(); showHud(); isRunning=true
        if(intent?.action==ACTION_SET_AUTO_FIRE) onAutoFire(intent.getBooleanExtra(EXTRA_AUTO_FIRE_ENABLED,false),intent.getIntExtra(EXTRA_AUTO_FIRE_CPS,12))
        if(intent?.action==ACTION_SET_DND) { val enabled=intent.getBooleanExtra(EXTRA_DND_ENABLED,false); dndEnabled=if(enabled) dndController.enable() else { dndController.restore(); false } }
        return START_STICKY
    }
    private fun createChannel() { if (Build.VERSION.SDK_INT >= 26) getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL, "Game assistant", NotificationManager.IMPORTANCE_LOW)) }
    private fun notification(): Notification = Notification.Builder(this, CHANNEL).setContentTitle("Game assistant active").setSmallIcon(android.R.drawable.ic_media_play).setOngoing(true).build()
    private fun params(width: Int, height: Int, gravity: Int, touchable: Boolean = true) = WindowManager.LayoutParams(width, height, if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE, (WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or if (!touchable) WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE else 0), PixelFormat.TRANSLUCENT).apply { this.gravity = gravity }
    private fun onAutoFire(enabled: Boolean, cps: Int) {
        autoFireEnabled=enabled
        val controller = macro ?: Companion.touchDispatcher?.let { TriggerMacroController(it).also { c -> macro = c } } ?: return
        if (enabled) { val dm=resources.displayMetrics; controller.startAutoFire(dm.widthPixels/2f, dm.heightPixels/2f, cps) } else controller.stop()
    }
    private fun showDock() { if (dock != null) return
        val view = ComposeView(this).apply { setContent { GameAssistantOverlayUI(onExpand = { togglePanel() }, expanded = false, onClose = { stopSelf() }, onAutoFire = ::onAutoFire) } }
        dock = view; wm.addView(view, params(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, Gravity.END or Gravity.CENTER_VERTICAL))
    }
    private fun togglePanel() { if (expanded.getAndSet(!expanded.get())) { panel?.let { wm.removeView(it) }; panel = null } else {
        val view = ComposeView(this).apply { setContent { GameAssistantOverlayUI(onExpand = { togglePanel() }, expanded = true, onClose = { stopSelf() }, onAutoFire = ::onAutoFire) } }; panel = view
        wm.addView(view, params(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, Gravity.CENTER))
    } }
    private fun showHud() { if (hud != null) return; hud = CrosshairOverlayView(this); wm.addView(hud, params(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, Gravity.TOP or Gravity.START, false)) }
    override fun onDestroy() { isRunning=false; autoFireEnabled=false; dndEnabled=false; dndController.restore(); macro?.close(); macro=null; dock?.let { wm.removeView(it) }; panel?.let { wm.removeView(it) }; hud?.let { wm.removeView(it) }; super.onDestroy() }
    override fun onBind(intent: Intent): IBinder? = super.onBind(intent)
}
