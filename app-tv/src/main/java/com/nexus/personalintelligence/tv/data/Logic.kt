package com.nexus.personalintelligence.tv.data

import android.content.pm.ApplicationInfo
import java.net.URI

/** Categories come from the real ApplicationInfo.category the app declares (Android 8+), plus System vs user apps. */
object CategoryLogic {
    val ORDER = listOf("Games", "Video", "Music", "Social", "News", "Maps", "Photos", "Productivity", "Apps", "System")

    fun labelFor(category: Int, isSystem: Boolean): String = when (category) {
        ApplicationInfo.CATEGORY_GAME -> "Games"
        ApplicationInfo.CATEGORY_AUDIO -> "Music"
        ApplicationInfo.CATEGORY_VIDEO -> "Video"
        ApplicationInfo.CATEGORY_IMAGE -> "Photos"
        ApplicationInfo.CATEGORY_SOCIAL -> "Social"
        ApplicationInfo.CATEGORY_NEWS -> "News"
        ApplicationInfo.CATEGORY_MAPS -> "Maps"
        ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Productivity"
        else -> if (isSystem) "System" else "Apps"
    }

    /** Only categories that actually occur, in a stable display order. */
    fun present(apps: List<AppEntry>): List<String> {
        val have = apps.map { it.category }.toSet()
        return ORDER.filter { it in have }
    }
}

object SearchLogic {
    /** Case-insensitive match on the app name; names that start with the query come first. */
    fun filter(apps: List<AppEntry>, query: String): List<AppEntry> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        val hits = apps.filter { it.label.lowercase().contains(q) }
        return hits.sortedBy { if (it.label.lowercase().startsWith(q)) 0 else 1 }
    }
}

data class WebShortcut(val name: String, val url: String)

object WebLogic {
    /** Returns a clean http(s) URL or null. Rejects other schemes, whitespace and hosts without a dot. */
    fun normalizeUrl(input: String): String? {
        val t = input.trim()
        if (t.isEmpty() || t.any { it.isWhitespace() }) return null
        val withScheme = if (t.contains("://")) t else "https://$t"
        return try {
            val uri = URI(withScheme)
            val scheme = uri.scheme?.lowercase()
            val host = uri.host
            if ((scheme == "http" || scheme == "https") && !host.isNullOrBlank() && (host.contains('.') || host == "localhost")) {
                withScheme
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    fun defaultName(url: String): String = try {
        URI(url).host?.removePrefix("www.") ?: url
    } catch (e: Exception) {
        url
    }

    fun cleanName(raw: String): String = raw.replace('\t', ' ').replace('\n', ' ').trim().take(40)

    fun encode(list: List<WebShortcut>): String = list.joinToString("\n") { "${cleanName(it.name)}\t${it.url}" }

    fun decode(raw: String?): List<WebShortcut> = raw.orEmpty().lines().mapNotNull { line ->
        val parts = line.split('\t')
        if (parts.size == 2 && parts[1].isNotBlank()) {
            WebShortcut(parts[0].ifBlank { defaultName(parts[1]) }, parts[1])
        } else {
            null
        }
    }
}
