package com.trigger.automation.engine

import android.content.Context
import android.os.Binder
import android.os.IBinder
import android.os.Process
import android.view.InputEvent
import java.lang.reflect.InvocationTargetException

/** Instantiated by Shizuku as a user service (ADB-backed sessions normally run as UID 2000). */
class ShizukuUserService private constructor(private val trustedCallerUid:Int) : IInputEventService.Stub() {
    companion object { private const val INJECT_INPUT_EVENT_MODE_ASYNC=0 }
    constructor():this(-1)
    /** Shizuku v13 prefers this constructor and supplies the calling application's context. */
    constructor(context:Context):this(runCatching { context.applicationInfo.uid }.getOrDefault(-1))
    @Volatile private var inputManager: Any? = null
    @Volatile private var injectMethod: java.lang.reflect.Method? = null

    init { connectInputManager() }

    private fun connectInputManager() {
        try {
            val serviceManager=Class.forName("android.os.ServiceManager")
            val binder=serviceManager.getDeclaredMethod("getService",String::class.java).apply { isAccessible=true }.invoke(null,"input") as? IBinder ?: return
            val stub=Class.forName("android.hardware.input.IInputManager\$Stub")
            val manager=stub.getDeclaredMethod("asInterface",IBinder::class.java).apply { isAccessible=true }.invoke(null,binder) ?: return
            val method=manager.javaClass.methods.firstOrNull { it.name=="injectInputEvent" && it.parameterTypes.size==2 } ?: return
            inputManager=manager; injectMethod=method
        } catch (_: ReflectiveOperationException) { inputManager=null; injectMethod=null }
          catch (_: SecurityException) { inputManager=null; injectMethod=null }
          catch (_: RuntimeException) { inputManager=null; injectMethod=null }
          catch (_: LinkageError) { inputManager=null; injectMethod=null }
    }

    private fun isAuthorizedCaller():Boolean {
        val processUid=Process.myUid()
        val privilegedProcess=processUid==Process.SHELL_UID || processUid==Process.ROOT_UID
        // Binder.getCallingUid() is the app UID on the client-to-user-service transaction;
        // validating it against the captured Shizuku client UID is the correct IPC boundary.
        return privilegedProcess && trustedCallerUid>=0 && Binder.getCallingUid()==trustedCallerUid
    }

    /** mode 0 is IInputManager.INJECT_INPUT_EVENT_MODE_ASYNC. */
    override fun injectEvent(event: InputEvent, mode: Int): Boolean {
        if(!isAuthorizedCaller() || event !is android.view.MotionEvent || mode!=INJECT_INPUT_EVENT_MODE_ASYNC) return false
        var manager=inputManager; var method=injectMethod
        if(manager==null || method==null) { connectInputManager(); manager=inputManager; method=injectMethod }
        if(manager==null || method==null) return false
        return try { method.invoke(manager,event,INJECT_INPUT_EVENT_MODE_ASYNC) as? Boolean ?: false }
        catch (e: InvocationTargetException) { if(e.targetException is SecurityException) false else false }
        catch (_: ReflectiveOperationException) { inputManager=null; injectMethod=null; false }
        catch (_: SecurityException) { false }
        catch (_: RuntimeException) { inputManager=null; injectMethod=null; false }
        catch (_: LinkageError) { inputManager=null; injectMethod=null; false }
    }

    override fun destroy() {
        if(!isAuthorizedCaller()) return
        inputManager=null; injectMethod=null
        // Shizuku asks user services to terminate explicitly when the client unbinds.
        Thread { android.os.Process.killProcess(android.os.Process.myPid()) }.start()
    }
}
