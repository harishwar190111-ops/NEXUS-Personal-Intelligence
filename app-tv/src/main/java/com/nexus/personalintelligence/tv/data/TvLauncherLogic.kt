package com.nexus.personalintelligence.tv.data

data class TvStartupConfig(
    val skipAnimation: Boolean = false,
    val reducedMotionMode: Boolean = false,
    val fastStartupMode: Boolean = false,
    val playStartupSound: Boolean = true,
    val soundVolume: Float = 0.8f
) {
    val shouldPlayAnimation: Boolean
        get() = !skipAnimation && !fastStartupMode && !reducedMotionMode

    val animationDurationScale: Float
        get() = when {
            fastStartupMode || skipAnimation -> 0.0f
            reducedMotionMode -> 0.2f
            else -> 1.0f
        }
}

object TvLauncherLogic {
    const val ACTION_MAIN = "android.intent.action.MAIN"
    const val CATEGORY_HOME = "android.intent.category.HOME"
    const val CATEGORY_DEFAULT = "android.intent.category.DEFAULT"
    const val CATEGORY_LEANBACK_LAUNCHER = "android.intent.category.LEANBACK_LAUNCHER"

    /** Validates whether an incoming intent matches the Android HOME launcher intent. */
    fun isHomeIntent(action: String?, categories: Set<String>?): Boolean {
        if (action != ACTION_MAIN) return false
        if (categories == null) return false
        return categories.contains(CATEGORY_HOME)
    }

    /** Validates whether an intent matches Android TV Leanback launcher intent. */
    fun isLeanbackIntent(action: String?, categories: Set<String>?): Boolean {
        if (action != ACTION_MAIN) return false
        if (categories == null) return false
        return categories.contains(CATEGORY_LEANBACK_LAUNCHER)
    }

    /** Returns whether back button press should be suppressed on the home screen. */
    fun shouldSuppressBackOnHome(
        isHomeScreen: Boolean,
        isEntryOpen: Boolean,
        startedAsHome: Boolean,
        launcherModeEnabled: Boolean
    ): Boolean {
        if (isEntryOpen) return false
        if (!isHomeScreen) return false
        return startedAsHome || launcherModeEnabled
    }
}
