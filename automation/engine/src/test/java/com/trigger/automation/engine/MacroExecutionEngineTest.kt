package com.trigger.automation.engine

import android.view.MotionEvent
import com.trigger.feature.macroeditor.model.Macro
import com.trigger.feature.macroeditor.model.MacroStep
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.Collections
import java.util.concurrent.CopyOnWriteArrayList

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35])
class MacroExecutionEngineTest {
    private val context get()=RuntimeEnvironment.getApplication()

    @Test fun executesStepsAndTouchEventsInOrder()= runBlocking {
        val actions=CopyOnWriteArrayList<Int>()
        val sink=InputEventSink { event -> actions.add((event as MotionEvent).actionMasked); true }
        val engine=MacroExecutionEngine(context,ShizukuTouchInjector(sink),stepTimeoutMs=2_000)
        val macro=Macro("macro-1","order","test.package",listOf(MacroStep.TapStep(10f,20f),MacroStep.WaitStep(10),MacroStep.TapStep(30f,40f)))
        engine.executeMacro(macro,this).join()
        assertEquals(MacroRunStatus.COMPLETED,engine.state.value.status)
        assertEquals(listOf(MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP,MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP),actions)
        assertEquals(listOf(0,1,2),engine.log.snapshot().filter { it.phase=="STEP_START" }.mapNotNull { it.stepIndex })
    }

    @Test fun cancellationPropagatesToRunState()= runBlocking {
        val sink=InputEventSink { true }; val engine=MacroExecutionEngine(context,ShizukuTouchInjector(sink),stepTimeoutMs=10_000)
        val job=engine.executeMacro(Macro("cancel","cancel","",listOf(MacroStep.WaitStep(9_000))),this)
        kotlinx.coroutines.delay(60); job.cancel(); job.join()
        assertEquals(MacroRunStatus.CANCELLED,engine.state.value.status)
        assertEquals(ExecutionFailureReason.CANCELLED,engine.state.value.failureReason)
    }

    @Test fun stepTimeoutIsReported()= runBlocking {
        val engine=MacroExecutionEngine(context,ShizukuTouchInjector(InputEventSink { true }),stepTimeoutMs=30)
        engine.executeMacro(Macro("timeout","timeout","",listOf(MacroStep.WaitStep(500))),this).join()
        assertEquals(MacroRunStatus.FAILED,engine.state.value.status)
        assertEquals(ExecutionFailureReason.TIMEOUT,engine.state.value.failureReason)
        assertTrue(engine.log.snapshot().any { it.failureReason==ExecutionFailureReason.TIMEOUT })
    }

    @Test fun repeatCountBoundsTheExecutionLoop()= runBlocking {
        val actions=CopyOnWriteArrayList<Int>();val engine=MacroExecutionEngine(context,ShizukuTouchInjector(InputEventSink { event -> actions.add(event.actionMasked);true }))
        engine.executeMacro(Macro("repeat","repeat","",listOf(MacroStep.TapStep(4f,5f)),repeatCount=3),this).join()
        assertEquals(6,actions.size);assertEquals(MacroRunStatus.COMPLETED,engine.state.value.status)
        assertEquals(3,engine.log.snapshot().count { it.phase=="STEP_START" })
    }

    @Test fun disconnectedInjectorReportsServiceFailure()= runBlocking {
        val disconnected=object:InputEventSink { override fun inject(event:MotionEvent)=false; override fun isConnected()=false }
        val engine=MacroExecutionEngine(context,ShizukuTouchInjector(disconnected))
        engine.executeMacro(Macro("offline","offline","",listOf(MacroStep.TapStep(1f,1f))),this).join()
        assertEquals(ExecutionFailureReason.SERVICE_DISCONNECTED,engine.state.value.failureReason)
    }
}
