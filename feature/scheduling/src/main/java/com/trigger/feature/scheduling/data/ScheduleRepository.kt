package com.trigger.feature.scheduling.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.trigger.feature.scheduling.worker.MacroScheduleWorker
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.UUID
import java.util.concurrent.TimeUnit

class ScheduleRepository(context:Context) {
    private val app=context.applicationContext
    private val prefs=app.getSharedPreferences("macro_schedules",Context.MODE_PRIVATE)
    private val wm=WorkManager.getInstance(app)
    private val json=Json { ignoreUnknownKeys=true }
    @Synchronized fun all():List<MacroSchedule> = try { json.decodeFromString(ListSerializer(MacroSchedule.serializer()),prefs.getString(KEY,"[]") ?: "[]") } catch (_:Exception) { emptyList() }
    @Synchronized fun save(schedule:MacroSchedule):MacroSchedule {
        require(schedule.macroJson.toByteArray().size < 9_000) { "Scheduled macro is too large for WorkManager input data" }
        val normalized=schedule.copy(intervalMinutes=schedule.intervalMinutes.coerceAtLeast(15),workId=schedule.workId ?: UUID.randomUUID().toString())
        val entries=all().filterNot { it.id==normalized.id }+normalized
        persist(entries); if(normalized.enabled) enqueue(normalized) else wm.cancelUniqueWork(normalized.id)
        return normalized
    }
    @Synchronized fun delete(id:String) { persist(all().filterNot { it.id==id }); wm.cancelUniqueWork(id) }
    @Synchronized fun setEnabled(id:String,enabled:Boolean):MacroSchedule? {
        val found=all().firstOrNull { it.id==id } ?: return null
        val updated=found.copy(enabled=enabled); persist(all().map { if(it.id==id) updated else it })
        if(enabled) enqueue(updated) else wm.cancelUniqueWork(id)
        return updated
    }
    private fun persist(entries:List<MacroSchedule>) { prefs.edit().putString(KEY,json.encodeToString(ListSerializer(MacroSchedule.serializer()),entries)).commit() }
    private fun enqueue(schedule:MacroSchedule) {
        val constraints=Constraints.Builder().setRequiresCharging(schedule.requiresCharging).build()
        val data=Data.Builder().putString(MacroScheduleWorker.KEY_MACRO_JSON,schedule.macroJson).build()
        val request=PeriodicWorkRequestBuilder<MacroScheduleWorker>(schedule.intervalMinutes.coerceAtLeast(15),TimeUnit.MINUTES)
            .setInitialDelay(schedule.initialDelayMinutes.coerceAtLeast(0),TimeUnit.MINUTES).setConstraints(constraints).setInputData(data).build()
        wm.enqueueUniquePeriodicWork(schedule.id,ExistingPeriodicWorkPolicy.UPDATE,request)
    }
    companion object { private const val KEY="schedules_json" }
}
