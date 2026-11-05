package com.nexus.personalintelligence.core.common.device

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceMetricFormatterTest {

    @Test
    fun testFormatBytes() {
        assertEquals("unknown", DeviceMetricFormatter.formatBytes(-1L))
        assertEquals("0 MB", DeviceMetricFormatter.formatBytes(0L))
        assertEquals("512 MB", DeviceMetricFormatter.formatBytes(512L * 1024L * 1024L))
        // 2 GB
        assertEquals("2.0 GB", DeviceMetricFormatter.formatBytes(2048L * 1024L * 1024L))
        // 16 GB
        assertEquals("16.0 GB", DeviceMetricFormatter.formatBytes(16L * 1024L * 1024L * 1024L))
    }

    @Test
    fun testFormatMemorySummary() {
        // 750 MB avail out of 2048 MB total -> 36% free
        val avail = 750L * 1024L * 1024L
        val total = 2048L * 1024L * 1024L
        val summary = DeviceMetricFormatter.formatMemorySummary(avail, total)
        assertEquals("750 MB / 2.0 GB (36% free)", summary)

        // Invalid cases
        assertEquals("unknown", DeviceMetricFormatter.formatMemorySummary(-1L, total))
        assertEquals("unknown", DeviceMetricFormatter.formatMemorySummary(avail, 0L))
    }

    @Test
    fun testFormatStorageSummary() {
        val free = 4L * 1024L * 1024L * 1024L
        val total = 16L * 1024L * 1024L * 1024L
        assertEquals("4.0 GB free / 16.0 GB total", DeviceMetricFormatter.formatStorageSummary(free, total))

        // Without total
        assertEquals("4.0 GB free", DeviceMetricFormatter.formatStorageSummary(free, -1L))

        // Negative
        assertEquals("unknown", DeviceMetricFormatter.formatStorageSummary(-1L, total))
    }

    @Test
    fun testFormatUptime() {
        assertEquals("unknown", DeviceMetricFormatter.formatUptime(-1L))
        assertEquals("45s", DeviceMetricFormatter.formatUptime(45_000L))
        assertEquals("12m 30s", DeviceMetricFormatter.formatUptime((12 * 60 + 30) * 1000L))
        assertEquals("3h 15m 0s", DeviceMetricFormatter.formatUptime((3 * 3600 + 15 * 60) * 1000L))
        assertEquals("2d 4h 10m", DeviceMetricFormatter.formatUptime((2 * 86400 + 4 * 3600 + 10 * 60) * 1000L))
    }

    @Test
    fun testFormatNetworkLabel() {
        assertEquals("Disconnected", DeviceMetricFormatter.formatNetworkLabel(isConnected = false, isWifi = false, isEthernet = false))
        assertEquals("Wi-Fi (192.168.1.100)", DeviceMetricFormatter.formatNetworkLabel(isConnected = true, isWifi = true, isEthernet = false, ipAddress = "192.168.1.100"))
        assertEquals("Ethernet", DeviceMetricFormatter.formatNetworkLabel(isConnected = true, isWifi = false, isEthernet = true, ipAddress = "127.0.0.1"))
        assertEquals("Connected", DeviceMetricFormatter.formatNetworkLabel(isConnected = true, isWifi = false, isEthernet = false))
    }
}
