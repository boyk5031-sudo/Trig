package com.trigger.automation.engine

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import rikka.shizuku.Shizuku
import java.util.concurrent.atomic.AtomicBoolean

/** Owns the lifecycle of the shell-UID UserService and reconnects after binder death. */
class ShizukuServiceConnection(context: Context) : ServiceConnection, AutoCloseable {
    enum class State { DISCONNECTED, BINDING, CONNECTED, PERMISSION_REQUIRED, CLOSED }
    private val app=context.applicationContext
    private val handler=Handler(Looper.getMainLooper())
    private val args=Shizuku.UserServiceArgs(ComponentName(app,ShizukuUserService::class.java))
        .daemon(false).processNameSuffix("input-event").tag("trigger-input-event").version(1)
    private val closed=AtomicBoolean(false)
    @Volatile private var remote: IInputEventService?=null
    @Volatile var state: State=State.DISCONNECTED
        private set
    @Volatile var onStateChanged: ((State)->Unit)?=null
    private var retryCount=0
    @Volatile private var permissionRequestPending=false
    private val permissionListener=Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if(requestCode==PERMISSION_REQUEST_CODE) { permissionRequestPending=false; if(grantResult==android.content.pm.PackageManager.PERMISSION_GRANTED) connect() else setState(State.PERMISSION_REQUIRED) }
    }
    init { Shizuku.addRequestPermissionResultListener(permissionListener) }

    fun requestPermission(requestCode: Int) { if(Shizuku.pingBinder()) Shizuku.requestPermission(requestCode) }

    @Synchronized fun connect(): Boolean {
        if(closed.get()) return false
        if(state==State.CONNECTED) return true
        val ready=try { Shizuku.pingBinder() } catch (_:Throwable) { false }
        if(!ready) { setState(State.DISCONNECTED); scheduleReconnect(); return false }
        val granted=try { Shizuku.checkSelfPermission()==android.content.pm.PackageManager.PERMISSION_GRANTED } catch (_:Throwable) { false }
        if(!granted) {
            setState(State.PERMISSION_REQUIRED)
            if(!permissionRequestPending) { permissionRequestPending=true; try { Shizuku.requestPermission(PERMISSION_REQUEST_CODE) } catch (_:Exception) { permissionRequestPending=false } }
            return false
        }
        setState(State.BINDING)
        return try { Shizuku.bindUserService(args,this); true }
        catch(e:Exception) { Log.w(TAG,"Unable to bind Shizuku input service",e); setState(State.DISCONNECTED); scheduleReconnect(); false }
    }

    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
        if(closed.get() || binder==null) { setState(State.DISCONNECTED); scheduleReconnect(); return }
        try {
            val api=IInputEventService.Stub.asInterface(binder)
            binder.linkToDeath({ remote=null; setState(State.DISCONNECTED); scheduleReconnect() },0)
            remote=api; retryCount=0; setState(State.CONNECTED)
        } catch(e:Exception) { remote=null; setState(State.DISCONNECTED); scheduleReconnect() }
    }
    override fun onServiceDisconnected(name: ComponentName?) { remote=null; setState(State.DISCONNECTED); scheduleReconnect() }

    fun inject(event: android.view.InputEvent): Boolean {
        val target=remote ?: return false
        return try { target.injectEvent(event, MODE_ASYNC) }
        catch (_: android.os.DeadObjectException) { remote=null; setState(State.DISCONNECTED); scheduleReconnect(); false }
        catch (_: android.os.RemoteException) { false }
    }

    @Synchronized private fun scheduleReconnect() {
        if(closed.get() || state==State.PERMISSION_REQUIRED) return
        val delay=(500L shl retryCount.coerceAtMost(5)).coerceAtMost(15_000L); retryCount++
        handler.removeCallbacks(reconnect); handler.postDelayed(reconnect,delay)
    }
    private val reconnect=Runnable { if(!closed.get()) connect() }
    private fun setState(value: State) { state=value; handler.post { onStateChanged?.invoke(value) } }

    @Synchronized override fun close() {
        if(!closed.compareAndSet(false,true)) return
        handler.removeCallbacks(reconnect); Shizuku.removeRequestPermissionResultListener(permissionListener); permissionRequestPending=false; val old=remote; remote=null
        try { old?.destroy() } catch (_:Exception) { }
        try { Shizuku.unbindUserService(args,this,true) } catch (_:Exception) { }
        setState(State.CLOSED)
    }
    companion object { private const val TAG="TriggerShizuku"; private const val PERMISSION_REQUEST_CODE=6204; const val MODE_ASYNC=0 }
}
