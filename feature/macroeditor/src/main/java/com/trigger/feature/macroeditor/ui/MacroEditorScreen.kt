package com.trigger.feature.macroeditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trigger.feature.macroeditor.model.MacroStep
import com.trigger.feature.macroeditor.model.Macro

@Composable
fun MacroEditorScreen(
    onTestStep: (MacroStep) -> Unit,
    modifier: Modifier = Modifier,
    editor: MacroEditorViewModel = viewModel(),
    initialMacro: Macro? = null,
    onSaveMacro: ((Macro)->Unit)? = null
) {
    val macro by editor.macro.collectAsState()
    var editIndex by remember { mutableIntStateOf(-1) }
    var showAdd by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }
    var importJson by remember { mutableStateOf("") }
    var dragRemainder by remember { mutableFloatStateOf(0f) }
    val clipboard= LocalClipboardManager.current
    LaunchedEffect(initialMacro?.id) { initialMacro?.let(editor::load) }

    Column(modifier.fillMaxSize().background(Color(0xFF101114)).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) { Text("Visual Macro Editor",Modifier.weight(1f),style=MaterialTheme.typography.headlineSmall,color=Color.White); if(onSaveMacro!=null) TextButton(onClick={onSaveMacro(macro)}) { Text("Save") } }
        OutlinedTextField(macro.name,editor::setName,label={Text("Macro name")},singleLine=true,modifier=Modifier.fillMaxWidth())
        OutlinedTextField(macro.packageName,editor::setPackage,label={Text("Target package")},singleLine=true,modifier=Modifier.fillMaxWidth())
        Row(verticalAlignment=Alignment.CenterVertically) {
            Text("Repeats",color=Color.White); IconButton(onClick={editor.repeatCount(macro.repeatCount-1)}) { Text("−") }; Text("${macro.repeatCount}",color=Color.White); IconButton(onClick={editor.repeatCount(macro.repeatCount+1)}) { Text("+") }
            Spacer(Modifier.weight(1f)); TextButton(onClick={showImport=true}) { Text("Import JSON") }; TextButton(onClick={clipboard.setText(AnnotatedString(editor.exportJson()))}) { Text("Copy JSON") }
        }
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) { Text("Steps (${macro.steps.size})",Modifier.weight(1f),color=Color.White,style=MaterialTheme.typography.titleMedium); Button(onClick={showAdd=true}) { Text("Add step") } }
        LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp),contentPadding=PaddingValues(vertical=8.dp)) {
            itemsIndexed(macro.steps,key={index,step -> "${index}_${step.hashCode()}"}) { index,step ->
                Card(Modifier.fillMaxWidth().animateItem().pointerInput(index,macro.steps.size) {
                    detectDragGestures(onDragStart={dragRemainder=0f}) { change,amount ->
                        change.consume(); dragRemainder+=amount.y
                        if(dragRemainder > 54f && index < macro.steps.lastIndex) { editor.move(index,index+1); dragRemainder=0f }
                        else if(dragRemainder < -54f && index > 0) { editor.move(index,index-1); dragRemainder=0f }
                    }
                },shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFF1C1C1E))) {
                    Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
                        Text("≡",color=Color.LightGray,modifier=Modifier.padding(horizontal=8.dp)); Column(Modifier.weight(1f)) { Text(stepTitle(step),color=Color.White,style=MaterialTheme.typography.titleSmall); Text(stepDescription(step),color=Color.LightGray,style=MaterialTheme.typography.bodySmall) }
                        TextButton(onClick={onTestStep(step)}) { Text("Test") }; TextButton(onClick={editIndex=index}) { Text("Edit") }; TextButton(onClick={editor.delete(index)}) { Text("×") }
                    }
                }
            }
        }
    }
    if(showAdd) StepDialog(null,{editor.add(it); showAdd=false},{showAdd=false})
    if(editIndex in macro.steps.indices) { val index=editIndex; StepDialog(macro.steps[index],{editor.replace(index,it);editIndex=-1},{editIndex=-1}, onDelete={editor.delete(index);editIndex=-1}) }
    if(showImport) AlertDialog(onDismissRequest={showImport=false},title={Text("Import macro JSON")},text={ OutlinedTextField(importJson,{importJson=it},minLines=6,label={Text("JSON")}) },confirmButton={TextButton(onClick={editor.importJson(importJson).onSuccess { showImport=false }.onFailure { importJson="Invalid macro JSON: ${it.message}" }}) {Text("Import")}},dismissButton={TextButton(onClick={showImport=false}) {Text("Cancel")}})
}

@Composable private fun StepDialog(step: MacroStep?, onSave:(MacroStep)->Unit,onDismiss:()->Unit,onDelete:(()->Unit)?=null) {
    var kind by remember(step) { mutableStateOf(stepKind(step)) }
    var fields by remember(step) { mutableStateOf(stepFields(step)) }
    AlertDialog(onDismissRequest=onDismiss,title={Text(if(step==null) "Add step" else "Edit step")},text={
        Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
            if(step==null) { listOf("Tap","Swipe","Wait","Launch app","Condition").forEach { TextButton(onClick={kind=it}) { Text(if(kind==it) "● $it" else it) } } }
            val labels=when(kind) { "Tap"->listOf("X","Y","Delay ms"); "Swipe"->listOf("Start X","Start Y","End X","End Y","Duration ms"); "Wait"->listOf("Duration ms"); "Launch app"->listOf("Package name"); else->listOf("Variable","Value") }
            labels.forEachIndexed { i,label -> OutlinedTextField(fields.getOrElse(i){""},{v->fields=fields.toMutableList().also { while(it.size<=i) it.add(""); it[i]=v }},label={Text(label)},singleLine=true) }
        }
    },confirmButton={TextButton(onClick={ val values=fields.map(String::trim); val result=when(kind) {
        "Tap"->MacroStep.TapStep(values.getOrNull(0)?.toFloatOrNull()?:0f,values.getOrNull(1)?.toFloatOrNull()?:0f,values.getOrNull(2)?.toLongOrNull()?:0L)
        "Swipe"->MacroStep.SwipeStep(values.getOrNull(0)?.toFloatOrNull()?:0f,values.getOrNull(1)?.toFloatOrNull()?:0f,values.getOrNull(2)?.toFloatOrNull()?:0f,values.getOrNull(3)?.toFloatOrNull()?:0f,values.getOrNull(4)?.toLongOrNull()?:300L)
        "Wait"->MacroStep.WaitStep(values.firstOrNull()?.toLongOrNull()?.coerceAtLeast(0)?:0L)
        "Launch app"->MacroStep.LaunchAppStep(values.firstOrNull().orEmpty())
        else->MacroStep.ConditionStep(values.getOrNull(0).orEmpty(),values.getOrNull(1).orEmpty())
    }; onSave(result)}) {Text("Save")}},dismissButton={Row { if(onDelete!=null) TextButton(onClick=onDelete){Text("Delete")}; TextButton(onClick=onDismiss){Text("Cancel")} }})
}
private fun stepKind(step: MacroStep?): String = when(step) { is MacroStep.TapStep->"Tap"; is MacroStep.SwipeStep->"Swipe"; is MacroStep.WaitStep->"Wait"; is MacroStep.LaunchAppStep->"Launch app"; is MacroStep.ConditionStep->"Condition"; null->"Tap" }
private fun stepFields(step: MacroStep?): List<String> = when(step) {
    is MacroStep.TapStep->listOf(step.x.toString(),step.y.toString(),step.delayMs.toString()); is MacroStep.SwipeStep->listOf(step.startX.toString(),step.startY.toString(),step.endX.toString(),step.endY.toString(),step.durationMs.toString()); is MacroStep.WaitStep->listOf(step.durationMs.toString()); is MacroStep.LaunchAppStep->listOf(step.targetPackage); is MacroStep.ConditionStep->listOf(step.variable,step.value); null->listOf("0","0","0")
}
private fun stepTitle(step: MacroStep)=when(step) { is MacroStep.TapStep->"Tap"; is MacroStep.SwipeStep->"Swipe"; is MacroStep.WaitStep->"Wait"; is MacroStep.LaunchAppStep->"Launch app"; is MacroStep.ConditionStep->"Condition" }
private fun stepDescription(step: MacroStep)=when(step) { is MacroStep.TapStep->"(${step.x}, ${step.y}) · ${step.delayMs} ms delay"; is MacroStep.SwipeStep->"(${step.startX}, ${step.startY}) → (${step.endX}, ${step.endY}) · ${step.durationMs} ms"; is MacroStep.WaitStep->"${step.durationMs} ms"; is MacroStep.LaunchAppStep->step.targetPackage; is MacroStep.ConditionStep->"${step.variable} = ${step.value}" }
