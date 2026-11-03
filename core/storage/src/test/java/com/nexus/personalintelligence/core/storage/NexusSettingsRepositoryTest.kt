package com.nexus.personalintelligence.core.storage

import com.nexus.personalintelligence.core.design.NexusThemePreset
import com.nexus.personalintelligence.core.design.ThemeRegistry
import org.junit.Assert.*
import org.junit.Test

class NexusSettingsRepositoryTest {
    @Test
    fun testSettingsAndBackupRestore() {
        val repo = NexusSettingsRepository()
        val aurora = ThemeRegistry.getTheme(NexusThemePreset.AURORA)
        repo.setTheme(aurora)
        val backup = repo.exportBackup()
        assertTrue(backup.contains("Aurora"))
        val restored = repo.restoreBackup(backup)
        assertTrue(restored)
    }
}
