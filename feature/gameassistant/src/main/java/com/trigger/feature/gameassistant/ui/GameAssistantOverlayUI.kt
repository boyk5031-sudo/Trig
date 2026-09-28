package com.trigger.feature.gameassistant.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*

@Composable fun GameAssistantOverlayUI(onExpand: () -> Unit, expanded: Boolean, onClose: () -> Unit, onAutoFire: (Boolean, Int) -> Unit = { _, _ -> }) {
    var cps by remember { mutableFloatStateOf(12f) }; var auto by remember { mutableStateOf(false) }; var dnd by remember { mutableStateOf(false) }
    val springSpec = spring<Float>(stiffness = Spring.StiffnessMediumLow)
    val slideSpec = spring<Int>(stiffness = Spring.StiffnessMediumLow)
    AnimatedVisibility(visible = true, enter = scaleIn(springSpec) + slideInVertically(slideSpec) { it / 3 }) {
        if (!expanded) Row(Modifier.clip(RoundedCornerShape(30.dp)).background(Color(0x991C1C1E)).border(1.dp, Color.White.copy(.2f), RoundedCornerShape(30.dp)).clickable(onClick = onExpand).padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) { Text("TRIG", color = Color.White, fontWeight = FontWeight.Bold); Spacer(Modifier.width(9.dp)); Text("FPS --", color = Color(0xFF9DE9FF), fontSize = 12.sp) }
        else Column(Modifier.fillMaxSize().background(Color.Black.copy(.22f)).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Column(Modifier.widthIn(max = 460.dp).fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(Brush.linearGradient(listOf(Color(0xDD292A30), Color(0xCC17181C)))).border(1.dp, Color.White.copy(.22f), RoundedCornerShape(32.dp)).padding(22.dp).verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) { Text("Game Control Center", color = Color.White, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f)); TextButton(onClick = onClose) { Text("Close") } }
                Text("FPS  --     CPS  0", color = Color(0xFF9DE9FF), modifier = Modifier.padding(vertical = 12.dp))
                CardLine("Crosshair HUD", "Dot · Crosshair · Circle-Dot · Open Cross")
                Row(verticalAlignment = Alignment.CenterVertically) { Text("Auto-fire", Modifier.weight(1f), color = Color.White); Switch(auto, { auto = it; onAutoFire(it, cps.toInt()) }) }
                Text("Rate ${cps.toInt()} CPS", color = Color.White); Slider(cps, { cps = it }, valueRange = 1f..60f)
                Row(verticalAlignment = Alignment.CenterVertically) { Text("Game DND", Modifier.weight(1f), color = Color.White); Switch(dnd, { dnd = it }) }
                CardLine("Voice DSP", "Microphone access required")
                CardLine("Memory booster", "Review eligible background apps")
            }
        }
    }
}
@Composable private fun CardLine(title: String, detail: String) { Column(Modifier.fillMaxWidth().padding(vertical = 8.dp).clip(RoundedCornerShape(18.dp)).background(Color.White.copy(.08f)).padding(14.dp)) { Text(title, color = Color.White, fontWeight = FontWeight.SemiBold); Text(detail, color = Color.White.copy(.65f), fontSize = 12.sp) } }
