package com.example

import android.os.Process as AndroidProcess
import android.util.Log
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import kotlin.system.exitProcess

class TouchUserService : ITouchService.Stub() {
    @Volatile private var listener: ITouchListener? = null
    @Volatile private var zone = floatArrayOf(0.5f, 0.82f, 0.09f) // cx, cy, r (r = fraction of width)
    @Volatile private var screenInteractive = true
    @Volatile private var running = false
    @Volatile private var lastDispatched = false
    @Volatile private var proc: java.lang.Process? = null
    @Volatile private var logcatProc: java.lang.Process? = null

    override fun destroy() {
        stop()
        exitProcess(0)
    }

    override fun setZone(cx: Float, cy: Float, r: Float) {
        zone = floatArrayOf(cx, cy, r)
        Log.d(TAG, "setZone: cx=$cx cy=$cy r=$r")
    }

    override fun setScreenInteractive(interactive: Boolean) {
        screenInteractive = interactive
    }

    override fun grantSecureSettings(): Boolean = try {
        Runtime.getRuntime().exec(
            arrayOf(
                "pm",
                "grant",
                BuildConfig.APPLICATION_ID,
                "android.permission.WRITE_SECURE_SETTINGS"
            )
        ).waitFor() == 0
    } catch (e: Exception) {
        Log.d(TAG, "grantSecureSettings failed: ${e.message}")
        false
    }

    override fun runDiagnosticCommand(cmd: String): String {
        return try {
            val process = ProcessBuilder("/system/bin/sh", "-c", cmd)
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor()
            output.ifBlank { "(Command completed with no output)" }
        } catch (e: Exception) {
            "Error executing command: ${e.message}"
        }
    }

    override fun start(l: ITouchListener) {
        listener = l
        if (running) return
        running = true
        val devices = findInputDevices()
        val mainPanel = devices.mainPanel
        if (mainPanel != null) {
            Thread {
                runCatching {
                    AndroidProcess.setThreadPriority(AndroidProcess.THREAD_PRIORITY_URGENT_DISPLAY)
                }
                try {
                    readMainPanelLoop(mainPanel)
                } finally {
                    running = false
                }
            }.apply {
                name = "FPAnim-TouchRaw"
                isDaemon = true
                priority = Thread.MAX_PRIORITY
            }.start()
        }

        // Also listen to any dedicated FOD / UDFPS / fingerprint input device for Always-On Display
        for (fodPath in devices.fodPaths) {
            Thread {
                runCatching {
                    AndroidProcess.setThreadPriority(AndroidProcess.THREAD_PRIORITY_URGENT_DISPLAY)
                }
                readFodDeviceLoop(fodPath)
            }.apply {
                name = "FPAnim-FodRaw"
                isDaemon = true
                priority = Thread.MAX_PRIORITY
            }.start()
        }

        // Real-time fingerprint mismatch / error logcat stream monitor
        Thread {
            try {
                monitorFingerprintErrors()
            } catch (t: Throwable) {
                Log.d(TAG, "monitorFingerprintErrors ended: ${t.message}")
            }
        }.apply {
            name = "FPAnim-FpErrorWatch"
            isDaemon = true
            priority = Thread.MIN_PRIORITY
        }.start()
    }

    override fun stop() {
        running = false
        proc?.destroy()
        proc = null
        logcatProc?.destroy()
        logcatProc = null
    }

    @Synchronized
    private fun dispatchTouch(inside: Boolean) {
        if (inside != lastDispatched) {
            lastDispatched = inside
            try {
                listener?.onZoneTouch(inside)
            } catch (_: Exception) {
            }
        }
    }

    private fun monitorFingerprintErrors() {
        val cmd = arrayOf(
            "logcat", "-T", "1", "-v", "brief",
            "FingerprintService:V", "Fingerprint21:V", "FingerprintProvider:V",
            "BiometricService:V", "BiometricScheduler:V", "AuthenticationClient:V",
            "AcquisitionClient:V", "KeyguardUpdateMonitor:V", "KeyguardBiometricLockoutLogger:V",
            "AuthController:V", "UdfpsController:V", "UdfpsControllerOverlay:V",
            "FodController:V", "FingerprintHal:V", "BiometricPrompt:V",
            "FingerprintManager:V", "*:S"
        )
        val process = try {
            ProcessBuilder(*cmd).redirectErrorStream(true).start().also { logcatProc = it }
        } catch (_: Exception) {
            return
        }
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        try {
            while (running) {
                val line = reader.readLine() ?: break
                val lower = line.lowercase()

                // 1. Detect genuine mismatch / recognition failure / bad acquisition events
                if (lower.contains("onauthenticationfailed") ||
                    lower.contains("onauthenticated(false)") ||
                    lower.contains("authenticated=false") ||
                    lower.contains("authenticated: false") ||
                    lower.contains("authenticated(false)") ||
                    lower.contains("auth_failed") ||
                    lower.contains("handleauthenticated(false)") ||
                    lower.contains("onfingerprintauthenticated(false)") ||
                    lower.contains("handlefingerprintauthfailed") ||
                    lower.contains("onbiometricauthfailed") ||
                    lower.contains("fingerprint_acquired_partial") ||
                    lower.contains("fingerprint_acquired_insufficient") ||
                    lower.contains("fingerprint_acquired_imager_dirty") ||
                    lower.contains("fingerprint_acquired_too_fast") ||
                    lower.contains("fingerprint_acquired_too_slow") ||
                    lower.contains("not recognized") ||
                    lower.contains("mismatch") ||
                    lower.contains("not match") ||
                    lower.contains("try again")
                ) {
                    try {
                        listener?.onFingerprintError()
                    } catch (_: Exception) {}
                }

                // 2. Detect when an app on the UNLOCKED phone starts or ends biometric verification.
                // Ignore KeyguardUpdateMonitor start events because those belong to the lock screen, not unlocked BiometricPrompt.
                val isKeyguardLine = lower.contains("keyguardupdatemonitor") ||
                    lower.contains("owner=com.android.systemui") ||
                    lower.contains("keyguard")

                if (!isKeyguardLine && (
                        lower.contains("showauthenticationdialog") ||
                        lower.contains("showdialog") ||
                        lower.contains("authcontainerview") ||
                        lower.contains("startauthentication") ||
                        lower.contains("prepareforauthentication") ||
                        lower.contains("starting authentication") ||
                        lower.contains("showudfpsoverlay")
                    )
                ) {
                    try {
                        listener?.onBiometricPromptStateChanged(true)
                    } catch (_: Exception) {}
                } else if (
                    lower.contains("stopauthentication") ||
                    lower.contains("cancelauthentication") ||
                    lower.contains("dismissauthenticationdialog") ||
                    lower.contains("hideudfps") ||
                    lower.contains("onauthenticationsucceeded") ||
                    lower.contains("onauthenticated(true)") ||
                    lower.contains("authenticated=true") ||
                    lower.contains("authenticated: true") ||
                    lower.contains("ondismissed") ||
                    lower.contains("finishing client") ||
                    lower.contains("client stopped")
                ) {
                    try {
                        listener?.onBiometricPromptStateChanged(false)
                    } catch (_: Exception) {}
                }
            }
        } finally {
            runCatching { process.destroy() }
        }
    }

    private class Panel(val path: String, val maxX: Int, val maxY: Int)
    private class InputDevices(val mainPanel: Panel?, val fodPaths: List<String>)

    private fun findInputDevices(): InputDevices {
        val text = try {
            Runtime.getRuntime().exec(arrayOf("getevent", "-p"))
                .inputStream.bufferedReader().readText()
        } catch (e: Exception) {
            Log.d(TAG, "getevent -p failed: ${e.message}")
            return InputDevices(null, emptyList())
        }
        val abs = Regex("""^\s*(?:ABS \(0003\):\s*)?([0-9a-fA-F]{4})\s*:\s*value\s+-?\d+,\s*min\s+-?\d+,\s*max\s+(\d+)""")
        val panels = mutableListOf<Panel>()
        val fodCandidates = mutableListOf<String>()

        var path: String? = null
        var devName = ""
        var mx = 0
        var my = 0
        fun flush() {
            val p = path ?: return
            if (mx > 0 && my > 0) {
                panels += Panel(p, mx, my)
            }
            val lower = devName.lowercase()
            if (lower.contains("fod") ||
                lower.contains("udfps") ||
                lower.contains("fp") ||
                lower.contains("finger") ||
                lower.contains("goodix") ||
                lower.contains("focal") ||
                lower.contains("silead") ||
                lower.contains("chipone") ||
                lower.contains("egis") ||
                lower.contains("cdfinger") ||
                lower.contains("ets")
            ) {
                if (!lower.contains("power") && !lower.contains("volume") && !lower.contains("headset")) {
                    fodCandidates += p
                }
            }
        }

        for (line in text.lines()) {
            if (line.startsWith("add device")) {
                flush()
                path = line.substringAfter(": ").trim()
                devName = ""
                mx = 0
                my = 0
            } else if (line.trimStart().startsWith("name:")) {
                devName = line.substringAfter("name:").trim().trim('"')
            } else {
                abs.find(line)?.let {
                    when (it.groupValues[1].lowercase()) {
                        "0035" -> mx = it.groupValues[2].toInt() // ABS_MT_POSITION_X
                        "0036" -> my = it.groupValues[2].toInt() // ABS_MT_POSITION_Y
                        "0000" -> if (mx == 0) mx = it.groupValues[2].toInt() // ABS_X fallback
                        "0001" -> if (my == 0) my = it.groupValues[2].toInt() // ABS_Y fallback
                    }
                }
            }
        }
        flush()

        val best = panels.maxByOrNull { it.maxX.toLong() * it.maxY }
        val extraFod = fodCandidates.filter { it != best?.path }.distinct()
        Log.d(TAG, "mainPanel=${best?.path} (${best?.maxX}x${best?.maxY}), extraFod=$extraFod")
        return InputDevices(best, extraFod)
    }

    private fun readMainPanelLoop(panel: Panel) {
        val structSize = if (AndroidProcess.is64Bit()) 24 else 16
        val typeOffset = if (structSize == 24) 16 else 8

        while (running) {
            val fis = try {
                FileInputStream(panel.path)
            } catch (e: Exception) {
                Log.d(TAG, "Direct FileInputStream(${panel.path}) unavailable (${e.message}), using getevent fallback")
                null
            }

            if (fis != null) {
                try {
                    readRawBinaryStream(fis, panel, structSize, typeOffset)
                } catch (e: Exception) {
                    Log.d(TAG, "readRawBinaryStream error: ${e.message}")
                } finally {
                    runCatching { fis.close() }
                }
            } else {
                readGeteventFallback(panel)
            }

            if (running) {
                try {
                    Thread.sleep(100L)
                } catch (_: InterruptedException) {
                    break
                }
            }
        }
    }

    private fun readRawBinaryStream(
        fis: FileInputStream,
        panel: Panel,
        structSize: Int,
        typeOffset: Int
    ) {
        val n = 16
        val x = IntArray(n) { -1 }
        val y = IntArray(n) { -1 }
        val on = BooleanArray(n)
        val hasXThisTouch = BooleanArray(n)
        val hasYThisTouch = BooleanArray(n)
        var slot = 0
        var fodKeyHeld = false

        fun isInsideZone(px: Int, py: Int, aodMode: Boolean, currentlyInside: Boolean): Boolean {
            if (px < 0 || py < 0) return false
            val z = zone
            // Spatial hysteresis: once finger is inside the sensor zone, allow 28% wider stay-radius
            // so thumb roll or capacitive jitter never causes the animation to flicker off mid-press.
            val aodScale = if (aodMode) 1.35f else 1.0f
            val hysteresisScale = if (currentlyInside) 1.28f else 1.0f
            val rPx = z[2] * aodScale * hysteresisScale * panel.maxX
            val cxPx = z[0] * panel.maxX
            val cyPx = z[1] * panel.maxY
            val dx = px - cxPx
            val dy = py - cyPx
            return dx * dx + dy * dy <= rPx * rPx
        }

        fun evaluateFrame() {
            val aod = !screenInteractive
            val wasInside = lastDispatched
            var inside = fodKeyHeld
            if (!inside) {
                for (i in 0 until n) {
                    if (on[i]) {
                        if (isInsideZone(x[i], y[i], aod, wasInside)) {
                            inside = true
                            break
                        } else if (aod && !hasXThisTouch[i] && !hasYThisTouch[i]) {
                            // In AOD low-power FOD gesture mode, the touch IC often emits BTN_TOUCH
                            // or ABS_MT_TRACKING_ID only when the hardware FOD sensor area is pressed.
                            inside = true
                            break
                        }
                    }
                }
            }
            dispatchTouch(inside)
        }

        val buf = ByteArray(structSize * 64)
        var leftover = 0

        while (running) {
            val readCount = fis.read(buf, leftover, buf.size - leftover)
            if (readCount <= 0) break
            val total = leftover + readCount
            var offset = 0
            var dirty = false

            while (offset + structSize <= total) {
                val type = readUInt16LE(buf, offset + typeOffset)
                val code = readUInt16LE(buf, offset + typeOffset + 2)
                val value = readInt32LE(buf, offset + typeOffset + 4)

                when (type) {
                    EV_SYN -> {
                        if (code == SYN_REPORT) {
                            // Evaluate at the atomic end of the kernel input frame so X, Y, and
                            // tracking state are 100% synchronized and never mix stale coordinates.
                            evaluateFrame()
                            dirty = false
                        }
                    }
                    EV_ABS -> {
                        when (code) {
                            ABS_MT_SLOT -> {
                                slot = value.coerceIn(0, n - 1)
                            }
                            ABS_MT_TRACKING_ID -> {
                                if (value == -1) {
                                    on[slot] = false
                                    x[slot] = -1
                                    y[slot] = -1
                                    hasXThisTouch[slot] = false
                                    hasYThisTouch[slot] = false
                                } else {
                                    on[slot] = true
                                    x[slot] = -1
                                    y[slot] = -1
                                    hasXThisTouch[slot] = false
                                    hasYThisTouch[slot] = false
                                }
                                dirty = true
                            }
                            ABS_MT_POSITION_X, ABS_X -> {
                                x[slot] = value
                                on[slot] = true
                                hasXThisTouch[slot] = true
                                dirty = true
                                if (!lastDispatched && hasYThisTouch[slot]) {
                                    evaluateFrame()
                                }
                            }
                            ABS_MT_POSITION_Y, ABS_Y -> {
                                y[slot] = value
                                on[slot] = true
                                hasYThisTouch[slot] = true
                                dirty = true
                                if (!lastDispatched && hasXThisTouch[slot]) {
                                    evaluateFrame()
                                }
                            }
                        }
                    }
                    EV_KEY -> {
                        when {
                            code == BTN_TOUCH || code == BTN_TOOL_FINGER -> {
                                if (value == 0) {
                                    for (i in 0 until n) {
                                        on[i] = false
                                        x[i] = -1
                                        y[i] = -1
                                        hasXThisTouch[i] = false
                                        hasYThisTouch[i] = false
                                    }
                                    fodKeyHeld = false
                                } else if (value == 1) {
                                    on[slot] = true
                                }
                                dirty = true
                            }
                            isDedicatedFodKeyCode(code) -> {
                                fodKeyHeld = if (code == KEY_FOD_UP) false else (value != 0)
                                dirty = true
                                evaluateFrame()
                            }
                        }
                    }
                }
                offset += structSize
            }

            if (dirty) {
                evaluateFrame()
            }

            leftover = total - offset
            if (leftover > 0 && offset > 0) {
                System.arraycopy(buf, offset, buf, 0, leftover)
            }
        }
    }

    private fun readFodDeviceLoop(fodPath: String) {
        val structSize = if (AndroidProcess.is64Bit()) 24 else 16
        val typeOffset = if (structSize == 24) 16 else 8
        val buf = ByteArray(structSize * 16)

        while (running) {
            val fis = try {
                FileInputStream(fodPath)
            } catch (_: Exception) {
                return
            }
            var leftover = 0
            try {
                while (running) {
                    val r = fis.read(buf, leftover, buf.size - leftover)
                    if (r <= 0) break
                    val total = leftover + r
                    var offset = 0
                    while (offset + structSize <= total) {
                        val type = readUInt16LE(buf, offset + typeOffset)
                        val code = readUInt16LE(buf, offset + typeOffset + 2)
                        val value = readInt32LE(buf, offset + typeOffset + 4)
                        if (type == EV_KEY && code != 116 && code != 115 && code != 114) {
                            if (code == KEY_FOD_UP) {
                                dispatchTouch(false)
                            } else {
                                dispatchTouch(value != 0)
                            }
                        }
                        offset += structSize
                    }
                    leftover = total - offset
                    if (leftover > 0 && offset > 0) {
                        System.arraycopy(buf, offset, buf, 0, leftover)
                    }
                }
            } catch (_: Exception) {
            } finally {
                runCatching { fis.close() }
            }
            if (running) {
                try {
                    Thread.sleep(250L)
                } catch (_: InterruptedException) {
                    break
                }
            }
        }
    }

    private fun readGeteventFallback(panel: Panel) {
        val p = try {
            ProcessBuilder("getevent", "-l", panel.path).redirectErrorStream(true).start()
        } catch (e: Exception) {
            return
        }
        proc = p
        val n = 16
        val x = IntArray(n) { -1 }
        val y = IntArray(n) { -1 }
        val on = BooleanArray(n)
        var slot = 0

        fun evaluateZone() {
            val z = zone
            val aod = !screenInteractive
            val aodScale = if (aod) 1.35f else 1.0f
            val hysteresisScale = if (lastDispatched) 1.28f else 1.0f
            val rPx = z[2] * aodScale * hysteresisScale * panel.maxX
            val cxPx = z[0] * panel.maxX
            val cyPx = z[1] * panel.maxY
            val rSq = rPx * rPx
            var inside = false
            for (i in 0 until n) {
                if (on[i]) {
                    if (x[i] >= 0 && y[i] >= 0) {
                        val dx = x[i] - cxPx
                        val dy = y[i] - cyPx
                        if (dx * dx + dy * dy <= rSq) {
                            inside = true
                            break
                        }
                    } else if (aod) {
                        inside = true
                        break
                    }
                }
            }
            dispatchTouch(inside)
        }

        try {
            val reader = BufferedReader(InputStreamReader(p.inputStream), 128)
            while (running) {
                val line = reader.readLine() ?: break
                val len = line.length
                var end3 = len - 1
                while (end3 >= 0 && line[end3] <= ' ') end3--
                if (end3 < 0) continue
                var start3 = end3
                while (start3 >= 0 && line[start3] > ' ') start3--
                val valStart = start3 + 1
                val valEnd = end3 + 1

                var end2 = start3
                while (end2 >= 0 && line[end2] <= ' ') end2--
                if (end2 < 0) continue
                var start2 = end2
                while (start2 >= 0 && line[start2] > ' ') start2--
                val codeStart = start2 + 1
                val codeEnd = end2 + 1

                if (line.regionMatches(codeStart, "SYN_REPORT", 0, 10) && codeEnd - codeStart == 10) {
                    evaluateZone()
                } else if (line.regionMatches(codeStart, "ABS_MT_POSITION_X", 0, 17) && codeEnd - codeStart == 17) {
                    val v = parseHexInt(line, valStart, valEnd)
                    if (v != Int.MIN_VALUE) {
                        x[slot] = v
                        on[slot] = true
                    }
                } else if (line.regionMatches(codeStart, "ABS_MT_POSITION_Y", 0, 17) && codeEnd - codeStart == 17) {
                    val v = parseHexInt(line, valStart, valEnd)
                    if (v != Int.MIN_VALUE) {
                        y[slot] = v
                        on[slot] = true
                    }
                } else if (line.regionMatches(codeStart, "ABS_MT_TRACKING_ID", 0, 18) && codeEnd - codeStart == 18) {
                    val v = parseHexInt(line, valStart, valEnd)
                    on[slot] = v != -1 && v != Int.MIN_VALUE
                } else if (line.regionMatches(codeStart, "ABS_MT_SLOT", 0, 11) && codeEnd - codeStart == 11) {
                    val v = parseHexInt(line, valStart, valEnd)
                    if (v != Int.MIN_VALUE) {
                        slot = v.coerceIn(0, n - 1)
                    }
                } else if (line.regionMatches(codeStart, "BTN_TOUCH", 0, 9) && codeEnd - codeStart == 9) {
                    val isUp = (valEnd - valStart == 2 && line.regionMatches(valStart, "UP", 0, 2, ignoreCase = true)) ||
                        (valEnd - valStart == 8 && line.regionMatches(valStart, "00000000", 0, 8))
                    if (isUp) {
                        for (i in 0 until n) on[i] = false
                    } else {
                        on[slot] = true
                    }
                }
            }
        } catch (_: Exception) {
        } finally {
            runCatching { p.destroy() }
        }
    }

    private fun isDedicatedFodKeyCode(code: Int): Boolean {
        return code == 0x0152 || // 338 KEY_FOD / BTN_TOOL_QUINTTAP
            code == 0x021e ||    // 542 KEY_FOD_DOWN
            code == 0x021f ||    // 543 KEY_FOD_UP
            code == 0x0220 ||    // 544 BTN_INFO / FOD
            code == 0x01c3 ||    // 451 FOD
            code in 0x02e8..0x02f8 // 744..760 UDFPS vendor range
    }

    private fun readUInt16LE(b: ByteArray, off: Int): Int {
        return (b[off].toInt() and 0xFF) or ((b[off + 1].toInt() and 0xFF) shl 8)
    }

    private fun readInt32LE(b: ByteArray, off: Int): Int {
        return (b[off].toInt() and 0xFF) or
            ((b[off + 1].toInt() and 0xFF) shl 8) or
            ((b[off + 2].toInt() and 0xFF) shl 16) or
            ((b[off + 3].toInt() and 0xFF) shl 24)
    }

    private fun parseHexInt(s: String, start: Int, end: Int): Int {
        if (start >= end) return Int.MIN_VALUE
        var res = 0L
        for (i in start until end) {
            val c = s[i]
            val d = when (c) {
                in '0'..'9' -> c - '0'
                in 'a'..'f' -> c - 'a' + 10
                in 'A'..'F' -> c - 'A' + 10
                else -> return Int.MIN_VALUE
            }
            res = (res shl 4) or d.toLong()
        }
        return res.toInt()
    }

    companion object {
        private const val TAG = "FPAnim"

        private const val EV_SYN = 0x00
        private const val EV_KEY = 0x01
        private const val EV_ABS = 0x03

        private const val SYN_REPORT = 0x00

        private const val ABS_X = 0x0000
        private const val ABS_Y = 0x0001
        private const val ABS_MT_SLOT = 0x002f
        private const val ABS_MT_POSITION_X = 0x0035
        private const val ABS_MT_POSITION_Y = 0x0036
        private const val ABS_MT_TRACKING_ID = 0x0039

        private const val BTN_TOOL_FINGER = 0x0145
        private const val BTN_TOUCH = 0x014a
        private const val KEY_FOD_UP = 0x021f
    }
}
