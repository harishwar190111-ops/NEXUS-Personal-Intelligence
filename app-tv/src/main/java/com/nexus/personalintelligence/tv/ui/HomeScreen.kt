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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.personalintelligence.core.common.ui.HomeLayout
import com.nexus.personalintelligence.core.common.ui.LocalNexusStyle
import com.nexus.personalintelligence.core.common.ui.NavPlacement
import com.nexus.personalintelligence.core.common.ui.NexusColors
import com.nexus.personalintelligence.core.common.ui.NexusWordmark
import com.nexus.personalintelligence.core.common.ui.StatusChip
import com.nexus.personalintelligence.core.common.ui.StatusKind
import com.nexus.personalintelligence.core.common.ui.nexusTitleStyle
import com.nexus.personalintelligence.core.common.util.NexusTime
import com.nexus.personalintelligence.tv.data.AppEntry
import com.nexus.personalintelligence.tv.data.AppIcon
import com.nexus.personalintelligence.tv.data.TvPrefs
import kotlinx.coroutines.delay

/**
 * Home dashboard. Its composition depends on the active preset:
 *  - Rows: favourites + recents
 *  - Spotlight: large favourite tiles, smaller recents
 *  - Compact: smaller tiles plus a third row previewing all apps
 * Navigation buttons sit above or below the rows depending on the preset.
 */
@Composable
fun HomeScreen(
    apps: List<AppEntry>,
    prefs: TvPrefs,
    loadIcon: suspend (AppEntry) -> AppIcon?,
    onLaunch: (AppEntry) -> Unit,
    onNavigate: (TvScreen) -> Unit,
) {
    val st = LocalNexusStyle.current
    val byPackage = remember(apps) { apps.associateBy { it.packageName } }
    val favourites = remember(byPackage, prefs.favourites) { prefs.favourites.mapNotNull { byPackage[it] } }
    val recents = remember(byPackage, prefs.recents) { prefs.recents.mapNotNull { byPackage[it] } }
    val preview = remember(apps) { apps.take(16) }
    val favouriteSet = remember(prefs.favourites) { prefs.favourites.toSet() }
    val baseWidth = rememberTileWidth()
    val now by rememberNow()

    val favWidth: Dp = when (st.homeLayout) {
        HomeLayout.Spotlight -> baseWidth * 1.25f
        else -> baseWidth
    }.coerceAtMost(280.dp)
    val recentWidth: Dp = when (st.homeLayout) {
        HomeLayout.Spotlight -> baseWidth * 0.8f
        else -> baseWidth
    }
    val showPreviewRow = st.homeLayout == HomeLayout.Compact

    // First focus: first favourite, else first recent, else the apps preview row, else the nav buttons.
    val initialFocus = remember { FocusRequester() }
    val focusTarget = when {
        favourites.isNotEmpty() -> 0
        recents.isNotEmpty() -> 1
        showPreviewRow && preview.isNotEmpty() -> 2
        else -> 3
    }
    LaunchedEffect(Unit) {
        delay(150)
        runCatching { initialFocus.requestFocus() }
    }

    val nav: @Composable () -> Unit = {
        NavButtons(
            onNavigate = onNavigate,
            firstFocus = if (focusTarget == 3) initialFocus else null,
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 48.dp, vertical = 27.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                NexusWordmark(textSize = 22.sp, markSize = 26.dp)
                Spacer(Modifier.height(10.dp))
                Text(NexusTime.greetingFor(now.hour), style = nexusTitleStyle(30.sp))
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatusChip("Phone link: not paired", StatusKind.Planned, fontSize = 13.sp)
                    StatusChip("${apps.size} apps found", StatusKind.Foundation, fontSize = 13.sp)
                }
            }
            ClockBlock(now, prefs.use24Hour)
        }
        Spacer(Modifier.height(10.dp))

        if (st.navPlacement == NavPlacement.Top) {
            nav()
            Spacer(Modifier.height(6.dp))
        }

        TileRow(
            title = "Favourites",
            items = favourites,
            emptyHint = "Nothing here yet. Open All apps and hold OK on an app to add it.",
            favouriteSet = favouriteSet,
            tileWidth = favWidth,
            loadIcon = loadIcon,
            onLaunch = onLaunch,
            firstFocus = if (focusTarget == 0) initialFocus else null,
        )
        TileRow(
            title = "Recently opened",
            items = recents,
            emptyHint = "Apps you open from NEXUS will appear here.",
            favouriteSet = favouriteSet,
            tileWidth = recentWidth,
            loadIcon = loadIcon,
            onLaunch = onLaunch,
            firstFocus = if (focusTarget == 1) initialFocus else null,
        )
        if (showPreviewRow) {
            TileRow(
                title = "Apps",
                items = preview,
                emptyHint = "No launchable apps were found.",
                favouriteSet = favouriteSet,
                tileWidth = baseWidth,
                loadIcon = loadIcon,
                onLaunch = onLaunch,
                firstFocus = if (focusTarget == 2) initialFocus else null,
            )
        }

        if (st.navPlacement == NavPlacement.Bottom) {
            Spacer(Modifier.height(8.dp))
            nav()
        }
    }
}

@Composable
private fun NavButtons(onNavigate: (TvScreen) -> Unit, firstFocus: FocusRequester?) {
    val items = listOf(
        "All apps" to TvScreen.AllApps,
        "Search" to TvScreen.Search,
        "Web apps" to TvScreen.WebApps,
        "Themes" to TvScreen.Themes,
        "Backgrounds" to TvScreen.Backgrounds,
        "Settings" to TvScreen.Settings,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items.forEachIndexed { index, (label, target) ->
            TvButton(
                label,
                onClick = { onNavigate(target) },
                modifier = if (index == 0 && firstFocus != null) Modifier.focusRequester(firstFocus) else Modifier,
                horizontalPadding = 18.dp,
            )
        }
    }
}

@Composable
private fun TileRow(
    title: String,
    items: List<AppEntry>,
    emptyHint: String,
    favouriteSet: Set<String>,
    tileWidth: Dp,
    loadIcon: suspend (AppEntry) -> AppIcon?,
    onLaunch: (AppEntry) -> Unit,
    firstFocus: FocusRequester?,
) {
    val st = LocalNexusStyle.current
    SectionTitle(title)
    if (items.isEmpty()) {
        Text(emptyHint, modifier = Modifier.padding(vertical = 18.dp), fontSize = 15.sp, color = NexusColors.TextMuted)
    } else {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(st.tileGapDp.dp),
        ) {
            itemsIndexed(items, key = { _, entry -> entry.packageName }) { index, entry ->
                AppTile(
                    entry = entry,
                    isFavourite = entry.packageName in favouriteSet,
                    loadIcon = loadIcon,
                    onClick = { onLaunch(entry) },
                    onLongClick = {},
                    modifier = Modifier
                        .width(tileWidth)
                        .then(if (index == 0 && firstFocus != null) Modifier.focusRequester(firstFocus) else Modifier),
                )
            }
        }
    }
}
