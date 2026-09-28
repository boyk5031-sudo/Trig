package com.trigger.feature.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable fun ComplianceCenterScreen() {
    var disclosure by remember { mutableStateOf<DisclosureAccess?>(null) }
    val items=listOf(
        DisclosureAccess.OVERLAY to "Overlay handle",
        DisclosureAccess.MICROPHONE to "Live voice DSP",
        DisclosureAccess.DO_NOT_DISTURB to "Game interruption control",
        DisclosureAccess.USAGE_STATS to "Foreground game detection (optional; not active in this build)",
        DisclosureAccess.BATTERY_OPTIMIZATION to "Battery optimization exemption"
    )
    LazyColumn(Modifier.fillMaxSize().padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item { Text("Privacy & permissions",style=MaterialTheme.typography.headlineMedium); Text("Review what each sensitive access enables before you grant it.",style=MaterialTheme.typography.bodyMedium) }
        items(items) { (kind,label) -> Card(Modifier.fillMaxWidth()) { Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.SpaceBetween) { Text(label,Modifier.weight(1f));TextButton(onClick={disclosure=kind}) { Text("Review") } } } }
        item {
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text("Data handling",style=MaterialTheme.typography.titleLarge)
                Text("• No analytics or remote telemetry is implemented. The app manifest does not request INTERNET; app features do not send data to a remote server or download executable code.")
                Text("• Macro definitions are stored locally in Room and in the macro catalog's app-private preferences. Schedule definitions are stored in app-private SharedPreferences and scheduled with WorkManager.")
                Text("• Execution diagnostics remain in process memory. Export is available only through Android's system document picker (SAF), to a destination you choose.")
                Text("• Audio buffers are processed transiently in memory by the optional DSP. Trigger does not persist or upload microphone recordings.")
                Text("• Android, Shizuku, WorkManager, and app-store services may have their own platform behavior and policies; Trigger does not control those services.")
            } }
        }
    }
    disclosure?.let { selected -> ProminentDisclosureDialog(selected,onContinue={disclosure=null},onDismiss={disclosure=null}) }
}
