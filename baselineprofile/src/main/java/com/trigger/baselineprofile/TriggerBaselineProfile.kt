package com.trigger.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TriggerBaselineProfile {
    @get:Rule val baselineRule=BaselineProfileRule()

    @Test fun dashboardAndNavigationStartupProfile() = baselineRule.collect(packageName="com.trigger.app") {
        pressHome();startActivityAndWait()
        device.wait(Until.hasObject(By.text("Trigger")),5_000)
        device.wait(Until.hasObject(By.text("Quick actions")),5_000)
        device.findObject(By.text("Games")).click()
        device.waitForIdle()
        device.findObject(By.text("Macros")).click()
        device.waitForIdle()
        device.findObject(By.text("Schedule")).click()
        device.waitForIdle()
        device.findObject(By.text("Monitor")).click()
        device.waitForIdle()
    }
}
