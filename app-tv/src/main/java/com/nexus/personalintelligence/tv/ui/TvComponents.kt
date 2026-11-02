package com.nexus.personalintelligence.tv.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.nexus.personalintelligence.core.common.ui.FocusStyle
import com.nexus.personalintelligence.core.common.ui.LocalNexusAccent
import com.nexus.personalintelligence.core.common.ui.LocalNexusStyle
import com.nexus.personalintelligence.core.common.ui.NexusColors
import com.nexus.personalintelligence.core.common.ui.nexusTitleStyle
import com.nexus.personalintelligence.core.common.util.NexusTime
import com.nexus.personalintelligence.tv.data.AppEntry
import com.nexus.personalintelligence.tv.data.AppIcon
import java.time.LocalDateTime
import kotlinx.coroutines.delay

/** Base tile width scales with the screen so 720p and 1080p outputs both show about six tiles. */
@Composable
fun rememberTileWidth(): Dp {
    val widthDp = LocalConfiguration.current.screenWidthDp
    val base = ((widthDp - 96) / 6.4f).dp.coerceIn(128.dp, 200.dp)
    return base * LocalNexusStyle.current.tileWidthScale
}

/** Current local date/time, refreshed at the start of every minute. */
@Composable
fun rememberNow(): State<LocalDateTime> = produceState(initialValue = LocalDateTime.now()) {
    while (true) {
        value = LocalDateTime.now()
        delay(60_000L - (System.currentTimeMillis() % 60_000L) + 20L)
    }
}

@Composable
fun ClockBlock(now: LocalDateTime, use24Hour: Boolean, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.End) {
        Text(NexusTime.formatTime(now.toLocalTime(), use24Hour), style = nexusTitleStyle(40.sp))
        Text(NexusTime.formatDate(now.toLocalDate()), fontSize = 16.sp, color = NexusColors.TextSecondary)
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier = modifier,
        style = nexusTitleStyle(14.sp),
        letterSpacing = 2.sp,
        color = NexusColors.TextSecondary,
    )
}

/** Standard screen layout: title, optional subtitle, safe margins (48dp x 27dp), optional vertical scroll. */
@Composable
fun ScreenFrame(
    title: String,
    subtitle: String? = null,
    scroll: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
            .padding(horizontal = 48.dp, vertical = 27.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(title, style = nexusTitleStyle(30.sp))
        if (subtitle != null) Text(subtitle, fontSize = 14.sp, color = NexusColors.TextSecondary)
        content()
    }
}

/** Focusable pill button with a clearly visible accent focus ring. OK/Enter activates it. */
@Composable
fun TvButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    swatch: Color? = null,
    horizontalPadding: Dp = 24.dp,
) {
    val accent = LocalNexusAccent.current.color
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.06f else 1f, tween(120), label = "buttonScale")
    val shape = RoundedCornerShape(50)
    val fill = when {
        focused -> accent.copy(alpha = 0.28f)
        selected -> accent.copy(alpha = 0.14f)
        else -> Color.White.copy(alpha = 0.07f)
    }
    val ring = when {
        focused -> accent
        selected -> accent.copy(alpha = 0.6f)
        else -> NexusColors.GlassBorder
    }
    Row(
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(fill)
            .border(if (focused) 3.dp else 1.dp, ring, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = horizontalPadding, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (swatch != null) {
            Box(Modifier.size(14.dp).clip(CircleShape).background(swatch))
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text,
            fontSize = 16.sp,
            fontWeight = if (focused || selected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (focused) Color.White else NexusColors.TextPrimary,
        )
    }
}

/** Round colour swatch, focusable. */
@Composable
fun SwatchButton(color: Color, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.18f else 1f, tween(120), label = "swatchScale")
    Box(
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .size(38.dp)
            .clip(CircleShape)
            .background(color)
            .border(if (focused) 4.dp else if (selected) 3.dp else 1.dp, if (focused || selected) Color.White else NexusColors.GlassBorder, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
    )
}

/** "-  value  +" control used by the studios. */
@Composable
fun TvStepper(
    label: String,
    valueText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
    firstFocus: FocusRequester? = null,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), fontSize = 17.sp, color = NexusColors.TextPrimary)
        TvButton(
            "-",
            onMinus,
            modifier = if (firstFocus != null) Modifier.focusRequester(firstFocus) else Modifier,
            horizontalPadding = 22.dp,
        )
        Text(valueText, Modifier.width(110.dp), fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = NexusColors.TextPrimary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        TvButton("+", onPlus, horizontalPadding = 22.dp)
    }
}

/** Small key for the on-screen keyboard. */
@Composable
private fun KeyButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, wide: Boolean = false) {
    val accent = LocalNexusAccent.current.color
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            .width(if (wide) 96.dp else 44.dp)
            .height(40.dp)
            .clip(shape)
            .background(if (focused) accent.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.08f))
            .border(if (focused) 3.dp else 1.dp, if (focused) accent else NexusColors.GlassBorder, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = if (focused) Color.White else NexusColors.TextPrimary, maxLines = 1)
    }
}

/** D-pad friendly on-screen keyboard (a TV has no keyboard). */
@Composable
fun OnScreenKeyboard(
    onKey: (String) -> Unit,
    onBackspace: () -> Unit,
    onSpace: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    firstFocus: FocusRequester? = null,
    extraKeys: List<String> = emptyList(),
) {
    val rows = listOf("1234567890", "qwertyuiop", "asdfghjkl", "zxcvbnm")
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        rows.forEachIndexed { r, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEachIndexed { c, ch ->
                    KeyButton(
                        ch.toString(),
                        onClick = { onKey(ch.toString()) },
                        modifier = if (r == 0 && c == 0 && firstFocus != null) Modifier.focusRequester(firstFocus) else Modifier,
                    )
                }
            }
        }
        if (extraKeys.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                extraKeys.forEach { k -> KeyButton(k, onClick = { onKey(k) }, wide = k.length > 1) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            KeyButton("Space", onClick = onSpace, wide = true)
            KeyButton("Delete", onClick = onBackspace, wide = true)
            KeyButton("Clear", onClick = onClear, wide = true)
        }
    }
}

/**
 * App tile. OK/Enter = [onClick]. Holding OK/Enter (key repeat) = [onLongClick].
 * Long-press is handled here because Compose's clickable does not fire it for D-pad keys.
 * Focus look (ring / glow / lift / underline), corner radius, aspect ratio and label type follow the active preset.
 */
@Composable
fun AppTile(
    entry: AppEntry,
    isFavourite: Boolean,
    loadIcon: suspend (AppEntry) -> AppIcon?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = LocalNexusAccent.current.color
    val st = LocalNexusStyle.current
    var focused by remember { mutableStateOf(false) }
    var longHandled by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) st.focusScale else 1f, tween(140), label = "tileScale")
    val lift by animateFloatAsState(if (focused && st.focusStyle == FocusStyle.Lift) -10f else 0f, tween(140), label = "tileLift")
    val icon by produceState<AppIcon?>(null, entry.packageName) { value = loadIcon(entry) }
    val shape = RoundedCornerShape(st.cornerDp.dp)

    val ringColor = when (st.focusStyle) {
        FocusStyle.Ring -> accent
        FocusStyle.Glow -> accent.copy(alpha = 0.75f)
        FocusStyle.Lift -> Color.White.copy(alpha = 0.85f)
        FocusStyle.Underline -> Color.White.copy(alpha = st.borderAlpha + 0.1f)
    }
    val ringWidth = if (!focused) 1.dp else when (st.focusStyle) {
        FocusStyle.Ring -> 3.dp
        FocusStyle.Glow -> 2.dp
        FocusStyle.Lift -> 2.dp
        FocusStyle.Underline -> 1.dp
    }

    Column(
        modifier = modifier
            .zIndex(if (focused) 1f else 0f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = lift * density
            }
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { ev ->
                val isSelect = ev.key == Key.DirectionCenter || ev.key == Key.Enter || ev.key == Key.NumPadEnter
                if (!isSelect) {
                    false
                } else {
                    when (ev.type) {
                        KeyEventType.KeyDown -> {
                            if (ev.nativeKeyEvent.repeatCount == 1) {
                                longHandled = true
                                onLongClick()
                            }
                            true
                        }
                        KeyEventType.KeyUp -> {
                            if (longHandled) longHandled = false else onClick()
                            true
                        }
                        else -> false
                    }
                }
            }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(st.tileAspect)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = st.glassAlpha + 0.02f), Color.White.copy(alpha = st.glassAlpha * 0.5f))))
                .background(if (focused && st.focusStyle == FocusStyle.Glow) accent.copy(alpha = 0.20f) else Color.Transparent)
                .border(ringWidth, if (focused) ringColor else Color.White.copy(alpha = st.borderAlpha), shape),
            contentAlignment = Alignment.Center,
        ) {
            val ic = icon
            when {
                ic == null -> Text(
                    entry.label.take(1).uppercase(),
                    style = nexusTitleStyle(28.sp),
                    color = NexusColors.TextMuted,
                )
                ic.isBanner -> Image(
                    bitmap = ic.bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                else -> Image(
                    bitmap = ic.bitmap,
                    contentDescription = null,
                    modifier = Modifier.fillMaxHeight(0.62f).aspectRatio(1f),
                )
            }
            if (isFavourite) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = "Favourite",
                    tint = accent,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(18.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            if (st.labelCaps) entry.label.uppercase() else entry.label,
            style = nexusTitleStyle(14.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (focused) Color.White else NexusColors.TextSecondary,
        )
        Box(
            Modifier
                .padding(top = 4.dp)
                .fillMaxWidth(if (focused && st.focusStyle == FocusStyle.Underline) 0.8f else 0f)
                .height(3.dp)
                .background(accent),
        )
    }
}
