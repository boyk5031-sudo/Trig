package com.trigger.automation.engine

import kotlinx.coroutines.Job
import java.util.concurrent.atomic.AtomicBoolean

enum class MacroRunStatus { IDLE, RUNNING, PAUSED, COMPLETED, FAILED, CANCELLED }
enum class ExecutionFailureReason { SERVICE_DISCONNECTED, TIMEOUT, INVALID_STEP, LAUNCH_FAILED, INJECTION_REJECTED, CANCELLED, UNKNOWN }
data class MacroExecutionState(
    val macroId: String? = null,
    val status: MacroRunStatus = MacroRunStatus.IDLE,
    val currentStepIndex: Int = -1,
    val totalSteps: Int = 0,
    val startedAtMs: Long? = null,
    val endedAtMs: Long? = null,
    val failureReason: ExecutionFailureReason? = null,
    val detail: String? = null
)
internal data class ActiveRun(val job: Job, val paused: AtomicBoolean = AtomicBoolean(false))
