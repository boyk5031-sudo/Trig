package com.trigger.app.schedule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.trigger.feature.scheduling.data.MacroSchedule
import com.trigger.feature.scheduling.data.ScheduleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class ScheduleRow(val schedule:MacroSchedule,val workStatus:String)
class ScheduleViewModel(application:Application):AndroidViewModel(application) {
    private val repo=ScheduleRepository(application); private val wm=WorkManager.getInstance(application)
    private val mutable=MutableStateFlow<List<ScheduleRow>>(emptyList()); val schedules:StateFlow<List<ScheduleRow>> = mutable.asStateFlow()
    init { viewModelScope.launch { while(true) { refresh(); delay(3000) } } }
    private suspend fun refresh()=withContext(Dispatchers.IO) { mutable.value=repo.all().map { schedule -> val info=try { wm.getWorkInfosForUniqueWork(schedule.id).get().firstOrNull()?.state?.name ?: if(schedule.enabled) "ENQUEUED" else "CANCELLED" } catch (_:Exception) { "UNKNOWN" }; ScheduleRow(schedule,info) } }
    fun save(name:String,json:String,intervalMinutes:Long,delayMinutes:Long,charging:Boolean) { viewModelScope.launch(Dispatchers.IO) { runCatching { repo.save(MacroSchedule(UUID.randomUUID().toString(),name,json,intervalMinutes.coerceAtLeast(15),delayMinutes.coerceAtLeast(0),charging)) }; refresh() } }
    fun toggle(id:String,enabled:Boolean) { viewModelScope.launch(Dispatchers.IO) { repo.setEnabled(id,enabled); refresh() } }
    fun delete(id:String) { viewModelScope.launch(Dispatchers.IO) { repo.delete(id); refresh() } }
}
