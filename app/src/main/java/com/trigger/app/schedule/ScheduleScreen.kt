package com.trigger.app.schedule

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.trigger.core.ui.GlassCard
import java.text.DateFormat
import java.util.Calendar

@Composable fun ScheduleScreen(vm:ScheduleViewModel= viewModel()) {
    val rows by vm.schedules.collectAsStateWithLifecycle(); var dialog by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) { Text("Schedules",style=MaterialTheme.typography.headlineMedium); Button(onClick={dialog=true}) { Text("Add") } }
        if(rows.isEmpty()) Text("No scheduled macros. Add one to run a macro on a recurring interval.",Modifier.padding(top=24.dp),color=MaterialTheme.colorScheme.onSurfaceVariant)
        LazyColumn(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(vertical=14.dp)) {
            items(rows,key={it.schedule.id}) { row -> GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(row.schedule.macroName,style=MaterialTheme.typography.titleMedium); Text("Every ${row.schedule.intervalMinutes} minutes · ${if(row.schedule.requiresCharging) "Charging required" else "Any power state"}",style=MaterialTheme.typography.bodySmall); Text("WorkManager · ${row.workStatus}",color=statusColor(row.workStatus),style=MaterialTheme.typography.labelMedium) }; Switch(row.schedule.enabled,{vm.toggle(row.schedule.id,it)}) }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End) { TextButton(onClick={vm.delete(row.schedule.id)}) { Text("Remove") } }
            } }
        }
    }
    if(dialog) ScheduleDialog(onDismiss={dialog=false},onSave={name,json,interval,delay,charging -> vm.save(name,json,interval,delay,charging);dialog=false})
}
@Composable private fun ScheduleDialog(onDismiss:()->Unit,onSave:(String,String,Long,Long,Boolean)->Unit) {
    val context= LocalContext.current; var name by remember { mutableStateOf("") }; var json by remember { mutableStateOf("") }; var interval by remember { mutableStateOf("15") }; var charging by remember { mutableStateOf(false) }; var scheduledAt by remember { mutableLongStateOf(0L) }
    AlertDialog(onDismissRequest=onDismiss,title={Text("New macro schedule")},text={
        Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name,{name=it},label={Text("Macro name")},singleLine=true)
            OutlinedTextField(json,{json=it},label={Text("Macro JSON")},minLines=3)
            OutlinedTextField(interval,{interval=it.filter(Char::isDigit)},label={Text("Repeat interval (minutes, minimum 15)")},singleLine=true)
            Row(verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) { Text("Require charging",Modifier.weight(1f)); Switch(charging,{charging=it}) }
            val display=if(scheduledAt>0) DateFormat.getDateTimeInstance().format(scheduledAt) else "Start immediately"
            TextButton(onClick={ val now=Calendar.getInstance(); DatePickerDialog(context,{_,year,month,day -> val picked=Calendar.getInstance().apply { set(year,month,day) }; TimePickerDialog(context,{_,hour,minute -> picked.set(Calendar.HOUR_OF_DAY,hour);picked.set(Calendar.MINUTE,minute);picked.set(Calendar.SECOND,0);scheduledAt=picked.timeInMillis },now.get(Calendar.HOUR_OF_DAY),now.get(Calendar.MINUTE),false).show() },now.get(Calendar.YEAR),now.get(Calendar.MONTH),now.get(Calendar.DAY_OF_MONTH)).show() }) { Text("First run: $display") }
        }
    },confirmButton={TextButton(enabled=name.isNotBlank()&&json.isNotBlank(),onClick={val delay=if(scheduledAt>System.currentTimeMillis()) (scheduledAt-System.currentTimeMillis())/60_000 else 0;onSave(name,json,interval.toLongOrNull()?.coerceAtLeast(15)?:15,delay,charging)}) { Text("Save schedule") }},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})
}
@Composable private fun statusColor(status:String)=when(status) { "RUNNING"->MaterialTheme.colorScheme.tertiary; "BLOCKED"->MaterialTheme.colorScheme.error; "ENQUEUED"->MaterialTheme.colorScheme.primary; else->MaterialTheme.colorScheme.onSurfaceVariant }
