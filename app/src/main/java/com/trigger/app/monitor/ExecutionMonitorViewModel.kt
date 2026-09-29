package com.trigger.app.monitor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.trigger.automation.engine.ExecutionLogEntry
import com.trigger.automation.engine.ExecutionRuntime
import com.trigger.automation.engine.MacroExecutionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

enum class LogFilter { ALL, SUCCESS, ERROR, WARNING }
data class ExecutionMonitorState(val execution:MacroExecutionState=MacroExecutionState(),val logs:List<ExecutionLogEntry> = emptyList(),val filter:LogFilter=LogFilter.ALL)
class ExecutionMonitorViewModel(application:Application):AndroidViewModel(application) {
    private val filter=MutableStateFlow(LogFilter.ALL); private val mutable=MutableStateFlow(ExecutionMonitorState()); val state:StateFlow<ExecutionMonitorState> = mutable.asStateFlow()
    init { viewModelScope.launch { combine(ExecutionRuntime.state,ExecutionRuntime.logs,filter) { execution,logs,selection -> ExecutionMonitorState(execution,logs.filter { matches(it,selection) },selection) }.collect { mutable.value=it } } }
    fun setFilter(value:LogFilter) { filter.value=value }
    fun stopExecution() { ExecutionRuntime.cancel() }
    fun clearLogs() { ExecutionRuntime.clearLogs() }
    fun exportJson():String = JSONArray().also { array -> state.value.logs.forEach { entry -> array.put(JSONObject().apply { put("timestampMs",entry.timestampMs);put("macroId",entry.macroId);put("stepIndex",entry.stepIndex);put("phase",entry.phase);put("durationMs",entry.durationMs);put("failureReason",entry.failureReason?.name);put("message",entry.message) }) } }.toString(2)
    private fun matches(entry:ExecutionLogEntry,selection:LogFilter)=when(selection) {
        LogFilter.ALL->true
        LogFilter.SUCCESS->entry.phase in setOf("COMPLETE","STEP_END")
        LogFilter.ERROR->entry.failureReason!=null || entry.phase=="FAILED"
        LogFilter.WARNING->entry.phase in setOf("CANCELLED","STEP_SKIPPED")
    }
}
