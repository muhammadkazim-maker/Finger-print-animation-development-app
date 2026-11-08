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
    private const val SERVICE_VERSION = 12

    @Volatile var service: ITouchService? = null
    @Volatile var onTouch: ((Boolean) -> Unit)? = null
    @Volatile var onFingerprintError: (() -> Unit)? = null
    @Volatile var onBiometricPromptStateChanged: ((Boolean) -> Unit)? = null
    @Volatile private var lastInteractive: Boolean = true
    @Volatile private var bindInFlight: Boolean = false
    private lateinit var app: Context

    private val ipcExecutor = java.util.concurrent.Executors.newSingleThreadExecutor { r ->
        Thread(r, "FPAnim-ShizukuIPC").apply { isDaemon = true }
    }

    private val listener = object : ITouchListener.Stub() {
        override fun onZoneTouch(down: Boolean) {
            onTouch?.invoke(down)
        }

        override fun onFingerprintError() {
            onFingerprintError?.invoke()
        }

        override fun onBiometricPromptStateChanged(active: Boolean) {
            onBiometricPromptStateChanged?.invoke(active)
        }
    }

    private val conn = object : ServiceConnection {
        override fun onServiceConnected(n: ComponentName, b: IBinder) {
            Log.d(TAG, "TouchUserService connected: $n (v$SERVICE_VERSION)")
            val s = ITouchService.Stub.asInterface(b)
            service = s
            bindInFlight = false
            runCatching {
                b.linkToDeath({
                    Log.d(TAG, "TouchUserService binder died; scheduling rebind")
                    service = null
                    bindInFlight = false
                    tryBind()
                }, 0)
            }
            ipcExecutor.execute {
                applyZoneInternal(s)
                runCatching { s.setScreenInteractive(lastInteractive) }
                try {
                    s.start(listener)
                } catch (e: Exception) {
                    Log.d(TAG, "Failed to start TouchUserService listener: ${e.message}")
                }
            }
        }

        override fun onServiceDisconnected(n: ComponentName) {
            Log.d(TAG, "TouchUserService disconnected: $n")
            service = null
            bindInFlight = false
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
        val s = service ?: return
        ipcExecutor.execute {
            runCatching { s.setScreenInteractive(interactive) }
        }
    }

    fun tryBind() {
        if (!::app.isInitialized) return
        val current = service
        if (current != null) {
            val isAlive = try {
                current.asBinder().isBinderAlive
            } catch (_: Exception) {
                false
            }
            if (isAlive) {
                // Already bound and alive — fast no-op with zero main-thread Binder IPC!
                return
            }
            service = null
        }
        if (bindInFlight) return

        ipcExecutor.execute {
            val alive = try {
                Shizuku.pingBinder()
            } catch (_: Exception) {
                false
            }
            if (!alive) return@execute

            val granted = try {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } catch (_: Exception) {
                false
            }
            if (!granted) return@execute

            bindInFlight = true
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                try {
                    val args = Shizuku.UserServiceArgs(
                        ComponentName(app.packageName, TouchUserService::class.java.name)
                    )
                        .processNameSuffix("touch")
                        .daemon(true)
                        .version(SERVICE_VERSION)
                    Shizuku.bindUserService(args, conn)
                } catch (e: Exception) {
                    bindInFlight = false
                    Log.d(TAG, "bindUserService error: ${e.message}")
                }
            }
        }
    }

    private fun applyZoneInternal(target: ITouchService?) {
        if (!::app.isInitialized || target == null) return
        val c = Prefs.load(app)
        try {
            val radius = (c.sensorSize * 0.5f).coerceIn(0.02f, 0.18f)
            target.setZone(c.cx, c.cy, radius)
        } catch (e: Exception) {
            Log.d(TAG, "applyZone error: ${e.message}")
        }
    }

    fun isConnectedAndActive(): Boolean {
        val current = service ?: return false
        return try {
            current.asBinder().isBinderAlive
        } catch (_: Exception) {
            false
        }
    }

    fun applyZone() {
        if (!::app.isInitialized) return
        val s = service ?: return
        ipcExecutor.execute {
            applyZoneInternal(s)
        }
    }

    fun runShellCommand(cmd: String): String {
        val s = service
        if (s != null) {
            runCatching {
                if (s.asBinder().isBinderAlive) {
                    return s.runDiagnosticCommand(cmd)
                }
            }
        }
        return try {
            val process = ProcessBuilder("/system/bin/sh", "-c", cmd)
                .redirectErrorStream(true)
                .start()
            val out = process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor()
            out.ifBlank { "(Command completed in standalone mode)" }
        } catch (e: Exception) {
            "Standalone shell output: ${e.message}"
        }
    }
}
