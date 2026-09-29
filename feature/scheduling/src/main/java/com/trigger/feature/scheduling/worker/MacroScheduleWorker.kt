package com.trigger.feature.scheduling.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.trigger.automation.engine.CoordinateTransformer
import com.trigger.automation.engine.MacroExecutionEngine
import com.trigger.automation.engine.ShizukuInputEventSink
import com.trigger.automation.engine.ShizukuTouchInjector
import com.trigger.feature.macroeditor.serialization.MacroSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

class MacroScheduleWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params) {
    override suspend fun doWork():Result {
        val encoded=inputData.getString(KEY_MACRO_JSON) ?: return Result.failure()
        val macro=try { MacroSerializer.decode(encoded) } catch (_:Exception) { return Result.failure() }
        val sink=ShizukuInputEventSink(applicationContext)
        try {
            sink.connect()
            var tries=0
            while(sink.connection.state!=com.trigger.automation.engine.ShizukuServiceConnection.State.CONNECTED && tries++<30 && currentCoroutineContext().isActive) delay(500)
            if(sink.connection.state!=com.trigger.automation.engine.ShizukuServiceConnection.State.CONNECTED) return Result.retry()
            val injector=ShizukuTouchInjector(sink)
            val engine=MacroExecutionEngine(applicationContext,injector,CoordinateTransformer(applicationContext))
            val job=engine.executeMacro(macro,CoroutineScope(currentCoroutineContext()))
            job.join()
            return when(engine.state.value.status) {
                com.trigger.automation.engine.MacroRunStatus.COMPLETED -> Result.success()
                com.trigger.automation.engine.MacroRunStatus.CANCELLED -> Result.failure()
                else -> Result.retry()
            }
        } finally { sink.close() }
    }
    companion object { const val KEY_MACRO_JSON="macro_json" }
}
