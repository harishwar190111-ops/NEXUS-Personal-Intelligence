package com.nexus.personalintelligence.tv.data

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import com.nexus.personalintelligence.core.common.device.DeviceMetricFormatter

/** Real values read from this device. Useful as evidence for the OS-feasibility research (SoC, board, RAM). */
object DeviceInfo {
    fun collect(context: Context): List<Pair<String, String>> {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val mem = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(mem)
        val dm = context.resources.displayMetrics
        val free = try {
            StatFs(Environment.getDataDirectory().path).availableBytes
        } catch (e: Exception) {
            -1L
        }
        return listOf(
            "Manufacturer" to Build.MANUFACTURER,
            "Model" to Build.MODEL,
            "Device" to Build.DEVICE,
            "Product" to Build.PRODUCT,
            "Board" to Build.BOARD,
            "Hardware" to Build.HARDWARE,
            "Bootloader version" to Build.BOOTLOADER,
            "Android" to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            "Security patch" to (Build.VERSION.SECURITY_PATCH ?: "unknown"),
            "Build" to Build.DISPLAY,
            "CPU ABIs" to Build.SUPPORTED_ABIS.joinToString(", "),
            "RAM total" to DeviceMetricFormatter.formatBytes(mem.totalMem),
            "RAM available now" to DeviceMetricFormatter.formatBytes(mem.availMem),
            "RAM summary" to DeviceMetricFormatter.formatMemorySummary(mem.availMem, mem.totalMem),
            "Low-RAM device flag" to (am?.isLowRamDevice ?: false).toString(),
            "Display" to "${dm.widthPixels} x ${dm.heightPixels} px, ${dm.densityDpi} dpi",
            "Free storage" to DeviceMetricFormatter.formatStorageSummary(free),
        )
    }
}
