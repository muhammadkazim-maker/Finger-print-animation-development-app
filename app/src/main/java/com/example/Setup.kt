package com.example

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.PowerManager
import android.provider.Settings
import rikka.shizuku.Shizuku

object Setup {
    fun shizukuInstalled(c: Context): Boolean =
        runCatching {
            c.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        }.getOrDefault(false)

    fun shizukuRunning(): Boolean =
        runCatching { Shizuku.pingBinder() }.getOrDefault(false)

    fun shizukuAllowed(): Boolean =
        runCatching {
            Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        }.getOrDefault(false)

    fun batteryOk(c: Context): Boolean =
        c.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(c.packageName) == true

    private fun me(c: Context): String =
        ComponentName(c, FpAccessibilityService::class.java).flattenToString()

    fun serviceEnabled(c: Context): Boolean {
        val on = Settings.Secure.getString(
            c.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()
        val expected = me(c)
        val shortName = "${c.packageName}/.FpAccessibilityService"
        return on.split(':').any { it == expected || it == shortName }
    }

    fun enableService(c: Context): Boolean = runCatching {
        if (ShizukuBridge.service?.grantSecureSettings() != true) return false
        val cur = Settings.Secure.getString(
            c.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()
        if (me(c) !in cur.split(':')) {
            Settings.Secure.putString(
                c.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                if (cur.isEmpty()) me(c) else "$cur:${me(c)}"
            )
        }
        Settings.Secure.putInt(c.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 1)
        true
    }.getOrDefault(false)

    fun allRequiredPermissionsGranted(c: Context): Boolean =
        shizukuInstalled(c) &&
            shizukuRunning() &&
            shizukuAllowed() &&
            serviceEnabled(c) &&
            batteryOk(c)

    fun isFullSetupComplete(c: Context): Boolean =
        allRequiredPermissionsGranted(c) &&
            Prefs.isBgDone(c) &&
            Prefs.isStockStepDone(c) &&
            Prefs.isCalibDone(c) &&
            Prefs.isAnimStepDone(c)
}
