package com.nexus.personalintelligence.core.common.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UserProfileTest {

    @Test
    fun testDefaultProfileCannotBeDeleted() {
        val store = UserProfileStore(defaultProfileName = "Main TV")
        val active = store.getActiveProfile()
        assertTrue(active.isDefault)
        assertEquals("Main TV", active.name)
        assertEquals("default_profile", active.id)

        // Attempt deletion of default profile must fail
        val deleted = store.deleteProfile("default_profile")
        assertFalse("Default profile must not be deletable", deleted)
        assertEquals(1, store.getProfileCount())
        assertEquals("default_profile", store.getActiveProfile().id)
    }

    @Test
    fun testAddAndSwitchProfilesWithSettingsIsolation() {
        val store = UserProfileStore()

        // Default profile has obsidian theme
        assertEquals("obsidian", store.getActiveProfile().settings.themePresetId)

        // Add a second profile with custom settings
        val kidProfile = store.addProfile(
            name = "Kids",
            settings = ProfileSettings(
                themePresetId = "neon_matrix",
                favouriteApps = listOf("com.example.cartoons"),
                assistantVoiceResponse = false
            )
        )
        assertNotNull(kidProfile)
        assertEquals(2, store.getProfileCount())

        // Active profile is still default
        assertEquals("default_profile", store.getActiveProfile().id)

        // Switch to kids profile
        val switched = store.switchProfile(kidProfile!!.id)
        assertTrue(switched)
        assertEquals(kidProfile.id, store.getActiveProfile().id)
        assertEquals("neon_matrix", store.getActiveProfile().settings.themePresetId)
        assertEquals(listOf("com.example.cartoons"), store.getActiveProfile().settings.favouriteApps)

        // Modify kids profile settings
        store.updateActiveProfileSettings { current ->
            current.copy(homeLayout = "COMPACT")
        }
        assertEquals("COMPACT", store.getActiveProfile().settings.homeLayout)

        // Switch back to default profile and verify settings isolation
        store.switchProfile("default_profile")
        assertEquals("default_profile", store.getActiveProfile().id)
        assertEquals("obsidian", store.getActiveProfile().settings.themePresetId)
        assertEquals("ROWS", store.getActiveProfile().settings.homeLayout)
        assertTrue(store.getActiveProfile().settings.favouriteApps.isEmpty())
    }

    @Test
    fun testBoundedProfileCountEnforced() {
        val maxLimit = 3
        val store = UserProfileStore(maxProfiles = maxLimit)

        assertEquals(1, store.getProfileCount()) // Default profile

        val p1 = store.addProfile("User 1")
        val p2 = store.addProfile("User 2")
        assertNotNull(p1)
        assertNotNull(p2)
        assertEquals(3, store.getProfileCount())

        // Adding 4th profile should exceed bound and return null
        val p3 = store.addProfile("User 3")
        assertNull("Exceeding maximum profile limit must be rejected", p3)
        assertEquals(3, store.getProfileCount())
    }

    @Test
    fun testDeleteNonDefaultProfileAndFallbackToDefault() {
        val store = UserProfileStore()
        val guest = store.addProfile("Guest")
        assertNotNull(guest)

        store.switchProfile(guest!!.id)
        assertEquals(guest.id, store.getActiveProfile().id)

        // Delete active guest profile
        val deleted = store.deleteProfile(guest.id)
        assertTrue(deleted)
        assertEquals(1, store.getProfileCount())

        // Active profile should have safely reverted to default profile
        assertEquals("default_profile", store.getActiveProfile().id)
        assertTrue(store.getActiveProfile().isDefault)
    }

    @Test
    fun testRejectBlankProfileName() {
        val store = UserProfileStore()
        val blank1 = store.addProfile("")
        val blank2 = store.addProfile("   ")
        assertNull(blank1)
        assertNull(blank2)
        assertEquals(1, store.getProfileCount())
    }
}
