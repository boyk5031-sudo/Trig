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
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
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
import com.trigger.feature.macroeditor.serialization.MacroSerializer
import com.trigger.feature.macroeditor.ui.MacroEditorScreen
import kotlinx.serialization.Serializable
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import com.trigger.feature.macroeditor.model.Macro

@Serializable data object DashboardRoute
@Serializable data object GameAssistantRoute
@Serializable data object MacroListRoute
@Serializable data class MacroEditorRoute(val macroId:String)
@Serializable data object ScheduleRoute
@Serializable data object ExecutionMonitorRoute
@Serializable data object PermissionCenterRoute
@Serializable data object PrivacyCenterRoute

@Composable fun TriggerNavGraph(serviceBinder:com.trigger.app.service.GameServiceBinder,navController:NavHostController= rememberNavController()) {
    val context= LocalContext.current; val catalog= remember { lazy { MacroCatalog(context.applicationContext) } }; val scope=rememberCoroutineScope()
    Scaffold(bottomBar={ NavigationBar {
        val entry by navController.currentBackStackEntryAsState(); val current=entry?.destination?.route.orEmpty()
        listOf("Home" to DashboardRoute,"Games" to GameAssistantRoute,"Macros" to MacroListRoute,"Schedule" to ScheduleRoute,"Monitor" to ExecutionMonitorRoute,"Access" to PermissionCenterRoute,"Privacy" to PrivacyCenterRoute).forEach { (label,route) ->
            val selected=current.contains(route::class.simpleName.orEmpty(),ignoreCase=true)
            NavigationBarItem(selected=selected,onClick={navController.navigate(route) { popUpTo(navController.graph.findStartDestination().id) { saveState=true }; launchSingleTop=true; restoreState=true }},icon={Text(label.take(1))},label={Text(label)})
        }
    } }) { padding ->
        NavHost(navController,startDestination=DashboardRoute,modifier=Modifier.padding(padding)) {
            composable<DashboardRoute> { DashboardScreen(onOpenGames={navController.navigate(GameAssistantRoute)},onOpenMacros={navController.navigate(MacroListRoute)},onOpenSchedules={navController.navigate(ScheduleRoute)},onOpenMonitor={navController.navigate(ExecutionMonitorRoute)},onOpenPermissions={navController.navigate(PermissionCenterRoute)}) }
            composable<GameAssistantRoute> { GameLauncherScreen(context) { app,turbo -> serviceBinder.launch(app.packageName,app.loadLabel(context.packageManager).toString(),turbo); context.packageManager.getLaunchIntentForPackage(app.packageName)?.let { context.startActivity(it) } } }
            composable<MacroListRoute> {
                var macros by remember { mutableStateOf(emptyList<Macro>()) }
                var importError by remember { mutableStateOf<String?>(null) }
                val currentEntry by navController.currentBackStackEntryAsState()
                LaunchedEffect(currentEntry?.destination?.route) {
                    if (currentEntry?.destination?.route?.contains("MacroListRoute") == true) macros = withContext(Dispatchers.IO) { catalog.value.all() }
                }
                Column(Modifier.fillMaxSize().padding(18.dp)) {
                    Text("Macros", style = MaterialTheme.typography.headlineMedium)
                    Row(
                        Modifier.fillMaxWidth().padding(top = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(onClick = {
                            scope.launch {
                            importError = null
                            val result = withContext(Dispatchers.IO) { runCatching {
                                val template = context.assets.open("macros/free_fire_launch.json")
                                    .bufferedReader().use { it.readText() }
                                MacroSerializer.decode(template).also { catalog.value.save(it) }
                            } }
                            result.onSuccess { macro ->
                                macros = withContext(Dispatchers.IO) { catalog.value.all() }
                                navController.navigate(MacroEditorRoute(macro.id))
                            }.onFailure { error ->
                                importError = "Could not import Free Fire macro: ${error.message ?: "invalid template"}"
                            }
                            }
                        }) {
                            Text("Import Free Fire")
                        }
                        Button(onClick = {
                            scope.launch {
                            val created = withContext(Dispatchers.IO) { catalog.value.create() }
                            macros = withContext(Dispatchers.IO) { catalog.value.all() }
                            navController.navigate(MacroEditorRoute(created.id))
                            }
                        }) {
                            Text("New macro")
                        }
                    }
                    importError?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        items(macros, key = { it.id }) { macro ->
                            GlassCard(Modifier.fillMaxWidth().clickable {
                                navController.navigate(MacroEditorRoute(macro.id))
                            }) {
                                Text(macro.name, style = MaterialTheme.typography.titleMedium)
                                Text("${macro.steps.size} steps · ${macro.packageName.ifBlank { "No target app" }}")
                            }
                        }
                    }
                }
            }
            composable<MacroEditorRoute> { entry ->
                val route=entry.toRoute<MacroEditorRoute>()
                val macro by produceState<Macro?>(null, route.macroId) {
                    value = withContext(Dispatchers.IO) { catalog.value.get(route.macroId) ?: catalog.value.create() }
                }
                macro?.let { loaded ->
                    MacroEditorScreen(onTestStep={step ->
                        serviceBinder.prepareActivation()
                        scope.launch(Dispatchers.IO) { testStep(step,context.applicationContext) }
                    },initialMacro=loaded,onSaveMacro={ saved -> scope.launch(Dispatchers.IO) { catalog.value.save(saved) }; Unit })
                }
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
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) { lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { while(true) { refresh++;delay(1200) } } }; val permissions=remember(refresh) { manager.requiredAccess() }; val activation=remember(refresh) { manager.detect() }
    Column(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("Permission center",style=MaterialTheme.typography.headlineMedium);Text("Privilege state is rechecked while this screen is open.")
        permissions.forEach { (access,granted) -> GlassCard(Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { Text(access.name.replace('_',' ')); Text(if(granted) "Granted" else "Missing",color=if(granted) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error) }
            if(!granted) TextButton(onClick={disclosure=when(access) { com.trigger.feature.gameassistant.activation.RequiredAccess.OVERLAY->DisclosureAccess.OVERLAY;com.trigger.feature.gameassistant.activation.RequiredAccess.BATTERY_EXEMPTION->DisclosureAccess.BATTERY_OPTIMIZATION;com.trigger.feature.gameassistant.activation.RequiredAccess.DND->DisclosureAccess.DO_NOT_DISTURB;com.trigger.feature.gameassistant.activation.RequiredAccess.MICROPHONE->DisclosureAccess.MICROPHONE;com.trigger.feature.gameassistant.activation.RequiredAccess.USAGE_STATS->DisclosureAccess.USAGE_STATS }}) { Text("Review & open settings") }
        } }
        GlassCard(Modifier.fillMaxWidth()) { Text("Activation: ${activation.mode}");Text(activation.message);TextButton(onClick={manager.openDeveloperOptions()}) { Text("Developer options / Wireless debugging") } }
    }
    disclosure?.let { selected -> ProminentDisclosureDialog(selected,onContinue={disclosure=null;openDisclosure(selected)},onDismiss={disclosure=null}) }
}
private fun testStep(step:MacroStep,context:android.content.Context) {
    val injector=ShizukuTouchInjector()
    when(step) {
        is MacroStep.TapStep -> injector.use { it.injectTap(step.x.toInt(),step.y.toInt()) }
        is MacroStep.SwipeStep -> if (!injector.injectDrag(0,step.startX,step.startY,step.endX,step.endY,step.durationMs, onComplete = { injector.close() })) injector.close()
        is MacroStep.LaunchAppStep -> context.packageManager.getLaunchIntentForPackage(step.targetPackage)?.let { it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);context.startActivity(it) }
        is MacroStep.WaitStep, is MacroStep.ConditionStep -> Unit
    }
}
