package com.trigger.automation.engine

import com.trigger.feature.macroeditor.model.Macro
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ResourceLifecycleTest {
    @Test fun closingInjectorStopsItsOwnedDragThread() {
        val worker = AtomicReference<Thread>()
        val moveSeen = CountDownLatch(1)
        val injector = ShizukuTouchInjector(InputEventSink { event ->
            if (event.actionMasked == android.view.MotionEvent.ACTION_MOVE) {
                worker.set(Thread.currentThread())
                moveSeen.countDown()
            }
            true
        })
        try {
            assertTrue(injector.injectDrag(0, 0f, 0f, 100f, 100f, 60_000))
            assertTrue(moveSeen.await(2, TimeUnit.SECONDS))
        } finally {
            injector.close()
        }
        worker.get()?.join(2_000)
        assertNotNull(worker.get())
        assertFalse("Owned drag executor must not survive close", worker.get().isAlive)
        assertTrue(injector.activePointers().isEmpty())
    }

    @Test fun completedRunDoesNotLeaveAnActiveChildJob() = runBlocking {
        val parent = Job()
        val scope = CoroutineScope(parent + Dispatchers.Default)
        ShizukuTouchInjector(InputEventSink { true }).use { injector ->
            try {
                val engine = MacroExecutionEngine(RuntimeEnvironment.getApplication(), injector)
                withTimeout(2_000) { engine.executeMacro(Macro("empty", "empty", "", emptyList()), scope).join() }
                assertFalse("No orphan SupervisorJob after a run", parent.children.any())
            } finally {
                parent.cancel()
            }
        }
    }

    @Test fun emptyLargeRepeatIsCancellable() = runBlocking {
        ShizukuTouchInjector(InputEventSink { true }).use { injector ->
            val engine = MacroExecutionEngine(RuntimeEnvironment.getApplication(), injector)
            val job = engine.executeMacro(Macro("repeat", "repeat", "", emptyList(), repeatCount = Int.MAX_VALUE), this)
            try {
                withTimeout(2_000) {
                    while (engine.state.value.status != MacroRunStatus.RUNNING) yield()
                    job.cancelAndJoin()
                }
                assertEquals(MacroRunStatus.CANCELLED, engine.state.value.status)
            } finally {
                job.cancelAndJoin()
            }
        }
    }
}
