package com.trigger.automation.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Process-wide observable execution snapshot used by dashboard and monitor screens. */
object ExecutionRuntime {
    private val mutableState=MutableStateFlow(MacroExecutionState())
    private val mutableLogs=MutableStateFlow<List<ExecutionLogEntry>>(emptyList())
    val state:StateFlow<MacroExecutionState> = mutableState.asStateFlow()
    val logs:StateFlow<List<ExecutionLogEntry>> = mutableLogs.asStateFlow()
    @Volatile private var currentJob:kotlinx.coroutines.Job?=null
    internal fun track(job:kotlinx.coroutines.Job?) { currentJob=job }
    fun cancel() { currentJob?.cancel() }
    internal fun publish(state:MacroExecutionState) { mutableState.value=state }
    internal fun append(entry:ExecutionLogEntry) { mutableLogs.update { (it+entry).takeLast(1000) } }
    fun clearLogs() { mutableLogs.value=emptyList() }
}
