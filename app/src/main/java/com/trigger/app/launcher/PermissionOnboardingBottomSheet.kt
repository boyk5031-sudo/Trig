package com.trigger.app.launcher

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.trigger.feature.gameassistant.activation.ActivationManager
import com.trigger.feature.gameassistant.activation.ActivationMode
import com.trigger.feature.gameassistant.activation.RequiredAccess
import com.trigger.feature.settings.DisclosureAccess
import com.trigger.feature.settings.ProminentDisclosureDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun PermissionOnboardingBottomSheet(context: Context, appLabel: String, activation: ActivationManager, onDismiss:()->Unit, onLaunch:()->Unit) {
    var refresh by remember { mutableIntStateOf(0) }
    var pendingDisclosure by remember { mutableStateOf<DisclosureAccess?>(null) }
    val owner= LocalLifecycleOwner.current
    DisposableEffect(owner) { val observer=LifecycleEventObserver { _,event -> if(event==Lifecycle.Event.ON_RESUME) refresh++ }; owner.lifecycle.addObserver(observer); onDispose { owner.lifecycle.removeObserver(observer) } }
    val mic= rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }
    val states=remember(refresh) { activation.requiredAccess() }
    val activationState=remember(refresh) { activation.detect() }
    val hasShell = activationState.ready && activationState.mode in setOf(ActivationMode.ROOT,ActivationMode.SHIZUKU)
    val allReady=states.filterKeys { it!=RequiredAccess.USAGE_STATS }.values.all { it } && hasShell
    val sheetState= rememberModalBottomSheetState(skipPartiallyExpanded=true)
    fun openAccess(access:DisclosureAccess) { when(access) { DisclosureAccess.OVERLAY->activation.openOverlaySettings();DisclosureAccess.MICROPHONE->mic.launch(Manifest.permission.RECORD_AUDIO);DisclosureAccess.DO_NOT_DISTURB->activation.openDndSettings();DisclosureAccess.USAGE_STATS->activation.openUsageSettings();DisclosureAccess.BATTERY_OPTIMIZATION->activation.openBatterySettings() } }
    fun disclosureFor(access:RequiredAccess)=when(access) { RequiredAccess.OVERLAY->DisclosureAccess.OVERLAY;RequiredAccess.BATTERY_EXEMPTION->DisclosureAccess.BATTERY_OPTIMIZATION;RequiredAccess.DND->DisclosureAccess.DO_NOT_DISTURB;RequiredAccess.MICROPHONE->DisclosureAccess.MICROPHONE;RequiredAccess.USAGE_STATS->DisclosureAccess.USAGE_STATS }
    ModalBottomSheet(onDismissRequest=onDismiss,sheetState=sheetState,shape=RoundedCornerShape(topStart=28.dp,topEnd=28.dp),containerColor=Color(0xFF1C1C1E),scrimColor=Color.Black.copy(.52f)) {
        Column(Modifier.fillMaxWidth().background(Color(0xFF1C1C1E)).padding(horizontal=24.dp).padding(bottom=32.dp)) {
            Text("Ready to play?",style=MaterialTheme.typography.headlineSmall,color=Color.White)
            Text("Set up Game Turbo for $appLabel",color=Color.White.copy(.7f),modifier=Modifier.padding(top=4.dp,bottom=14.dp))
            RequiredAccess.entries.forEach { access ->
                val granted=states[access]==true
                Row(Modifier.fillMaxWidth().padding(vertical=6.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(label(access),color=Color.White); Text(if(granted) "Access granted" else if(access==RequiredAccess.USAGE_STATS) "Optional; auto-detection not active" else "Required system access",color=Color.White.copy(.55f),style=MaterialTheme.typography.bodySmall) }
                    Text(if(granted) "✓" else "OFF",color=if(granted) Color(0xFF55D69E) else Color(0xFFFF8A80))
                    if(!granted) TextButton(onClick={pendingDisclosure=disclosureFor(access)}) { Text(if(access==RequiredAccess.USAGE_STATS) "Review" else "Grant") }
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical=8.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Shizuku / Root activation",color=Color.White); Text(activationState.message,color=Color.White.copy(.55f),style=MaterialTheme.typography.bodySmall) }
                Text(if(hasShell) "✓" else "OFF",color=if(hasShell) Color(0xFF55D69E) else Color(0xFFFF8A80))
                if(!hasShell) TextButton(onClick={activation.openDeveloperOptions()}) { Text("Setup") }
            }
            Button(onClick={if(allReady) onLaunch() else {
                val missing=RequiredAccess.entries.firstOrNull { it!=RequiredAccess.USAGE_STATS && states[it]!=true }
                if(missing!=null) pendingDisclosure=disclosureFor(missing) else activation.openDeveloperOptions()
            }},modifier=Modifier.fillMaxWidth().height(54.dp),shape=RoundedCornerShape(18.dp)) { Text(if(allReady) "Grant All & Launch" else "Grant All & Launch") }
        }
    }
    pendingDisclosure?.let { access -> ProminentDisclosureDialog(access,onContinue={pendingDisclosure=null;openAccess(access)},onDismiss={pendingDisclosure=null}) }
}
private fun label(access: RequiredAccess)=when(access) { RequiredAccess.OVERLAY->"Display over other apps"; RequiredAccess.BATTERY_EXEMPTION->"Battery optimization exemption"; RequiredAccess.DND->"Do Not Disturb access"; RequiredAccess.MICROPHONE->"Microphone"; RequiredAccess.USAGE_STATS->"Usage statistics" }
