package com.nexus.personalintelligence.tv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.personalintelligence.core.common.ui.AnimQuality
import com.nexus.personalintelligence.core.common.ui.BackgroundSpec
import com.nexus.personalintelligence.core.common.ui.BackgroundType
import com.nexus.personalintelligence.core.common.ui.CustomTheme
import com.nexus.personalintelligence.core.common.ui.GlassPanel
import com.nexus.personalintelligence.core.common.ui.LocalNexusAccent
import com.nexus.personalintelligence.core.common.ui.NexusAccent
import com.nexus.personalintelligence.core.common.ui.NexusBackdrop
import com.nexus.personalintelligence.core.common.ui.NexusColors
import com.nexus.personalintelligence.core.common.ui.ThemePreset
import com.nexus.personalintelligence.core.common.ui.ThemePresets
import com.nexus.personalintelligence.core.common.ui.nexusTitleStyle
import com.nexus.personalintelligence.tv.data.WebShortcut
import kotlinx.coroutines.delay

/** Full-screen text entry using the on-screen keyboard. Back cancels. */
@Composable
fun TextEntryScreen(
    title: String,
    hint: String,
    initial: String,
    extraKeys: List<String>,
    onBindField: ((TvEditableFieldState) -> Unit)? = null,
    onUnbindField: (() -> Unit)? = null,
    onSubmit: (String) -> Unit,
    onCancel: () -> Unit,
) {
    val fieldState = remember(title, initial) {
        TvEditableFieldState(initialText = initial, onEnter = { onSubmit(initial) })
    }
    DisposableEffect(fieldState) {
        onBindField?.invoke(fieldState)
        onDispose { onUnbindField?.invoke() }
    }
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(150)
        runCatching { firstFocus.requestFocus() }
    }
    ScreenFrame(title, hint, scroll = false) {
        TvInAppTextField(
            state = fieldState,
            label = title,
            allowMaskingChoice = true
        )
        OnScreenKeyboard(
            onKey = { fieldState.insert(it) },
            onBackspace = { fieldState.backspace() },
            onSpace = { fieldState.insert(" ") },
            onClear = { fieldState.clear() },
            modifier = Modifier.fillMaxWidth(),
            firstFocus = firstFocus,
            extraKeys = extraKeys,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally)) {
            TvButton("Done", onClick = { onSubmit(fieldState.text) })
            TvButton("Cancel", onClick = onCancel)
        }
    }
}

@Composable
fun WebAppsScreen(
    sites: List<WebShortcut>,
    browsers: List<String>,
    onOpen: (WebShortcut) -> Unit,
    onAdd: () -> Unit,
    onRename: (WebShortcut) -> Unit,
    onRemove: (WebShortcut) -> Unit,
) {
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(150)
        runCatching { firstFocus.requestFocus() }
    }
    ScreenFrame(
        "Web apps",
        if (browsers.isEmpty()) "No browser app was found on this TV, so websites cannot be opened until one is installed."
        else "Opens in: ${browsers.joinToString(", ")}. NEXUS does not bypass sign-in or DRM.",
    ) {
        TvButton("Add website", onClick = onAdd, modifier = Modifier.focusRequester(firstFocus))
        if (sites.isEmpty()) {
            Text("No websites yet. Add one with the on-screen keyboard.", fontSize = 15.sp, color = NexusColors.TextMuted)
        }
        sites.forEach { site ->
            GlassPanel(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(site.name, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text(site.url, fontSize = 13.sp, color = NexusColors.TextMuted, maxLines = 1)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TvButton("Open", onClick = { onOpen(site) }, horizontalPadding = 18.dp)
                        TvButton("Rename", onClick = { onRename(site) }, horizontalPadding = 18.dp)
                        TvButton("Remove", onClick = { onRemove(site) }, horizontalPadding = 18.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetCard(preset: ThemePreset, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    val accent = preset.accent.color
    val shape = RoundedCornerShape(preset.style.cornerDp.coerceAtMost(28).dp)
    Row(
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            .clip(shape)
            .background(Color.White.copy(alpha = if (focused) 0.16f else (preset.style.glassAlpha + 0.03f)))
            .border(if (focused) 3.dp else if (selected) 2.dp else 1.dp, if (focused) accent else if (selected) accent.copy(alpha = 0.7f) else Color.White.copy(alpha = preset.style.borderAlpha), shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(accent))
        Column(Modifier.padding(start = 14.dp)) {
            Text(
                if (selected) "${preset.label}  (active)" else preset.label,
                style = nexusTitleStyle(18.sp),
                color = if (focused) Color.White else NexusColors.TextPrimary,
            )
            Text(preset.blurb, fontSize = 12.sp, color = NexusColors.TextSecondary, maxLines = 2)
            Text(
                "${preset.style.homeLayout.name} layout - ${preset.style.focusStyle.name} focus - ${preset.background.type.label}",
                fontSize = 11.sp,
                color = NexusColors.TextMuted,
                maxLines = 1,
            )
        }
    }
}

/** Ten presets. Selecting one applies and saves it immediately; the whole TV is the live preview. */
@Composable
fun ThemeStudioScreen(
    presetId: String,
    custom: CustomTheme,
    onPick: (String) -> Unit,
    onCustom: (CustomTheme) -> Unit,
) {
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(150)
        runCatching { firstFocus.requestFocus() }
    }
    ScreenFrame("Theme Studio", "Pick a preset: it applies immediately and is saved. Presets change layout, shape, type and focus, not just colour.") {
        ThemePresets.all.chunked(2).forEachIndexed { rowIndex, pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                pair.forEachIndexed { colIndex, preset ->
                    PresetCard(
                        preset,
                        selected = preset.id == presetId,
                        onClick = { onPick(preset.id) },
                        modifier = Modifier
                            .weight(1f)
                            .then(if (rowIndex == 0 && colIndex == 0) Modifier.focusRequester(firstFocus) else Modifier),
                    )
                }
            }
        }
        if (presetId == ThemePresets.CUSTOM_ID) {
            SectionTitle("Custom Studio")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Accent", fontSize = 16.sp, color = NexusColors.TextSecondary)
                NexusAccent.entries.forEach { a ->
                    SwatchButton(a.color, selected = a == custom.accent, onClick = { onCustom(custom.copy(accent = a)) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Backdrop", fontSize = 16.sp, color = NexusColors.TextSecondary)
                NexusBackdrop.entries.forEach { b ->
                    TvButton(b.label, onClick = { onCustom(custom.copy(backdrop = b)) }, selected = b == custom.backdrop, horizontalPadding = 18.dp)
                }
            }
            TvStepper(
                "Corner radius",
                "${custom.cornerDp} dp",
                onMinus = { onCustom(custom.copy(cornerDp = (custom.cornerDp - 4).coerceAtLeast(0))) },
                onPlus = { onCustom(custom.copy(cornerDp = (custom.cornerDp + 4).coerceAtMost(48))) },
            )
            TvStepper(
                "Glass strength",
                "${custom.glassPercent} %",
                onMinus = { onCustom(custom.copy(glassPercent = (custom.glassPercent - 2).coerceAtLeast(2))) },
                onPlus = { onCustom(custom.copy(glassPercent = (custom.glassPercent + 2).coerceAtMost(30))) },
            )
        }
        TvButton("Restore defaults", onClick = {
            onPick(ThemePresets.DEFAULT_ID)
            onCustom(CustomTheme())
        })
    }
}

@Composable
fun BackgroundStudioScreen(
    spec: BackgroundSpec,
    onChange: (BackgroundSpec) -> Unit,
    onRestoreDefault: () -> Unit,
    onImportImage: () -> Unit,
) {
    val firstFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(150)
        runCatching { firstFocus.requestFocus() }
    }
    ScreenFrame("Background Studio", "Changes apply live behind this screen and are saved. Animation pauses whenever NEXUS is not on screen.") {
        SectionTitle("Type")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 6.dp, horizontal = 4.dp)) {
            items(BackgroundType.entries.toList(), key = { it.name }) { type ->
                TvButton(
                    type.label,
                    onClick = { onChange(spec.copy(type = type)) },
                    selected = type == spec.type,
                    modifier = if (type == BackgroundType.entries.first()) Modifier.focusRequester(firstFocus) else Modifier,
                    horizontalPadding = 18.dp,
                )
            }
        }
        if (spec.type == BackgroundType.Image) {
            TvButton("Choose image...", onClick = onImportImage)
            Text("Uses the TV's own file picker. If this TV has none, you'll see a message.", fontSize = 13.sp, color = NexusColors.TextMuted)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Primary", fontSize = 16.sp, color = NexusColors.TextSecondary)
                NexusAccent.entries.forEach { a -> SwatchButton(a.color, a == spec.primary, { onChange(spec.copy(primary = a)) }) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Secondary", fontSize = 16.sp, color = NexusColors.TextSecondary)
                NexusAccent.entries.forEach { a -> SwatchButton(a.color, a == spec.secondary, { onChange(spec.copy(secondary = a)) }) }
            }
            TvStepper("Speed", "${spec.speed} / 5", { onChange(spec.copy(speed = (spec.speed - 1).coerceAtLeast(1))) }, { onChange(spec.copy(speed = (spec.speed + 1).coerceAtMost(5))) })
            TvStepper("Intensity", "${spec.intensity} / 5", { onChange(spec.copy(intensity = (spec.intensity - 1).coerceAtLeast(1))) }, { onChange(spec.copy(intensity = (spec.intensity + 1).coerceAtMost(5))) })
            if (spec.type == BackgroundType.Particles) {
                TvStepper("Particle density", "${spec.density} / 5", { onChange(spec.copy(density = (spec.density - 1).coerceAtLeast(1))) }, { onChange(spec.copy(density = (spec.density + 1).coerceAtMost(5))) })
            }
        }
        TvStepper("Brightness", "${spec.brightness} %", { onChange(spec.copy(brightness = (spec.brightness - 10).coerceAtLeast(30))) }, { onChange(spec.copy(brightness = (spec.brightness + 10).coerceAtMost(100))) })
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Quality", fontSize = 16.sp, color = NexusColors.TextSecondary)
            AnimQuality.entries.forEach { q -> TvButton(q.label, { onChange(spec.copy(quality = q)) }, selected = q == spec.quality, horizontalPadding = 18.dp) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            TvButton(if (spec.animate) "Animation: on" else "Animation: off", { onChange(spec.copy(animate = !spec.animate)) }, selected = spec.animate)
            if (spec.type != BackgroundType.Image) {
                TvButton(if (spec.reverse) "Direction: reverse" else "Direction: forward", { onChange(spec.copy(reverse = !spec.reverse)) })
            }
            TvButton("Restore preset default", onClick = onRestoreDefault)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Frame rate is capped by Quality, and quality steps down by itself if frames arrive late. Not benchmarked on your TV yet.",
            fontSize = 12.sp,
            color = NexusColors.TextMuted,
        )
    }
}
