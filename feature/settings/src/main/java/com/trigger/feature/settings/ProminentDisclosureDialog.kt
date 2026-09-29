package com.trigger.feature.settings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight

enum class DisclosureAccess { OVERLAY, MICROPHONE, DO_NOT_DISTURB, USAGE_STATS, BATTERY_OPTIMIZATION }

/** Shown immediately before opening a permission flow; callers retain full control over whether to proceed. */
@Composable fun ProminentDisclosureDialog(access:DisclosureAccess,onContinue:()->Unit,onDismiss:()->Unit) {
    val (title,body)=when(access) {
        DisclosureAccess.OVERLAY -> "Display over other apps" to "Trigger uses this access only to show the floating Game Turbo handle and control panel while you play. The overlay is started by you, can be stopped at any time, and may appear above other apps. Trigger does not capture screenshots or use this permission to read other apps' screen content."
        DisclosureAccess.MICROPHONE -> "Microphone and live audio processing" to "If you enable Voice DSP, microphone PCM frames are processed locally by FemaleVoiceDSP and routed to this device's audio output. Trigger does not record or upload conversations. Android does not guarantee that this output becomes the microphone input of a third-party game chat; do not use the feature for undisclosed recording."
        DisclosureAccess.DO_NOT_DISTURB -> "Do Not Disturb access" to "During a game session, Trigger can temporarily change the interruption filter to reduce notification banners and interruptions. The previous filter is restored when the overlay service stops. You can revoke this access in Android settings at any time."
        DisclosureAccess.USAGE_STATS -> "Usage access" to "Usage access can reveal which app is currently in the foreground. It is intended only for local game detection and overlay behavior; usage history is not transmitted. Automatic foreground-game detection is not wired in this build, so this access is optional and is not required to launch a game."
        DisclosureAccess.BATTERY_OPTIMIZATION -> "Battery optimization exemption" to "Exemption can help Android keep the user-started overlay available during gameplay. It may increase battery use. Trigger does not need exemption to browse games or edit macros; you can leave it disabled and change it later in settings."
    }
    AlertDialog(onDismissRequest=onDismiss,title={Text(title,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold)},text={Text(body,style=MaterialTheme.typography.bodyMedium)},confirmButton={TextButton(onClick=onContinue){Text("Continue to settings")}},dismissButton={TextButton(onClick=onDismiss){Text("Not now")}})
}
