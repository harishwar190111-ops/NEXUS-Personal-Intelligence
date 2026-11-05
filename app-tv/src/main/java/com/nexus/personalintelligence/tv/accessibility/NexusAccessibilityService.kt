package com.nexus.personalintelligence.tv.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager

/**
 * Optional accessibility service allowing the NEXUS remote to send global navigation commands
 * (Back, Home, D-pad, etc.) outside the NEXUS launcher to third-party TV apps.
 *
 * Privacy guarantee: NEXUS does not read, log, or collect screen text or passwords.
 * This service is NEVER enabled automatically.
 */
class NexusAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        activeInstance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No screen reading
    }

    override fun onInterrupt() {
        activeInstance = null
    }

    override fun onDestroy() {
        super.onDestroy()
        activeInstance = null
    }

    fun performBack(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)
    fun performHome(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)
    fun performNotifications(): Boolean = performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)

    companion object {
        @Volatile
        var activeInstance: NexusAccessibilityService? = null

        fun isEnabled(context: Context): Boolean {
            if (activeInstance != null) return true
            val am = runCatching { context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager }.getOrNull()
                ?: return false
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabledServices.contains(context.packageName)
        }

        fun createSettingsIntent(): Intent {
            return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }
}
