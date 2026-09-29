package com.trigger.app.launcher

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.trigger.app.bridge.SystemActivationBridgeImpl
import com.trigger.feature.gameassistant.activation.ActivationManager
import com.trigger.feature.gameassistant.activation.RequiredAccess

@Composable fun GameLauncherScreen(context: Context, onLaunch: (ApplicationInfo, Boolean) -> Unit) {
    var apps by remember { mutableStateOf(emptyList<ApplicationInfo>()) }; var selected by remember { mutableStateOf<ApplicationInfo?>(null) }; var turbo by remember { mutableStateOf(true) }; var refresh by remember { mutableIntStateOf(0) }
    val activation = remember { ActivationManager(context, SystemActivationBridgeImpl(context)) }
    val mic = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }
    LaunchedEffect(refresh) {
        val pm=context.packageManager; val intent=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        @Suppress("DEPRECATION") val resolves=pm.queryIntentActivities(intent,0)
        apps=resolves.mapNotNull { it.activityInfo?.applicationInfo }.distinctBy { it.packageName }.filter { ai ->
            val game=(ai.flags and ApplicationInfo.FLAG_IS_GAME) != 0
            game || ai.category == ApplicationInfo.CATEGORY_GAME
        }.sortedBy { it.loadLabel(pm).toString().lowercase() }
    }
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Game Turbo", style=MaterialTheme.typography.headlineMedium); Text("Choose a game to configure before launch", modifier=Modifier.padding(bottom=12.dp))
        Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) { Text("Enable overlay",Modifier.weight(1f)); Switch(turbo,{turbo=it}) }
        LazyVerticalGrid(columns=GridCells.Adaptive(150.dp), contentPadding=PaddingValues(top=12.dp), horizontalArrangement=Arrangement.spacedBy(12.dp), verticalArrangement=Arrangement.spacedBy(12.dp)) {
            items(apps,key={it.packageName}) { app -> Card(Modifier.fillMaxWidth().clickable { selected=app }) { Column(Modifier.padding(16.dp)) { Text(app.loadLabel(context.packageManager).toString(),style=MaterialTheme.typography.titleMedium); Text("Tap to launch",style=MaterialTheme.typography.bodySmall) } } }
        }
    }
    selected?.let { app ->
        PermissionOnboardingBottomSheet(context, app.loadLabel(context.packageManager).toString(), activation, onDismiss={selected=null}) {
            onLaunch(app,turbo); selected=null
        }
    }
}
