package com.trigger.app.dashboard

import android.app.Application
import android.app.ActivityManager
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.trigger.app.bridge.SystemActivationBridgeImpl
import com.trigger.feature.gameassistant.activation.ActivationManager
import com.trigger.feature.gameassistant.activation.ActivationMode
import com.trigger.feature.gameassistant.service.GameOverlayService
import com.trigger.automation.engine.ExecutionRuntime
import com.trigger.feature.scheduling.data.ScheduleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow

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
    private val schedules=ScheduleRepository(app)
    private val mutable = MutableStateFlow(DashboardState())
    // Poll only while a lifecycle-aware UI subscriber exists; preserve immediate toggle feedback.
    val state: StateFlow<DashboardState> = channelFlow {
        launch {
            while (true) {
                mutable.value = withContext(Dispatchers.IO) { readStatus() }
                delay(1500)
            }
        }
        combine(mutable, ExecutionRuntime.state) { status, ex ->
            status.copy(executionName=ex.macroId, executionStatus=ex.status.name,
                executionProgress=if(ex.totalSteps>0) ((ex.currentStepIndex+1).toFloat()/ex.totalSteps).coerceIn(0f,1f) else 0f)
        }.collect { send(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(0), DashboardState())

    private fun readStatus(): DashboardState {
        val activationState=activation.detect()
        val am=app.getSystemService(android.content.Context.ACTIVITY_SERVICE) as ActivityManager
        val memory=ActivityManager.MemoryInfo().also(am::getMemoryInfo)
        val nm=app.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val dnd=nm.isNotificationPolicyAccessGranted && nm.currentInterruptionFilter!=android.app.NotificationManager.INTERRUPTION_FILTER_ALL
        return DashboardState(activationMode=activationState.mode, activationReady=activationState.ready,
            overlayRunning=GameOverlayService.isRunning, autoFire=GameOverlayService.autoFireEnabled, dnd=dnd,
            ramUsedMb=(memory.totalMem-memory.availMem)/(1024*1024), ramTotalMb=memory.totalMem/(1024*1024),
            scheduleCount=schedules.all().count { it.enabled })
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
        mutable.update { it.copy(overlayRunning=enabled) }
    }
    fun toggleAutoFire(enabled:Boolean) { if(enabled && Build.VERSION.SDK_INT>=23 && !android.provider.Settings.canDrawOverlays(app)) { activation.openOverlaySettings(); return }; startOverlayCommand(Intent(app,GameOverlayService::class.java).setAction(GameOverlayService.ACTION_SET_AUTO_FIRE).putExtra(GameOverlayService.EXTRA_AUTO_FIRE_ENABLED,enabled)); mutable.update { it.copy(autoFire=enabled) } }
    fun toggleDnd(enabled:Boolean) { if(enabled && Build.VERSION.SDK_INT>=23 && !android.provider.Settings.canDrawOverlays(app)) { activation.openOverlaySettings(); return }; startOverlayCommand(Intent(app,GameOverlayService::class.java).setAction(GameOverlayService.ACTION_SET_DND).putExtra(GameOverlayService.EXTRA_DND_ENABLED,enabled)); mutable.update { it.copy(dnd=enabled) } }
    private fun startOverlayCommand(intent:Intent) { if(intent.action==GameOverlayService.ACTION_START || intent.getBooleanExtra(GameOverlayService.EXTRA_AUTO_FIRE_ENABLED,false)) com.trigger.app.service.GameServiceBinder(app).prepareActivation(); if(Build.VERSION.SDK_INT>=26) app.startForegroundService(intent) else app.startService(intent) }
}
