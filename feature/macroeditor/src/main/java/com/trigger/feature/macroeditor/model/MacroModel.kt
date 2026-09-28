package com.trigger.feature.macroeditor.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Macro(
    val id: String,
    val name: String,
    val packageName: String,
    val steps: List<MacroStep> = emptyList(),
    val repeatCount: Int = 1,
    val enabled: Boolean = true
)

@Serializable
sealed class MacroStep {
    @Serializable @SerialName("tap") data class TapStep(val x: Float, val y: Float, val delayMs: Long = 0) : MacroStep()
    @Serializable @SerialName("swipe") data class SwipeStep(val startX: Float, val startY: Float, val endX: Float, val endY: Float, val durationMs: Long = 300) : MacroStep()
    @Serializable @SerialName("wait") data class WaitStep(val durationMs: Long) : MacroStep()
    @Serializable @SerialName("launch_app") data class LaunchAppStep(val targetPackage: String) : MacroStep()
    @Serializable @SerialName("condition") data class ConditionStep(val variable: String, val value: String) : MacroStep()
}
