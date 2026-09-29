package com.trigger.app.monitor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trigger.core.ui.GlassCard
import com.trigger.automation.engine.MacroRunStatus
import java.text.DateFormat
import java.util.Date

@Composable fun ExecutionMonitorScreen(vm:ExecutionMonitorViewModel= viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle(); val context=LocalContext.current
    val createFile=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> if(uri!=null) runCatching { context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(vm.exportJson()) } } }
    val active=state.execution.status==MacroRunStatus.RUNNING || state.execution.status==MacroRunStatus.PAUSED
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Execution monitor",style=MaterialTheme.typography.headlineMedium)
        GlassCard(Modifier.fillMaxWidth().padding(top=12.dp)) {
            Text(if(active) "${state.execution.status}: ${state.execution.macroId ?: "Macro"}" else "No active macro",style=MaterialTheme.typography.titleMedium)
            Text(if(state.execution.totalSteps>0) "Step ${state.execution.currentStepIndex+1} of ${state.execution.totalSteps}" else state.execution.detail ?: "Waiting for execution",style=MaterialTheme.typography.bodySmall)
            LinearProgressIndicator(progress={if(state.execution.totalSteps>0) ((state.execution.currentStepIndex+1).toFloat()/state.execution.totalSteps).coerceIn(0f,1f) else 0f},modifier=Modifier.fillMaxWidth().padding(vertical=10.dp))
            Button(onClick=vm::stopExecution,enabled=active,colors=ButtonDefaults.buttonColors(containerColor=MaterialTheme.colorScheme.error)) { Text("Emergency stop") }
        }
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Text("Execution logs",style=MaterialTheme.typography.titleLarge); TextButton(onClick={createFile.launch("trigger-execution-logs.json")}) { Text("Export JSON") } }
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) { LogFilter.entries.forEach { filter -> FilterChip(selected=filter==state.filter,onClick={vm.setFilter(filter)},label={Text(filter.name)}) } }
        LazyColumn(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(vertical=8.dp)) {
            items(state.logs.reversed(),key={"${it.timestampMs}_${it.stepIndex}_${it.phase}"}) { entry -> GlassCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Text(entry.phase,style=MaterialTheme.typography.labelLarge,color=when { entry.failureReason!=null -> MaterialTheme.colorScheme.error; entry.phase=="COMPLETE" || entry.phase=="STEP_END" -> MaterialTheme.colorScheme.tertiary; else -> MaterialTheme.colorScheme.primary }); Text(DateFormat.getTimeInstance().format(Date(entry.timestampMs)),style=MaterialTheme.typography.labelSmall) }
                Text("${entry.macroId}${entry.stepIndex?.let { " · step ${it+1}" } ?: ""}")
                entry.message?.let { Text(it,style=MaterialTheme.typography.bodySmall) }; entry.durationMs?.let { Text("${it} ms",style=MaterialTheme.typography.labelSmall) }
            } }
        }
    }
}
