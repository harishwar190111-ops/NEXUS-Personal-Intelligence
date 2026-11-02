package com.nexus.personalintelligence.tv.ui

enum class TvScreen { Home, AllApps, Search, WebApps, Themes, Backgrounds, Settings }

/** A pending on-screen-keyboard request (web address, site name, or rename). */
sealed interface EntryRequest {
    data object NewUrl : EntryRequest
    data class NewName(val url: String) : EntryRequest
    data class Rename(val url: String, val current: String) : EntryRequest
}
