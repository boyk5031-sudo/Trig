package com.trigger.feature.gameassistant.activation

import android.content.Context
import android.content.pm.PackageManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class ActivationManagerTest {
    private val context:Context get()=RuntimeEnvironment.getApplication()
    private class Bridge(private val alive:Boolean,private val granted:Boolean):ShizukuBridge {
        override fun isBinderAlive()=alive
        override fun isPermissionGranted()=granted
        override fun executeAsShell(command:String)=false
    }
    @Test fun reportsShizukuWhenBinderAndPermissionAreReady() {
        val state=ActivationManager(context,Bridge(true,true),listOf("/definitely/not/su")).detect()
        assertEquals(ActivationMode.SHIZUKU,state.mode);assertTrue(state.ready)
    }
    @Test fun reportsUnavailableWirelessSetupWhenShizukuIsDown() {
        val state=ActivationManager(context,Bridge(false,false),listOf("/definitely/not/su")).detect()
        assertEquals(ActivationMode.WIRELESS_DEBUGGING,state.mode);assertFalse(state.ready)
    }
    @Test fun rootBinaryIsPrioritized() {
        val fakeSu=File.createTempFile("test-su","bin").apply { setExecutable(true) }
        try { val state=ActivationManager(context,Bridge(true,true),listOf(fakeSu.absolutePath)).detect();assertEquals(ActivationMode.ROOT,state.mode) } finally { fakeSu.delete() }
    }
}
