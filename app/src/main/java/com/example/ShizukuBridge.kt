package com.example

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import rikka.shizuku.Shizuku

object ShizukuBridge {
    private const val TAG = "FPAnim"
    private const val SERVICE_VERSION = 6

    @Volatile var service: ITouchService? = null
    @Volatile var onTouch: ((Boolean) -> Unit)? = null
    @Volatile private var lastInteractive: Boolean = true
    private lateinit var app: Context

    private val listener = object : ITouchListener.Stub() {
        override fun onZoneTouch(down: Boolean) {
            onTouch?.invoke(down)
        }
    }

    private val conn = object : ServiceConnection {
        override fun onServiceConnected(n: ComponentName, b: IBinder) {
            Log.d(TAG, "TouchUserService connected: $n (v$SERVICE_VERSION)")
            val s = ITouchService.Stub.asInterface(b)
            service = s
            runCatching {
                b.linkToDeath({
                    Log.d(TAG, "TouchUserService binder died; scheduling rebind")
                    service = null
                    tryBind()
                }, 0)
            }
            applyZone()
            runCatching { s.setScreenInteractive(lastInteractive) }
            try {
                s.start(listener)
            } catch (e: Exception) {
                Log.d(TAG, "Failed to start TouchUserService listener: ${e.message}")
            }
        }

        override fun onServiceDisconnected(n: ComponentName) {
            Log.d(TAG, "TouchUserService disconnected: $n")
            service = null
            tryBind()
        }
    }

    fun init(ctx: Context) {
        app = ctx.applicationContext
        runCatching {
            val pw = app.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (pw != null) lastInteractive = pw.isInteractive
        }
        Shizuku.addBinderReceivedListenerSticky { tryBind() }
        Shizuku.addRequestPermissionResultListener { _, r ->
            if (r == PackageManager.PERMISSION_GRANTED) tryBind()
        }
    }

    fun setScreenInteractive(interactive: Boolean) {
        lastInteractive = interactive
        runCatching { service?.setScreenInteractive(interactive) }
    }

    fun tryBind() {
        if (!::app.isInitialized) return
        val alive = try {
            Shizuku.pingBinder()
        } catch (_: Exception) {
            false
        }
        val currentBinderAlive = try {
            service?.asBinder()?.isBinderAlive == true
        } catch (_: Exception) {
            false
        }
        if (currentBinderAlive) {
            applyZone()
            runCatching { service?.setScreenInteractive(lastInteractive) }
            runCatching { service?.start(listener) }
            return
        }
        service = null
        if (!alive) return

        val granted = try {
            Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        } catch (_: Exception) {
            false
        }
        if (!granted) return

        try {
            val args = Shizuku.UserServiceArgs(ComponentName(app.packageName, TouchUserService::class.java.name))
                .processNameSuffix("touch")
                .daemon(true)
                .version(SERVICE_VERSION)
            Shizuku.bindUserService(args, conn)
        } catch (e: Exception) {
            Log.d(TAG, "bindUserService error: ${e.message}")
        }
    }

    fun applyZone() {
        if (!::app.isInitialized) return
        val c = Prefs.load(app)
        try {
            // High-sensitivity radius matching the full calibrated sensor area + initial thumb edge contact
            val radius = (c.size * 0.74f).coerceIn(0.11f, 0.32f)
            service?.setZone(c.cx, c.cy, radius)
        } catch (e: Exception) {
            Log.d(TAG, "applyZone error: ${e.message}")
        }
    }

    fun runShellCommand(cmd: String): String {
        tryBind()
        val s = service
        if (s != null) {
            runCatching {
                return s.runDiagnosticCommand(cmd)
            }
        }
        return try {
            val process = ProcessBuilder("/system/bin/sh", "-c", cmd)
                .redirectErrorStream(true)
                .start()
            val out = process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor()
            out.ifBlank { "(Command completed with no output)" }
        } catch (e: Exception) {
            "Shizuku service not connected yet. Open Shizuku and ensure FP Animator is allowed.\nDetails: ${e.message}"
        }
    }
}
