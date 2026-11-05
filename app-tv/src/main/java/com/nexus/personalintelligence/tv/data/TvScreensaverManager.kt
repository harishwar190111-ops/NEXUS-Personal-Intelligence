package com.nexus.personalintelligence.tv.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TvScreensaverManager(
    var timeoutMs: Long = 300_000L // 5 minutes default
) {
    private var lastActivityTimestamp: Long = System.currentTimeMillis()

    private val _isScreensaverActive = MutableStateFlow(false)
    val isScreensaverActive: StateFlow<Boolean> = _isScreensaverActive.asStateFlow()

    private val _isQuickSettingsOpen = MutableStateFlow(false)
    val isQuickSettingsOpen: StateFlow<Boolean> = _isQuickSettingsOpen.asStateFlow()

    fun recordActivity(timestamp: Long = System.currentTimeMillis()) {
        lastActivityTimestamp = timestamp
        if (_isScreensaverActive.value) {
            _isScreensaverActive.value = false
        }
    }

    fun checkIdle(currentTimestamp: Long = System.currentTimeMillis()): Boolean {
        if (timeoutMs <= 0L) return false // Screensaver disabled
        if (!_isScreensaverActive.value && (currentTimestamp - lastActivityTimestamp >= timeoutMs)) {
            _isScreensaverActive.value = true
            return true
        }
        return _isScreensaverActive.value
    }

    fun startScreensaver() {
        _isScreensaverActive.value = true
    }

    fun dismissScreensaver() {
        _isScreensaverActive.value = false
        lastActivityTimestamp = System.currentTimeMillis()
    }

    fun openQuickSettings() {
        _isQuickSettingsOpen.value = true
        recordActivity()
    }

    fun closeQuickSettings() {
        _isQuickSettingsOpen.value = false
        recordActivity()
    }

    val lastActivity: Long
        get() = lastActivityTimestamp
}
