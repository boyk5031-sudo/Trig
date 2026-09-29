package com.trigger.automation.engine

import android.content.Context
import android.content.Intent
import android.os.Build
import android.view.Surface
import android.view.WindowManager
import com.trigger.feature.macroeditor.model.Macro
import com.trigger.feature.macroeditor.model.MacroStep
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.ceil

/** Sequential, cancellable macro runner. Each physical injection uses the Shizuku event transport. */
class MacroExecutionEngine(
    context: Context,
    private val injector: ShizukuTouchInjector,
    private val transformer: CoordinateTransformer = CoordinateTransformer(context.applicationContext),
    private val stepTimeoutMs: Long = 5_000,
    private val variables: () -> Map<String,String> = { emptyMap() },
    val log: ExecutionLog = ExecutionLog()
) {
    private val app=context.applicationContext
    private val _state=MutableStateFlow(MacroExecutionState())
    val state: StateFlow<MacroExecutionState> = _state.asStateFlow()
    @Volatile private var activeJob: Job?=null
    @Volatile private var pauseGate: CompletableDeferred<Unit>?=null
    private val pointerSequence=AtomicInteger(0)

    @Synchronized fun executeMacro(macro: Macro, scope: CoroutineScope): Job {
        activeJob?.cancel()
        pauseGate=null
        val run=scope.launch(SupervisorJob(scope.coroutineContext[Job]) + Dispatchers.Default) {
            val started=System.currentTimeMillis()
            setState(MacroExecutionState(macro.id,MacroRunStatus.RUNNING,-1,macro.steps.size,started))
            log.record(ExecutionLogEntry(started,macro.id,null,"START",message="Macro started"))
            try {
                repeat(macro.repeatCount.coerceAtLeast(1)) { repeatIndex ->
                    var skipNext=false
                    for ((index,step) in macro.steps.withIndex()) {
                        awaitResume()
                        if(skipNext) { skipNext=false; continue }
                        setState(_state.value.copy(status=MacroRunStatus.RUNNING,currentStepIndex=index))
                        val begin=System.currentTimeMillis()
                        log.record(ExecutionLogEntry(begin,macro.id,index,"STEP_START",message="repeat ${repeatIndex+1}"))
                        try {
                            withTimeout(stepTimeoutMs.coerceAtLeast(1)) {
                                if(step is MacroStep.ConditionStep) {
                                    skipNext=variables()[step.variable] != step.value
                                } else executeStep(step)
                            }
                            log.record(ExecutionLogEntry(System.currentTimeMillis(),macro.id,index,"STEP_END",System.currentTimeMillis()-begin))
                        } catch (timeout: TimeoutCancellationException) {
                            log.record(ExecutionLogEntry(System.currentTimeMillis(),macro.id,index,"STEP_FAILED",System.currentTimeMillis()-begin,ExecutionFailureReason.TIMEOUT,"Step timed out"))
                            throw MacroFailure(ExecutionFailureReason.TIMEOUT,"Step $index exceeded ${stepTimeoutMs}ms")
                        } catch (failure: MacroFailure) {
                            log.record(ExecutionLogEntry(System.currentTimeMillis(),macro.id,index,"STEP_FAILED",System.currentTimeMillis()-begin,failure.reason,failure.message))
                            throw failure
                        }
                    }
                }
                val ended=System.currentTimeMillis(); setState(_state.value.copy(status=MacroRunStatus.COMPLETED,endedAtMs=ended,currentStepIndex=-1)); log.record(ExecutionLogEntry(ended,macro.id,null,"COMPLETE",message="Completed"))
            } catch (cancelled: CancellationException) {
                val ended=System.currentTimeMillis(); setState(_state.value.copy(status=MacroRunStatus.CANCELLED,endedAtMs=ended,failureReason=ExecutionFailureReason.CANCELLED,detail="Cancelled")); log.record(ExecutionLogEntry(ended,macro.id,_state.value.currentStepIndex,"CANCELLED",failureReason=ExecutionFailureReason.CANCELLED)); throw cancelled
            } catch (failure: MacroFailure) {
                val ended=System.currentTimeMillis(); setState(_state.value.copy(status=MacroRunStatus.FAILED,endedAtMs=ended,failureReason=failure.reason,detail=failure.message)); log.record(ExecutionLogEntry(ended,macro.id,_state.value.currentStepIndex,"FAILED",failureReason=failure.reason,message=failure.message))
            } catch (failure: Throwable) {
                val ended=System.currentTimeMillis(); setState(_state.value.copy(status=MacroRunStatus.FAILED,endedAtMs=ended,failureReason=ExecutionFailureReason.UNKNOWN,detail=failure.message)); log.record(ExecutionLogEntry(ended,macro.id,_state.value.currentStepIndex,"FAILED",failureReason=ExecutionFailureReason.UNKNOWN,message=failure.message))
            }
        }
        activeJob=run
        ExecutionRuntime.track(run)
        return run
    }

    @Synchronized fun pause(): Boolean {
        if(activeJob?.isActive!=true || _state.value.status!=MacroRunStatus.RUNNING) return false
        if(pauseGate==null) pauseGate=CompletableDeferred()
        setState(_state.value.copy(status=MacroRunStatus.PAUSED)); return true
    }
    @Synchronized fun resume(): Boolean {
        val gate=pauseGate ?: return false
        pauseGate=null; gate.complete(Unit)
        if(_state.value.status==MacroRunStatus.PAUSED) setState(_state.value.copy(status=MacroRunStatus.RUNNING))
        return true
    }
    @Synchronized fun cancel() { pauseGate?.complete(Unit); pauseGate=null; activeJob?.cancel() }

    private suspend fun awaitResume() { pauseGate?.await() }
    private suspend fun executeStep(step: MacroStep) {
        when(step) {
            is MacroStep.TapStep -> {
                awaitResume(); val (x,y)=toPhysical(step.x,step.y)
                if(!injector.injectTap(nextPointer(),x,y)) throw injectionFailure()
                if(step.delayMs>0) pausableDelay(step.delayMs)
            }
            is MacroStep.SwipeStep -> {
                val (sx,sy)=toPhysical(step.startX,step.startY); val (ex,ey)=toPhysical(step.endX,step.endY)
                val id=nextPointer(); if(!injector.injectTouchDown(id,sx,sy)) throw injectionFailure()
                try {
                    val duration=step.durationMs.coerceAtLeast(1); val frames=ceil(duration/16.0).toInt().coerceIn(1,300)
                    for(frame in 1..frames) {
                        awaitResume(); val fraction=frame.toFloat()/frames
                        val x=sx+(ex-sx)*fraction; val y=sy+(ey-sy)*fraction
                        if(!injector.injectMove(id,x,y)) throw injectionFailure()
                        val wait=(duration/frames).coerceAtLeast(1); pausableDelay(wait)
                    }
                } finally { injector.injectTouchUp(id,ex,ey) }
            }
            is MacroStep.WaitStep -> pausableDelay(step.durationMs.coerceAtLeast(0))
            is MacroStep.LaunchAppStep -> {
                val launch=app.packageManager.getLaunchIntentForPackage(step.targetPackage) ?: throw MacroFailure(ExecutionFailureReason.LAUNCH_FAILED,"Target application is unavailable")
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); app.startActivity(launch)
            }
            is MacroStep.ConditionStep -> Unit
        }
    }
    private suspend fun pausableDelay(duration: Long) { var remaining=duration; while(remaining>0) { awaitResume(); val slice=minOf(remaining,50); delay(slice); remaining-=slice } }
    private fun toPhysical(x:Float,y:Float):Pair<Float,Float> {
        val dm=app.resources.displayMetrics
        return try { val p=transformer.toPhysical(x,y,dm.widthPixels,dm.heightPixels); p.x to p.y } catch (_:Exception) { x to y }
    }
    private fun nextPointer()=pointerSequence.getAndUpdate { (it+1)%10 }
    private fun injectionFailure()=MacroFailure(if(injector.isConnected()) ExecutionFailureReason.INJECTION_REJECTED else ExecutionFailureReason.SERVICE_DISCONNECTED,"Input event could not be injected")
    private fun setState(value: MacroExecutionState) { _state.value=value; ExecutionRuntime.publish(value) }
    private class MacroFailure(val reason: ExecutionFailureReason,message:String): Exception(message)
}
