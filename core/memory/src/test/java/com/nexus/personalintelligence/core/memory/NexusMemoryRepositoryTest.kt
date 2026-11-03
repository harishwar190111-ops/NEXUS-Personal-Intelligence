package com.nexus.personalintelligence.core.memory

import org.junit.Assert.*
import org.junit.Test

class NexusMemoryRepositoryTest {
    @Test
    fun testMemoryLifecycleAndPrivacyFilter() {
        val repo = NexusMemoryRepository()
        assertTrue(repo.addOrUpdate("preferred_theme", "Aurora", MemoryCategory.PREFERRED_THEME))
        assertFalse(repo.addOrUpdate("secret_key", "xyz", MemoryCategory.USER_CUSTOM_NOTE))
        val json = repo.exportToJson()
        repo.clearAll()
        assertEquals(0, repo.memories.value.size)
        repo.importFromJson(json)
        assertEquals(1, repo.memories.value.size)
    }
}
