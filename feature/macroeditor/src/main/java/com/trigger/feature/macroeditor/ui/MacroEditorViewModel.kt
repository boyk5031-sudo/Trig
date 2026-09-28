package com.trigger.feature.macroeditor.ui

import androidx.lifecycle.ViewModel
import com.trigger.feature.macroeditor.model.Macro
import com.trigger.feature.macroeditor.model.MacroStep
import com.trigger.feature.macroeditor.serialization.MacroSerializer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

class MacroEditorViewModel : ViewModel() {
    private val _macro = MutableStateFlow(Macro(UUID.randomUUID().toString(), "New macro", "", emptyList()))
    val macro: StateFlow<Macro> = _macro.asStateFlow()
    fun load(value: Macro) { _macro.value=value }
    fun setName(value: String) { _macro.update { it.copy(name=value) } }
    fun setPackage(value: String) { _macro.update { it.copy(packageName=value) } }
    fun add(step: MacroStep) { _macro.update { it.copy(steps=it.steps+step) } }
    fun replace(index: Int, step: MacroStep) { _macro.update { m -> if(index !in m.steps.indices) m else m.copy(steps=m.steps.toMutableList().also { it[index]=step }) } }
    fun delete(index: Int) { _macro.update { m -> m.copy(steps=m.steps.filterIndexed { i,_ -> i!=index }) } }
    fun move(from: Int,to: Int) { _macro.update { m -> if(from !in m.steps.indices || to !in m.steps.indices || from==to) m else m.copy(steps=m.steps.toMutableList().also { val item=it.removeAt(from); it.add(to,item) }) } }
    fun repeatCount(value: Int) { _macro.update { it.copy(repeatCount=value.coerceAtLeast(1)) } }
    fun importJson(source: String): Result<Unit> = runCatching { _macro.value=MacroSerializer.decode(source) }
    fun exportJson(): String = MacroSerializer.encode(_macro.value)
}
