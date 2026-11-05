package com.nexus.personalintelligence.core.common.profile

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class ProfileSettings(
    val themePresetId: String = "obsidian",
    val favouriteApps: List<String> = emptyList(),
    val wallpaperPath: String = "",
    val homeLayout: String = "ROWS",
    val assistantVoiceResponse: Boolean = true,
    val assistantLanguage: String = "ta-IN",
    val clock24Hour: Boolean = true
)

@Serializable
data class UserProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val isDefault: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val settings: ProfileSettings = ProfileSettings()
)

class UserProfileStore(
    val maxProfiles: Int = 6,
    defaultProfileName: String = "Default"
) {
    private val profiles = mutableListOf<UserProfile>()
    private var activeProfileId: String

    init {
        val defaultProfile = UserProfile(
            id = "default_profile",
            name = defaultProfileName,
            isDefault = true
        )
        profiles.add(defaultProfile)
        activeProfileId = defaultProfile.id
    }

    @Synchronized
    fun getActiveProfile(): UserProfile {
        return profiles.find { it.id == activeProfileId } ?: profiles.first()
    }

    @Synchronized
    fun getProfiles(): List<UserProfile> = profiles.toList()

    @Synchronized
    fun addProfile(name: String, settings: ProfileSettings = ProfileSettings()): UserProfile? {
        if (profiles.size >= maxProfiles) {
            return null
        }
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return null

        val newProfile = UserProfile(
            name = trimmed,
            isDefault = false,
            settings = settings
        )
        profiles.add(newProfile)
        return newProfile
    }

    @Synchronized
    fun switchProfile(profileId: String): Boolean {
        val target = profiles.find { it.id == profileId } ?: return false
        activeProfileId = target.id
        return true
    }

    @Synchronized
    fun updateActiveProfileSettings(updater: (ProfileSettings) -> ProfileSettings): Boolean {
        val index = profiles.indexOfFirst { it.id == activeProfileId }
        if (index == -1) return false
        val current = profiles[index]
        profiles[index] = current.copy(settings = updater(current.settings))
        return true
    }

    @Synchronized
    fun deleteProfile(profileId: String): Boolean {
        val target = profiles.find { it.id == profileId } ?: return false
        if (target.isDefault) {
            // Default profile cannot be deleted
            return false
        }
        profiles.remove(target)
        if (activeProfileId == profileId) {
            // Revert active to default profile if active was deleted
            activeProfileId = profiles.first().id
        }
        return true
    }

    @Synchronized
    fun getProfileCount(): Int = profiles.size
}
