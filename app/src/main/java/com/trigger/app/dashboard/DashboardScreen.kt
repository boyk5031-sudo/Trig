package com.trigger.app.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trigger.core.ui.GlassCard
import com.trigger.core.ui.StatusIndicator

@Composable fun DashboardScreen(
    onOpenGames:()->Unit,onOpenMacros:()->Unit,onOpenSchedules:()->Unit,onOpenMonitor:()->Unit,onOpenPermissions:()->Unit,
    vm:DashboardViewModel= viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Text("Trigger",style=MaterialTheme.typography.headlineLarge); Text("Game automation at a glance",color=MaterialTheme.colorScheme.onSurfaceVariant)
        GlassCard(Modifier.fillMaxWidth()) {
            Text("SYSTEM STATUS",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary); Spacer(Modifier.height(12.dp))
            StatusIndicator("Shizuku / privilege engine",state.activationReady,"${state.activationMode}${if(state.activationReady) " · ready" else " · setup required"}")
            Spacer(Modifier.height(12.dp)); StatusIndicator("Overlay service",state.overlayRunning,if(state.overlayRunning) "Running" else "Stopped")
            Spacer(Modifier.height(12.dp)); StatusIndicator("Execution engine",state.executionStatus=="RUNNING" || state.executionStatus=="PAUSED",state.executionStatus)
        }
        Text("Quick actions",style=MaterialTheme.typography.titleLarge)
        GlassCard(Modifier.fillMaxWidth()) {
            ActionToggle("Game overlay",state.overlayRunning,vm::toggleOverlay)
            ActionToggle("Auto-fire trigger",state.autoFire,vm::toggleAutoFire)
            ActionToggle("Game Do Not Disturb",state.dnd,vm::toggleDnd)
        }
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            GlassCard(Modifier.weight(1f)) { Text("MEMORY",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary); Text("${state.ramUsedMb} MB",style=MaterialTheme.typography.headlineSmall); Text("of ${state.ramTotalMb} MB",color=MaterialTheme.colorScheme.onSurfaceVariant) }
            GlassCard(Modifier.weight(1f)) { Text("SCHEDULES",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary); Text("${state.scheduleCount}",style=MaterialTheme.typography.headlineSmall); Text("active",color=MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        GlassCard(Modifier.fillMaxWidth()) { Text("RECENT EXECUTION",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary); Text(state.executionName ?: "No macro run yet",style=MaterialTheme.typography.titleMedium); Text("${state.executionStatus} · ${(state.executionProgress*100).toInt()}%",color=MaterialTheme.colorScheme.onSurfaceVariant); LinearProgressIndicator(progress={state.executionProgress},modifier=Modifier.fillMaxWidth().padding(top=8.dp)) }
        Text("Workspace",style=MaterialTheme.typography.titleLarge)
        Button(onClick=onOpenGames,modifier=Modifier.fillMaxWidth()) { Text("Game assistant") }
        OutlinedButton(onClick=onOpenMacros,modifier=Modifier.fillMaxWidth()) { Text("Macro editor") }
        OutlinedButton(onClick=onOpenSchedules,modifier=Modifier.fillMaxWidth()) { Text("Schedules") }
        OutlinedButton(onClick=onOpenMonitor,modifier=Modifier.fillMaxWidth()) { Text("Execution monitor") }
        OutlinedButton(onClick=onOpenPermissions,modifier=Modifier.fillMaxWidth()) { Text("Permission center") }
    }
}
@Composable private fun ActionToggle(label:String,checked:Boolean,onChanged:(Boolean)->Unit) { Row(Modifier.fillMaxWidth().padding(vertical=4.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) { Text(label); Switch(checked,onChanged) } }
