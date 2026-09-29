package com.trigger.feature.gameassistant.util

import android.app.NotificationManager
import android.content.Context
import android.os.Build

/** Captures and restores the user's prior interruption filter; DND access is user-granted. */
class GameDndController(context: Context) {
    private val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private var original: Int? = null
    @Synchronized fun enable(alarmsOnly: Boolean = false): Boolean {
        if (Build.VERSION.SDK_INT < 23 || !manager.isNotificationPolicyAccessGranted) return false
        if (original == null) original = manager.currentInterruptionFilter
        manager.setInterruptionFilter(if (alarmsOnly) NotificationManager.INTERRUPTION_FILTER_ALARMS else NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        return true
    }
    @Synchronized fun restore() { val old=original ?: return; if(Build.VERSION.SDK_INT >= 23 && manager.isNotificationPolicyAccessGranted) manager.setInterruptionFilter(old); original=null }
}
