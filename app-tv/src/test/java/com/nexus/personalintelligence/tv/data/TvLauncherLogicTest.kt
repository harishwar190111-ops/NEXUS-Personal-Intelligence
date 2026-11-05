package com.nexus.personalintelligence.tv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TvLauncherLogicTest {

    @Test
    fun testHomeIntentRecognition() {
        val action = "android.intent.action.MAIN"
        val homeCats = setOf("android.intent.category.HOME", "android.intent.category.DEFAULT")
        assertTrue(TvLauncherLogic.isHomeIntent(action, homeCats))

        // Different action
        assertFalse(TvLauncherLogic.isHomeIntent("android.intent.action.VIEW", homeCats))

        // No HOME category
        val normalCats = setOf("android.intent.category.LAUNCHER")
        assertFalse(TvLauncherLogic.isHomeIntent(action, normalCats))

        // Null checks
        assertFalse(TvLauncherLogic.isHomeIntent(null, homeCats))
        assertFalse(TvLauncherLogic.isHomeIntent(action, null))
    }

    @Test
    fun testLeanbackIntentRecognition() {
        val action = "android.intent.action.MAIN"
        val leanbackCats = setOf("android.intent.category.LEANBACK_LAUNCHER")
        assertTrue(TvLauncherLogic.isLeanbackIntent(action, leanbackCats))

        val nonLeanbackCats = setOf("android.intent.category.HOME")
        assertFalse(TvLauncherLogic.isLeanbackIntent(action, nonLeanbackCats))
    }

    @Test
    fun testShouldSuppressBackOnHome() {
        // When launcher mode is active and on home screen with no entry open -> suppress back
        assertTrue(TvLauncherLogic.shouldSuppressBackOnHome(
            isHomeScreen = true,
            isEntryOpen = false,
            startedAsHome = false,
            launcherModeEnabled = true
        ))

        // When started as home -> suppress back
        assertTrue(TvLauncherLogic.shouldSuppressBackOnHome(
            isHomeScreen = true,
            isEntryOpen = false,
            startedAsHome = true,
            launcherModeEnabled = false
        ))

        // When entry/dialog is open -> do NOT suppress back (back should close entry)
        assertFalse(TvLauncherLogic.shouldSuppressBackOnHome(
            isHomeScreen = true,
            isEntryOpen = true,
            startedAsHome = true,
            launcherModeEnabled = true
        ))

        // When on a different screen (e.g. Settings) -> do NOT suppress back
        assertFalse(TvLauncherLogic.shouldSuppressBackOnHome(
            isHomeScreen = false,
            isEntryOpen = false,
            startedAsHome = true,
            launcherModeEnabled = true
        ))
    }

    @Test
    fun testStartupConfigModes() {
        // Default config
        val defaultCfg = TvStartupConfig()
        assertTrue(defaultCfg.shouldPlayAnimation)
        assertEquals(1.0f, defaultCfg.animationDurationScale, 0.001f)

        // Fast startup mode skips animation
        val fastCfg = TvStartupConfig(fastStartupMode = true)
        assertFalse(fastCfg.shouldPlayAnimation)
        assertEquals(0.0f, fastCfg.animationDurationScale, 0.001f)

        // Reduced motion mode scales animation down
        val motionCfg = TvStartupConfig(reducedMotionMode = true)
        assertFalse(motionCfg.shouldPlayAnimation)
        assertEquals(0.2f, motionCfg.animationDurationScale, 0.001f)

        // Skip animation
        val skipCfg = TvStartupConfig(skipAnimation = true)
        assertFalse(skipCfg.shouldPlayAnimation)
        assertEquals(0.0f, skipCfg.animationDurationScale, 0.001f)
    }
}
