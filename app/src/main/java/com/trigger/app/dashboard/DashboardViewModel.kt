package com.trigger.app.dashboard

import android.app.Application
import android.app.ActivityManager
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.trigger.app.bridge.SystemActivationBridgeImpl
import com.trigger.feature.gameassistant.activation.ActivationManager
import com.trigger.feature.gameassistant.activation.ActivationMode
import com.trigger.feature.gameassistant.service.GameOverlayService
import com.trigger.feature.gameassistant.util.GameDndController
import com.trigger.automation.engine.ExecutionRuntime
import com.trigger.feature.scheduling.data.ScheduleRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

 data class DashboardState(
    val activationMode:ActivationMode=ActivationMode.UNAVAILABLE,
    val activationReady:Boolean=false,
    val overlayRunning:Boolean=false,
    val autoFire:Boolean=false,
    val dnd:Boolean=false,
    val ramUsedMb:Long=0,
    val ramTotalMb:Long=0,
    val scheduleCount:Int=0,
    val executionName:String?=null,
    val executionStatus:String="IDLE",
    val executionProgress:Float=0f
 )
class DashboardViewModel(application:Application):AndroidViewModel(application) {
    private val app=application
    private val activation=ActivationManager(app,SystemActivationBridgeImpl(app))
    private val dndController=GameDndController(app)
    private val schedules=ScheduleRepository(app)
    private val mutable=MutableStateFlow(DashboardState())
    val state:StateFlow<DashboardState> = mutable.asStateFlow()
    init { viewModelScope.launch { ExecutionRuntime.state.collect { ex -> mutable.value=mutable.value.copy(executionName=ex.macroId,executionStatus=ex.status.name,executionProgress=if(ex.totalSteps>0) ((ex.currentStepIndex+1).toFloat()/ex.totalSteps).coerceIn(0f,1f) else 0f) } }; viewModelScope.launch { while(true) { refresh(); delay(1500) } } }
    private fun refresh() {
        val activationState=activation.detect(); val am=app.getSystemService(android.content.Context.ACTIVITY_SERVICE) as ActivityManager; val memory=ActivityManager.MemoryInfo(); am.getMemoryInfo(memory)
        val used=(memory.totalMem-memory.availMem)/(1024*1024); val total=memory.totalMem/(1024*1024)
        val nm=app.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val dnd=if(Build.VERSION.SDK_INT>=23 && nm.isNotificationPolicyAccessGranted) nm.currentInterruptionFilter!=android.app.NotificationManager.INTERRUPTION_FILTER_ALL else false
        mutable.value=mutable.value.copy(activationMode=activationState.mode,activationReady=activationState.ready,overlayRunning=GameOverlayService.isRunning,autoFire=GameOverlayService.autoFireEnabled,dnd=dnd,ramUsedMb=used,ramTotalMb=total,scheduleCount=schedules.all().count { it.enabled })
    }
    fun toggleOverlay(enabled:Boolean) {
        if (enabled && Build.VERSION.SDK_INT >= 23 && !android.provider.Settings.canDrawOverlays(app)) {
            activation.openOverlaySettings()
            return
        }
        if (enabled) {
            startOverlayCommand(Intent(app, GameOverlayService::class.java).setAction(GameOverlayService.ACTION_START))
        } else {
            // stopService avoids creating a fresh background service just to deliver ACTION_STOP.
            app.stopService(Intent(app, GameOverlayService::class.java))
        }
        mutable.value = mutable.value.copy(overlayRunning = enabled)
    }
    fun toggleAutoFire(enabled:Boolean) { if(enabled && Build.VERSION.SDK_INT>=23 && !android.provider.Settings.canDrawOverlays(app)) { activation.openOverlaySettings(); return }; startOverlayCommand(Intent(app,GameOverlayService::class.java).setAction(GameOverlayService.ACTION_SET_AUTO_FIRE).putExtra(GameOverlayService.EXTRA_AUTO_FIRE_ENABLED,enabled)); mutable.value=mutable.value.copy(autoFire=enabled) }
    fun toggleDnd(enabled:Boolean) { if(enabled && Build.VERSION.SDK_INT>=23 && !android.provider.Settings.canDrawOverlays(app)) { activation.openOverlaySettings(); return }; startOverlayCommand(Intent(app,GameOverlayService::class.java).setAction(GameOverlayService.ACTION_SET_DND).putExtra(GameOverlayService.EXTRA_DND_ENABLED,enabled)); mutable.value=mutable.value.copy(dnd=enabled) }
    private fun startOverlayCommand(intent:Intent) { if(Build.VERSION.SDK_INT>=26) app.startForegroundService(intent) else app.startService(intent) }
}
