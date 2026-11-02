package com.nexus.personalintelligence.tv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed as rowItemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.personalintelligence.core.common.ui.LocalNexusStyle
import com.nexus.personalintelligence.core.common.ui.NexusColors
import com.nexus.personalintelligence.core.common.ui.nexusTitleStyle
import com.nexus.personalintelligence.tv.data.AppEntry
import com.nexus.personalintelligence.tv.data.AppIcon
import com.nexus.personalintelligence.tv.data.CategoryLogic
import com.nexus.personalintelligence.tv.data.SearchLogic
import kotlinx.coroutines.delay

/** Every launchable app, filterable by category. Hold OK (or use Edit favourites) to star an app. */
@Composable
fun AllAppsScreen(
    apps: List<AppEntry>,
    favourites: List<String>,
    loadIcon: suspend (AppEntry) -> AppIcon?,
    onLaunch: (AppEntry) -> Unit,
    onToggleFavourite: (AppEntry) -> Unit,
) {
    val st = LocalNexusStyle.current
    var editMode by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf("All") }
    val categories = remember(apps) { listOf("All") + CategoryLogic.present(apps) }
    val shown = remember(apps, category) { if (category == "All") apps else apps.filter { it.category == category } }
    val favouriteSet = remember(favourites) { favourites.toSet() }
    val tileWidth = rememberTileWidth()
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(150)
        runCatching { firstFocus.requestFocus() }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 27.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("All apps", style = nexusTitleStyle(30.sp))
                Text(
                    if (editMode) "Edit mode: press OK on an app to add or remove it from Favourites."
                    else "Press OK to open. Hold OK to add or remove a favourite. ${shown.size} apps.",
                    fontSize = 14.sp,
                    color = NexusColors.TextSecondary,
                )
            }
            TvButton(if (editMode) "Done" else "Edit favourites", onClick = { editMode = !editMode }, selected = editMode)
        }
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            rowItemsIndexed(categories, key = { _, c -> c }) { _, c ->
                TvButton(c, onClick = { category = c }, selected = c == category, horizontalPadding = 18.dp)
            }
        }
        if (shown.isEmpty()) {
            Text("No apps in this category.", Modifier.padding(top = 20.dp), color = NexusColors.TextMuted, fontSize = 16.sp)
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = tileWidth),
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(st.tileGapDp.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(shown, key = { _, e -> e.packageName }) { index, entry ->
                    AppTile(
                        entry = entry,
                        isFavourite = entry.packageName in favouriteSet,
                        loadIcon = loadIcon,
                        onClick = { if (editMode) onToggleFavourite(entry) else onLaunch(entry) },
                        onLongClick = { onToggleFavourite(entry) },
                        modifier = (if (index == 0) Modifier.focusRequester(firstFocus) else Modifier).fillMaxWidth(),
                    )
                }
            }
        }
    }
}

/** Live search over installed apps using the on-screen keyboard. */
@Composable
fun SearchScreen(
    apps: List<AppEntry>,
    favourites: List<String>,
    loadIcon: suspend (AppEntry) -> AppIcon?,
    onLaunch: (AppEntry) -> Unit,
) {
    val st = LocalNexusStyle.current
    var query by remember { mutableStateOf("") }
    val results = remember(apps, query) { SearchLogic.filter(apps, query) }
    val favouriteSet = remember(favourites) { favourites.toSet() }
    val tileWidth = rememberTileWidth()
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(150)
        runCatching { firstFocus.requestFocus() }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 27.dp)) {
        Text("Search apps", style = nexusTitleStyle(30.sp))
        Spacer(Modifier.height(6.dp))
        Text(
            if (query.isEmpty()) "Type with the keyboard below." else "\"$query\" - ${results.size} found",
            fontSize = 18.sp,
            color = if (query.isEmpty()) NexusColors.TextMuted else NexusColors.TextPrimary,
        )
        NoResultHint(results.isEmpty() && query.isNotEmpty())
        if (results.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(st.tileGapDp.dp),
            ) {
                rowItemsIndexed(results, key = { _, e -> e.packageName }) { _, entry ->
                    AppTile(
                        entry = entry,
                        isFavourite = entry.packageName in favouriteSet,
                        loadIcon = loadIcon,
                        onClick = { onLaunch(entry) },
                        onLongClick = {},
                        modifier = Modifier.width(tileWidth),
                    )
                }
            }
        } else {
            Spacer(Modifier.height(if (query.isEmpty()) 16.dp else 8.dp))
        }
        Spacer(Modifier.weight(1f))
        OnScreenKeyboard(
            onKey = { query += it },
            onBackspace = { query = query.dropLast(1) },
            onSpace = { if (query.isNotEmpty() && !query.endsWith(" ")) query += " " },
            onClear = { query = "" },
            modifier = Modifier.fillMaxWidth(),
            firstFocus = firstFocus,
        )
    }
}

/** Shows a hint when a search has no result. */
@Composable
private fun NoResultHint(noResult: Boolean) {
    if (noResult) {
        Text("No installed app matches that name.", Modifier.padding(top = 14.dp), fontSize = 15.sp, color = NexusColors.TextMuted)
    }
}
