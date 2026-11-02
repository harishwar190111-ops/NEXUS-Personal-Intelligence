package com.nexus.personalintelligence.tv.data

/** Pure list logic for favourites/recents. No Android classes, so it is unit-tested on the JVM. */
object PrefsLogic {
    const val MAX_RECENTS = 12

    fun encode(list: List<String>): String = list.joinToString(",")

    fun decode(raw: String?): List<String> =
        raw.orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }.distinct()

    /** Adds [pkg] to the end if absent, removes it if present. */
    fun toggled(current: List<String>, pkg: String): List<String> =
        if (pkg in current) current.filterNot { it == pkg } else current + pkg

    /** Moves [pkg] to the front, removes duplicates, keeps at most [max] entries. */
    fun withRecent(current: List<String>, pkg: String, max: Int = MAX_RECENTS): List<String> =
        (listOf(pkg) + current.filterNot { it == pkg }).take(max)
}
