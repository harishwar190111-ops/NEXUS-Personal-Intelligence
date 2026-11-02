package com.nexus.personalintelligence.tv.data

import android.content.pm.ApplicationInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LogicTest {
    private fun app(label: String, category: String = "Apps") = AppEntry("pkg.$label", label, "Main", category)

    @Test
    fun category_mapsDeclaredCategoryAndSystemFlag() {
        assertEquals("Games", CategoryLogic.labelFor(ApplicationInfo.CATEGORY_GAME, false))
        assertEquals("Video", CategoryLogic.labelFor(ApplicationInfo.CATEGORY_VIDEO, true))
        assertEquals("System", CategoryLogic.labelFor(ApplicationInfo.CATEGORY_UNDEFINED, true))
        assertEquals("Apps", CategoryLogic.labelFor(ApplicationInfo.CATEGORY_UNDEFINED, false))
    }

    @Test
    fun category_presentListsOnlyExistingCategoriesInOrder() {
        val apps = listOf(app("a", "System"), app("b", "Games"), app("c", "Games"))
        assertEquals(listOf("Games", "System"), CategoryLogic.present(apps))
    }

    @Test
    fun search_isCaseInsensitiveAndPrefersPrefixMatches() {
        val apps = listOf(app("My Netflix Fan"), app("Netflix"), app("YouTube"))
        assertEquals(listOf("Netflix", "My Netflix Fan"), SearchLogic.filter(apps, " NETF ").map { it.label })
        assertEquals(emptyList<AppEntry>(), SearchLogic.filter(apps, ""))
        assertEquals(emptyList<AppEntry>(), SearchLogic.filter(apps, "zzz"))
    }

    @Test
    fun url_normalizationAcceptsOnlyHttpWebAddresses() {
        assertEquals("https://example.com", WebLogic.normalizeUrl("example.com"))
        assertEquals("http://a.org/x", WebLogic.normalizeUrl("http://a.org/x"))
        assertEquals("https://localhost", WebLogic.normalizeUrl("localhost"))
        assertNull(WebLogic.normalizeUrl(""))
        assertNull(WebLogic.normalizeUrl("not a url"))
        assertNull(WebLogic.normalizeUrl("ftp://example.com"))
        assertNull(WebLogic.normalizeUrl("javascript:alert(1)"))
        assertNull(WebLogic.normalizeUrl("nodot"))
    }

    @Test
    fun url_defaultNameDropsWww() {
        assertEquals("example.com", WebLogic.defaultName("https://www.example.com/path"))
    }

    @Test
    fun shortcuts_roundTripAndSkipBrokenLines() {
        val list = listOf(WebShortcut("One", "https://one.example"), WebShortcut("Two", "https://two.example"))
        assertEquals(list, WebLogic.decode(WebLogic.encode(list)))
        assertEquals(emptyList<WebShortcut>(), WebLogic.decode("broken line without tab"))
        assertEquals(emptyList<WebShortcut>(), WebLogic.decode(null))
        assertEquals("a b", WebLogic.cleanName("a\tb\n"))
    }
}
