package com.trigger.app.navigation

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.trigger.app.dashboard.DashboardScreen
import com.trigger.app.launcher.GameLauncherScreen
import com.trigger.app.monitor.ExecutionMonitorScreen
import com.trigger.app.schedule.ScheduleScreen
import com.trigger.feature.settings.ComplianceCenterScreen
import com.trigger.feature.settings.DisclosureAccess
import com.trigger.feature.settings.ProminentDisclosureDialog
import com.trigger.automation.engine.ShizukuTouchInjector
import com.trigger.core.ui.GlassCard
import com.trigger.feature.gameassistant.activation.ActivationManager
import com.trigger.feature.gameassistant.service.GameOverlayService
import com.trigger.feature.macroeditor.model.MacroStep
import com.trigger.feature.macroeditor.ui.MacroEditorScreen
import kotlinx.serialization.Serializable
import kotlinx.coroutines.delay

@Serializable data object DashboardRoute
@Serializable data object GameAssistantRoute
@Serializable data object MacroListRoute
@Serializable data class MacroEditorRoute(val macroId:String)
@Serializable data object ScheduleRoute
@Serializable data object ExecutionMonitorRoute
@Serializable data object PermissionCenterRoute
@Serializable data object PrivacyCenterRoute

@Composable fun TriggerNavGraph(serviceBinder:com.trigger.app.service.GameServiceBinder,navController:NavHostController= rememberNavController()) {
    val context= LocalContext.current; val catalog= remember { MacroCatalog(context) }
    Scaffold(bottomBar={ NavigationBar {
        val entry by navController.currentBackStackEntryAsState(); val current=entry?.destination?.route.orEmpty()
        listOf("Home" to DashboardRoute,"Games" to GameAssistantRoute,"Macros" to MacroListRoute,"Schedule" to ScheduleRoute,"Monitor" to ExecutionMonitorRoute,"Access" to PermissionCenterRoute,"Privacy" to PrivacyCenterRoute).forEach { (label,route) ->
            val selected=current.contains(route::class.simpleName.orEmpty(),ignoreCase=true)
            NavigationBarItem(selected=selected,onClick={navController.navigate(route) { launchSingleTop=true; restoreState=true }},icon={Text(label.take(1))},label={Text(label)})
        }
    } }) { padding ->
        NavHost(navController,startDestination=DashboardRoute,modifier=Modifier.padding(padding)) {
            composable<DashboardRoute> { DashboardScreen(onOpenGames={navController.navigate(GameAssistantRoute)},onOpenMacros={navController.navigate(MacroListRoute)},onOpenSchedules={navController.navigate(ScheduleRoute)},onOpenMonitor={navController.navigate(ExecutionMonitorRoute)},onOpenPermissions={navController.navigate(PermissionCenterRoute)}) }
            composable<GameAssistantRoute> { GameLauncherScreen(context) { app,turbo -> serviceBinder.launch(app.packageName,app.loadLabel(context.packageManager).toString(),turbo); context.packageManager.getLaunchIntentForPackage(app.packageName)?.let { context.startActivity(it) } } }
            composable<MacroListRoute> {
                var macros by remember { mutableStateOf(catalog.all()) }
                val currentEntry by navController.currentBackStackEntryAsState()
                LaunchedEffect(currentEntry?.destination?.route) { if(currentEntry?.destination?.route?.contains("MacroListRoute")==true) macros=catalog.all() }
                Column(Modifier.fillMaxSize().padding(18.dp)) {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Text("Macros",style=MaterialTheme.typography.headlineMedium); Button(onClick={val created=catalog.create();navController.navigate(MacroEditorRoute(created.id))}) { Text("New") } }
                    LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(vertical=12.dp)) { items(macros,key={it.id}) { macro -> GlassCard(Modifier.fillMaxWidth().clickable { navController.navigate(MacroEditorRoute(macro.id)) }) { Text(macro.name,style=MaterialTheme.typography.titleMedium);Text("${macro.steps.size} steps · ${macro.packageName.ifBlank { "No target app" }}") } } }
                }
            }
            composable<MacroEditorRoute> { entry ->
                val route=entry.toRoute<MacroEditorRoute>(); val macro=catalog.get(route.macroId) ?: catalog.create()
                MacroEditorScreen(onTestStep={step -> testStep(step,context,serviceBinder)},initialMacro=macro,onSaveMacro=catalog::save)
            }
            composable<ScheduleRoute> { ScheduleScreen() }
            composable<ExecutionMonitorRoute> { ExecutionMonitorScreen() }
            composable<PermissionCenterRoute> { PermissionCenterScreen() }
            composable<PrivacyCenterRoute> { ComplianceCenterScreen() }
        }
    }
}

@Composable private fun PermissionCenterScreen() {
    val context= LocalContext.current; val manager=remember { ActivationManager(context,com.trigger.app.bridge.SystemActivationBridgeImpl(context)) }; var refresh by remember { mutableIntStateOf(0) }
    val micPermission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }
    var disclosure by remember { mutableStateOf<DisclosureAccess?>(null) }
    fun openDisclosure(access:DisclosureAccess) { when(access) { DisclosureAccess.OVERLAY->manager.openOverlaySettings();DisclosureAccess.MICROPHONE->micPermission.launch(Manifest.permission.RECORD_AUDIO);DisclosureAccess.DO_NOT_DISTURB->manager.openDndSettings();DisclosureAccess.USAGE_STATS->manager.openUsageSettings();DisclosureAccess.BATTERY_OPTIMIZATION->manager.openBatterySettings() } }
    LaunchedEffect(Unit) { while(true) { refresh++;delay(1200) } }; val permissions=remember(refresh) { manager.requiredAccess() }; val activation=remember(refresh) { manager.detect() }
    Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("Permission center",style=MaterialTheme.typography.headlineMedium);Text("Privilege state is rechecked while this screen is open.")
        permissions.forEach { (access,granted) -> GlassCard(Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Text(access.name.replace('_',' ')); Text(if(granted) "Granted" else "Missing",color=if(granted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error) }
            if(!granted) TextButton(onClick={disclosure=when(access) { com.trigger.feature.gameassistant.activation.RequiredAccess.OVERLAY->DisclosureAccess.OVERLAY;com.trigger.feature.gameassistant.activation.RequiredAccess.BATTERY_EXEMPTION->DisclosureAccess.BATTERY_OPTIMIZATION;com.trigger.feature.gameassistant.activation.RequiredAccess.DND->DisclosureAccess.DO_NOT_DISTURB;com.trigger.feature.gameassistant.activation.RequiredAccess.MICROPHONE->DisclosureAccess.MICROPHONE;com.trigger.feature.gameassistant.activation.RequiredAccess.USAGE_STATS->DisclosureAccess.USAGE_STATS }}) { Text("Review & open settings") }
        } }
        GlassCard(Modifier.fillMaxWidth()) { Text("Activation: ${activation.mode}");Text(activation.message);TextButton(onClick={manager.openDeveloperOptions()}) { Text("Developer options / Wireless debugging") } }
    }
    disclosure?.let { selected -> ProminentDisclosureDialog(selected,onContinue={disclosure=null;openDisclosure(selected)},onDismiss={disclosure=null}) }
}
private fun testStep(step:MacroStep,context:android.content.Context,binder:com.trigger.app.service.GameServiceBinder) {
    binder.prepareActivation(); val injector=ShizukuTouchInjector()
    when(step) {
        is MacroStep.TapStep -> injector.injectTap(step.x.toInt(),step.y.toInt())
        is MacroStep.SwipeStep -> injector.injectDrag(0,step.startX,step.startY,step.endX,step.endY,step.durationMs)
        is MacroStep.LaunchAppStep -> context.packageManager.getLaunchIntentForPackage(step.targetPackage)?.let { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);context.startActivity(it) }
        is MacroStep.WaitStep, is MacroStep.ConditionStep -> Unit
    }
}
