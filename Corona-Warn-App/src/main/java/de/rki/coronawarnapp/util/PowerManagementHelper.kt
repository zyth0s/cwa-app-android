package de.rki.coronawarnapp.util

import android.content.Context
import android.os.Build
import android.os.PowerManager

object PowerManagementHelper {
    /**
     * Checks if app is excluded from battery optimizations
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     *  Checks if device supports locationless scanning.
     */
    fun deviceSupportsLocationlessScanning(context: Context): Boolean {
        return Build.VERSION.SDK_INT > Build.VERSION_CODES.Q
    }
}
