package com.nexus.personalintelligence.core.common.device

import java.util.Locale

object DeviceMetricFormatter {

    /** Formats byte counts into human-readable MB or GB strings, or "unknown" if negative. */
    fun formatBytes(bytes: Long): String {
        if (bytes < 0L) return "unknown"
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1024.0) {
            val gb = mb / 1024.0
            String.format(Locale.US, "%.1f GB", gb)
        } else {
            "${bytes / (1024 * 1024)} MB"
        }
    }

    /** Formats memory info into a summary e.g. "750 MB / 2048 MB (36% free)" or "unknown". */
    fun formatMemorySummary(availBytes: Long, totalBytes: Long): String {
        if (totalBytes <= 0L || availBytes < 0L) return "unknown"
        val availStr = formatBytes(availBytes)
        val totalStr = formatBytes(totalBytes)
        val pctFree = ((availBytes.toDouble() / totalBytes.toDouble()) * 100).toInt()
        return "$availStr / $totalStr ($pctFree% free)"
    }

    /** Formats storage info into a summary e.g. "4.2 GB free / 16.0 GB total" */
    fun formatStorageSummary(freeBytes: Long, totalBytes: Long = -1L): String {
        if (freeBytes < 0L) return "unknown"
        val freeStr = formatBytes(freeBytes)
        return if (totalBytes > 0L) {
            val totalStr = formatBytes(totalBytes)
            "$freeStr free / $totalStr total"
        } else {
            "$freeStr free"
        }
    }

    /** Formats system uptime from milliseconds into "Xd Xh Xm" or "Xh Xm Xs" or "Xs". */
    fun formatUptime(uptimeMs: Long): String {
        if (uptimeMs < 0L) return "unknown"
        val totalSec = uptimeMs / 1000
        val days = totalSec / 86400
        val hours = (totalSec % 86400) / 3600
        val minutes = (totalSec % 3600) / 60
        val seconds = totalSec % 60

        return when {
            days > 0 -> "${days}d ${hours}h ${minutes}m"
            hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else -> "${seconds}s"
        }
    }

    /** Returns a clean network label based on connectivity state, transport, and optional IP. */
    fun formatNetworkLabel(
        isConnected: Boolean,
        isWifi: Boolean,
        isEthernet: Boolean,
        ipAddress: String? = null
    ): String {
        if (!isConnected) return "Disconnected"
        val type = when {
            isEthernet -> "Ethernet"
            isWifi -> "Wi-Fi"
            else -> "Connected"
        }
        val ipPart = if (!ipAddress.isNullOrBlank() && ipAddress != "127.0.0.1") " ($ipAddress)" else ""
        return "$type$ipPart"
    }
}
