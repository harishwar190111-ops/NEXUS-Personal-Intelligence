package com.nexus.personalintelligence.core.remote

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class RemoteCommandType {
    DPAD_UP, DPAD_DOWN, DPAD_LEFT, DPAD_RIGHT, DPAD_CENTER,
    BACK, HOME, MENU, SETTINGS, POWER,
    PLAY_PAUSE, STOP, PREVIOUS, NEXT, REWIND, FAST_FORWARD,
    VOLUME_UP, VOLUME_DOWN, MUTE,
    POINTER_MOVE, POINTER_TAP, POINTER_DOUBLE_TAP, POINTER_LONG_PRESS, POINTER_SCROLL,
    KEY_INPUT, TEXT_INPUT, BACKSPACE, ENTER, SPACE, TAB, ESCAPE,
    LAUNCH_APP, OPEN_URL
}

@Serializable
data class RemoteCommand(
    val commandId: String = UUID.randomUUID().toString(),
    val type: RemoteCommandType,
    val text: String? = null,
    val deltaX: Float = 0f,
    val deltaY: Float = 0f,
    val packageName: String? = null,
    val targetUrl: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class RemoteLayoutConfig(
    val isOneHanded: Boolean = false,
    val isLeftHanded: Boolean = false,
    val touchSensitivity: Float = 1.0f,
    val hapticFeedbackEnabled: Boolean = true,
    val pointerSpeed: Float = 1.0f
)

@Serializable
data class AppCapability(
    val packageName: String,
    val appName: String,
    val supportsDirectIntent: Boolean,
    val supportsMediaKeys: Boolean,
    val requiresAccessibilityForFullNavigation: Boolean,
    val limitationNote: String? = null
)

class ThirdPartyAppCapabilityRegistry {
    private val capabilities = mutableMapOf<String, AppCapability>()

    init {
        registerDefaultMatrix()
    }

    private fun registerDefaultMatrix() {
        capabilities["com.netflix.ninja"] = AppCapability(
            packageName = "com.netflix.ninja",
            appName = "Netflix",
            supportsDirectIntent = true,
            supportsMediaKeys = true,
            requiresAccessibilityForFullNavigation = true,
            limitationNote = "Third-party DRM prevents arbitrary synthetic mouse gestures; standard TV D-pad and media key injection supported via Android accessibility or Leanback key events."
        )
        capabilities["com.google.android.youtube.tv"] = AppCapability(
            packageName = "com.google.android.youtube.tv",
            appName = "YouTube TV",
            supportsDirectIntent = true,
            supportsMediaKeys = true,
            requiresAccessibilityForFullNavigation = false,
            limitationNote = "Direct deep linking and standard D-pad/media events supported."
        )
    }

    fun getCapability(packageName: String): AppCapability {
        return capabilities[packageName] ?: AppCapability(
            packageName = packageName,
            appName = packageName,
            supportsDirectIntent = true,
            supportsMediaKeys = true,
            requiresAccessibilityForFullNavigation = true,
            limitationNote = "Standard TV D-pad/media key injection supported. Pointer gestures require TV accessibility service."
        )
    }
}
