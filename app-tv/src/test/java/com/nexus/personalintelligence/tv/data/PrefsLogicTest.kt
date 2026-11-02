package com.nexus.personalintelligence.tv.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PrefsLogicTest {
    @Test
    fun decode_handlesNullBlankAndDuplicates() {
        assertEquals(emptyList<String>(), PrefsLogic.decode(null))
        assertEquals(emptyList<String>(), PrefsLogic.decode(""))
        assertEquals(listOf("a.b", "c.d"), PrefsLogic.decode(" a.b, ,c.d,a.b"))
    }

    @Test
    fun encodeDecode_roundTrips() {
        val list = listOf("com.one", "com.two", "com.three")
        assertEquals(list, PrefsLogic.decode(PrefsLogic.encode(list)))
    }

    @Test
    fun toggled_addsThenRemoves() {
        val added = PrefsLogic.toggled(listOf("a"), "b")
        assertEquals(listOf("a", "b"), added)
        assertEquals(listOf("a"), PrefsLogic.toggled(added, "b"))
    }

    @Test
    fun withRecent_movesToFrontWithoutDuplicates() {
        assertEquals(listOf("c", "a", "b"), PrefsLogic.withRecent(listOf("a", "b", "c"), "c"))
    }

    @Test
    fun withRecent_capsLength() {
        val start = (1..12).map { "app$it" }
        val result = PrefsLogic.withRecent(start, "new")
        assertEquals(PrefsLogic.MAX_RECENTS, result.size)
        assertEquals("new", result.first())
        assertEquals("app11", result.last())
    }
}
