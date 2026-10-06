package com.trigger.feature.gameassistant.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.content.pm.ServiceInfo
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.setContent
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.trigger.feature.gameassistant.hud.CrosshairOverlayView
import com.trigger.feature.gameassistant.ui.GameAssistantOverlayUI
import com.trigger.feature.gameassistant.util.GameDndController
import com.trigger.feature.gameassistant.util.TriggerMacroController
import java.util.concurrent.atomic.AtomicBoolean

interface TouchDispatcher {
    fun tap(x: Float, y: Float)
    fun startHold(x: Float, y: Float)
    fun stop()
}

class GameOverlayService : LifecycleService() {
    companion object {
        const val ACTION_STOP = "com.trigger.gameassistant.STOP"
        const val ACTION_START = "com.trigger.gameassistant.START"
        const val ACTION_SET_AUTO_FIRE = "com.trigger.gameassistant.AUTO_FIRE"
        const val ACTION_SET_DND = "com.trigger.gameassistant.DND"
        const val EXTRA_DND_ENABLED = "dnd_enabled"
        const val EXTRA_AUTO_FIRE_ENABLED = "auto_fire_enabled"
        const val EXTRA_AUTO_FIRE_CPS = "auto_fire_cps"
        const val EXTRA_TARGET_PACKAGE = "target_package"
        const val EXTRA_TARGET_LABEL = "target_label"
        const val EXTRA_TURBO_ENABLED = "turbo_enabled"
        const val EXTRA_ORIENTATION = "orientation"
        const val CHANNEL = "game_assistant"
        private const val NOTIFICATION_ID = 44
        private const val DEFAULT_CPS = 12

        @Volatile var touchDispatcher: TouchDispatcher? = null
        @Volatile var isRunning: Boolean = false
            private set
        @Volatile var autoFireEnabled: Boolean = false
            private set
        @Volatile var dndEnabled: Boolean = false
            private set
    }

    private lateinit var windowManager: WindowManager
    private lateinit var dndController: GameDndController
    private var dock: View? = null
    private var panel: View? = null
    private var hud: CrosshairOverlayView? = null
    private val expanded = AtomicBoolean(false)
    private var macro: TriggerMacroController? = null
    private lateinit var composeSavedStateOwner: SavedStateRegistryOwner

    private inner class ServiceSavedStateOwner : SavedStateRegistryOwner {
        private val controller = SavedStateRegistryController.create(this)

        override val lifecycle
            get() = this@GameOverlayService.lifecycle
        override val savedStateRegistry: SavedStateRegistry
            get() = controller.savedStateRegistry

        init {
            controller.performAttach()
            controller.performRestore(null)
        }
    }

    override fun onCreate() {
        // Create/attach the registry while LifecycleService is still INITIALIZED. ComposeView
        // requires both lifecycle and saved-state owners when it is attached to WindowManager.
        composeSavedStateOwner = ServiceSavedStateOwner()
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        dndController = GameDndController(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !android.provider.Settings.canDrawOverlays(this)) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        // Promote only for real work. Starting the service with ACTION_STOP must not start a
        // foreground service (which can crash when the app is in the background on Android 12+).
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(
                    NOTIFICATION_ID,
                    buildNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, buildNotification())
            }
        } catch (_: SecurityException) {
            stopSelf(startId)
            return START_NOT_STICKY
        } catch (_: RuntimeException) {
            // Covers foreground-service start restrictions/type errors on Android 12+.
            stopSelf(startId)
            return START_NOT_STICKY
        }
        if (!showDock() || !showHud()) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        isRunning = true

        when (intent?.action) {
            ACTION_SET_AUTO_FIRE -> onAutoFire(
                intent.getBooleanExtra(EXTRA_AUTO_FIRE_ENABLED, false),
                intent.getIntExtra(EXTRA_AUTO_FIRE_CPS, DEFAULT_CPS)
            )
            ACTION_SET_DND -> {
                val enabled = intent.getBooleanExtra(EXTRA_DND_ENABLED, false)
                dndEnabled = try {
                    if (enabled) dndController.enable() else {
                        dndController.restore()
                        false
                    }
                } catch (_: SecurityException) {
                    false
                } catch (_: IllegalStateException) {
                    false
                }
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL, "Game assistant", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification = Notification.Builder(this, CHANNEL)
        .setContentTitle("Game assistant active")
        .setSmallIcon(android.R.drawable.ic_media_play)
        .setOngoing(true)
        .build()

    private fun createComposeView(content: @Composable () -> Unit): ComposeView = ComposeView(this).apply {
        // A Service has no Activity decor view. Explicitly provide the LifecycleOwner so ComposeView
        // can create/dispose its composition safely when attached directly to WindowManager.
        setViewTreeLifecycleOwner(this@GameOverlayService)
        setViewTreeSavedStateRegistryOwner(composeSavedStateOwner)
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent(content)
    }

    private fun showDock(): Boolean {
        if (dock != null) return true
        val view = createComposeView {
            GameAssistantOverlayUI(
                onExpand = { togglePanel() },
                expanded = false,
                onClose = { stopSelf() },
                onAutoFire = ::onAutoFire
            )
        }
        if (!addOverlay(view, overlayParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, Gravity.END or Gravity.CENTER_VERTICAL))) {
            return false
        }
        dock = view
        return true
    }

    private fun togglePanel() {
        if (expanded.getAndSet(!expanded.get())) {
            removeOverlay(panel)
            panel = null
            return
        }
        val view = createComposeView {
            GameAssistantOverlayUI(
                onExpand = { togglePanel() },
                expanded = true,
                onClose = { stopSelf() },
                onAutoFire = ::onAutoFire
            )
        }
        if (addOverlay(view, overlayParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, Gravity.CENTER))) {
            panel = view
        } else {
            expanded.set(false)
        }
    }

    private fun showHud(): Boolean {
        if (hud != null) return true
        val view = CrosshairOverlayView(this)
        if (!addOverlay(view, overlayParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, Gravity.TOP or Gravity.START, touchable = false))) {
            return false
        }
        hud = view
        return true
    }

    private fun overlayParams(width: Int, height: Int, gravity: Int, touchable: Boolean = true): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        if (!touchable) flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        return WindowManager.LayoutParams(width, height, type, flags, PixelFormat.TRANSLUCENT).apply {
            this.gravity = gravity
        }
    }

    private fun addOverlay(view: View, params: WindowManager.LayoutParams): Boolean = try {
        windowManager.addView(view, params)
        true
    } catch (_: SecurityException) {
        false
    } catch (_: WindowManager.BadTokenException) {
        false
    } catch (_: RuntimeException) {
        // WindowManager can reject stale tokens or unavailable displays during teardown.
        false
    }

    private fun removeOverlay(view: View?) {
        if (view == null) return
        try {
            windowManager.removeView(view)
        } catch (_: RuntimeException) {
            // Window already detached or WindowManager is shutting down; teardown stays idempotent.
        }
    }

    private fun onAutoFire(enabled: Boolean, cps: Int) {
        autoFireEnabled = enabled
        val controller = macro ?: touchDispatcher?.let { dispatcher ->
            TriggerMacroController(dispatcher).also { macro = it }
        } ?: return
        if (enabled) {
            val metrics = resources.displayMetrics
            controller.startAutoFire(metrics.widthPixels / 2f, metrics.heightPixels / 2f, cps)
        } else {
            controller.stop()
        }
    }

    override fun onDestroy() {
        isRunning = false
        autoFireEnabled = false
        dndEnabled = false
        try {
            dndController.restore()
        } catch (_: RuntimeException) {
            // Access may have been revoked while the service was active.
        }
        macro?.close()
        macro = null
        removeOverlay(panel)
        removeOverlay(dock)
        removeOverlay(hud)
        panel = null
        dock = null
        hud = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? = super.onBind(intent)

}
