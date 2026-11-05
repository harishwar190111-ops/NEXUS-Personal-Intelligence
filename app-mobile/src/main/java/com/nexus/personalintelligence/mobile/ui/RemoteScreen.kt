package com.nexus.personalintelligence.mobile.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.personalintelligence.core.common.ui.GlassPanel
import com.nexus.personalintelligence.core.common.ui.LocalNexusAccent
import com.nexus.personalintelligence.core.common.ui.NexusColors
import com.nexus.personalintelligence.core.common.ui.StatusChip
import com.nexus.personalintelligence.core.common.ui.StatusKind
import com.nexus.personalintelligence.core.remote.ConnectionState
import com.nexus.personalintelligence.core.remote.RemoteCommand
import com.nexus.personalintelligence.core.remote.RemoteCommandType
import com.nexus.personalintelligence.mobile.data.PairedTvInfo

enum class RemoteMode(val label: String) {
    Dpad("D-Pad"),
    Touchpad("Touchpad"),
    Keyboard("Keyboard")
}

@Composable
fun RemoteScreen(
    pairedTv: PairedTvInfo,
    connectionState: ConnectionState,
    statusMessage: String? = null,
    onSendCommand: (RemoteCommand) -> Unit,
    onReconnect: () -> Unit = {},
    onNavigateToDevices: () -> Unit
) {
    var mode by remember { mutableStateOf(RemoteMode.Dpad) }
    val accent = LocalNexusAccent.current.color
    val isConnected = connectionState == ConnectionState.CONNECTED

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header with TV name and connection state
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (pairedTv.isPaired) pairedTv.tvDeviceName.ifBlank { "NEXUS TV" } else "NEXUS TV",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (pairedTv.isPaired) "${pairedTv.host}:${pairedTv.port}" else "Not Paired",
                    fontSize = 12.sp,
                    color = NexusColors.TextMuted
                )
            }
            if (isConnected) {
                StatusChip("Connected", StatusKind.Implemented)
            } else {
                StatusChip(
                    text = if (connectionState == ConnectionState.CONNECTING || connectionState == ConnectionState.RECONNECTING) "Connecting..." else "Disconnected",
                    kind = StatusKind.Alert,
                    modifier = Modifier.clickable { onNavigateToDevices() }
                )
            }
        }

        // Mode switch tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(24.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            RemoteMode.entries.forEach { m ->
                val selected = m == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (selected) accent.copy(alpha = 0.25f) else Color.Transparent)
                        .clickable { mode = m }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = m.label,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected) accent else NexusColors.TextSecondary
                    )
                }
            }
        }

        if (!isConnected) {
            GlassPanel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (connectionState == ConnectionState.NOT_PAIRED) "TV Not Paired" else "TV is disconnected",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = statusMessage ?: if (pairedTv.isPaired) "Could not connect to ${pairedTv.host}:${pairedTv.port}." else "Pair with your TV in Devices to send remote commands.",
                        fontSize = 13.sp,
                        color = NexusColors.TextMuted,
                        textAlign = TextAlign.Center
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (pairedTv.isPaired) {
                            GlassButton("Reconnect", onReconnect)
                        }
                        GlassButton("Go to Devices", onNavigateToDevices)
                    }
                }
            }
        }

        // Content per mode
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (mode) {
                RemoteMode.Dpad -> DpadView(onSendCommand = onSendCommand, enabled = isConnected)
                RemoteMode.Touchpad -> TouchpadView(onSendCommand = onSendCommand, enabled = isConnected)
                RemoteMode.Keyboard -> RemoteKeyboardView(onSendCommand = onSendCommand, enabled = isConnected)
            }
        }
    }
}

@Composable
private fun DpadView(onSendCommand: (RemoteCommand) -> Unit, enabled: Boolean) {
    val accent = LocalNexusAccent.current.color

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick top actions: Power, Settings, Menu
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ControlButton("POWER", Color.Red.copy(alpha = 0.8f), enabled) {
                onSendCommand(RemoteCommand(type = RemoteCommandType.POWER))
            }
            ControlButton("MENU", NexusColors.TextSecondary, enabled) {
                onSendCommand(RemoteCommand(type = RemoteCommandType.MENU))
            }
            ControlButton("SETTINGS", NexusColors.TextSecondary, enabled) {
                onSendCommand(RemoteCommand(type = RemoteCommandType.SETTINGS))
            }
        }

        Spacer(Modifier.height(8.dp))

        // D-Pad Cross Layout
        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, NexusColors.GlassBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // UP
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
                    .size(54.dp)
                    .clip(CircleShape)
                    .clickable(enabled = enabled) { onSendCommand(RemoteCommand(type = RemoteCommandType.DPAD_UP)) },
                contentAlignment = Alignment.Center
            ) {
                Text("▲", fontSize = 22.sp, color = accent)
            }

            // DOWN
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 10.dp)
                    .size(54.dp)
                    .clip(CircleShape)
                    .clickable(enabled = enabled) { onSendCommand(RemoteCommand(type = RemoteCommandType.DPAD_DOWN)) },
                contentAlignment = Alignment.Center
            ) {
                Text("▼", fontSize = 22.sp, color = accent)
            }

            // LEFT
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 10.dp)
                    .size(54.dp)
                    .clip(CircleShape)
                    .clickable(enabled = enabled) { onSendCommand(RemoteCommand(type = RemoteCommandType.DPAD_LEFT)) },
                contentAlignment = Alignment.Center
            ) {
                Text("◀", fontSize = 22.sp, color = accent)
            }

            // RIGHT
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 10.dp)
                    .size(54.dp)
                    .clip(CircleShape)
                    .clickable(enabled = enabled) { onSendCommand(RemoteCommand(type = RemoteCommandType.DPAD_RIGHT)) },
                contentAlignment = Alignment.Center
            ) {
                Text("▶", fontSize = 22.sp, color = accent)
            }

            // CENTER / OK
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.25f))
                    .border(2.dp, accent, CircleShape)
                    .clickable(enabled = enabled) { onSendCommand(RemoteCommand(type = RemoteCommandType.DPAD_CENTER)) },
                contentAlignment = Alignment.Center
            ) {
                Text("OK", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(Modifier.height(8.dp))

        // Navigation row: BACK, HOME
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ControlButton("BACK", NexusColors.TextPrimary, enabled) {
                onSendCommand(RemoteCommand(type = RemoteCommandType.BACK))
            }
            ControlButton("HOME", accent, enabled) {
                onSendCommand(RemoteCommand(type = RemoteCommandType.HOME))
            }
        }

        // Media transport row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ControlButton("⏮ PREV", NexusColors.TextSecondary, enabled) {
                onSendCommand(RemoteCommand(type = RemoteCommandType.PREVIOUS))
            }
            ControlButton("⏪ REW", NexusColors.TextSecondary, enabled) {
                onSendCommand(RemoteCommand(type = RemoteCommandType.REWIND))
            }
            ControlButton("⏯ PLAY", accent, enabled) {
                onSendCommand(RemoteCommand(type = RemoteCommandType.PLAY_PAUSE))
            }
            ControlButton("⏩ FWD", NexusColors.TextSecondary, enabled) {
                onSendCommand(RemoteCommand(type = RemoteCommandType.FAST_FORWARD))
            }
            ControlButton("⏭ NEXT", NexusColors.TextSecondary, enabled) {
                onSendCommand(RemoteCommand(type = RemoteCommandType.NEXT))
            }
        }

        // Volume row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ControlButton("VOL -", NexusColors.TextSecondary, enabled) {
                onSendCommand(RemoteCommand(type = RemoteCommandType.VOLUME_DOWN))
            }
            ControlButton("MUTE", NexusColors.TextMuted, enabled) {
                onSendCommand(RemoteCommand(type = RemoteCommandType.MUTE))
            }
            ControlButton("VOL +", NexusColors.TextSecondary, enabled) {
                onSendCommand(RemoteCommand(type = RemoteCommandType.VOLUME_UP))
            }
        }
    }
}

@Composable
private fun ControlButton(label: String, color: Color, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, NexusColors.GlassBorder, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (enabled) color else color.copy(alpha = 0.4f))
    }
}

@Composable
private fun TouchpadView(onSendCommand: (RemoteCommand) -> Unit, enabled: Boolean) {
    val accent = LocalNexusAccent.current.color

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Swipe to navigate TV focus or pointer. Tap to select.",
            fontSize = 12.sp,
            color = NexusColors.TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White.copy(alpha = 0.04f))
                .border(1.5.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectTapGestures(
                        onTap = {
                            onSendCommand(RemoteCommand(type = RemoteCommandType.DPAD_CENTER))
                        }
                    )
                }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    var accumulatedX = 0f
                    var accumulatedY = 0f
                    val threshold = 40f

                    detectDragGestures(
                        onDragEnd = {
                            accumulatedX = 0f
                            accumulatedY = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            accumulatedX += dragAmount.x
                            accumulatedY += dragAmount.y

                            if (Math.abs(accumulatedX) > threshold) {
                                if (accumulatedX > 0) {
                                    onSendCommand(RemoteCommand(type = RemoteCommandType.DPAD_RIGHT))
                                } else {
                                    onSendCommand(RemoteCommand(type = RemoteCommandType.DPAD_LEFT))
                                }
                                accumulatedX = 0f
                            }

                            if (Math.abs(accumulatedY) > threshold) {
                                if (accumulatedY > 0) {
                                    onSendCommand(RemoteCommand(type = RemoteCommandType.DPAD_DOWN))
                                } else {
                                    onSendCommand(RemoteCommand(type = RemoteCommandType.DPAD_UP))
                                }
                                accumulatedY = 0f
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Touchpad Area", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = accent.copy(alpha = 0.6f))
                Text("Swipe & Tap", fontSize = 12.sp, color = NexusColors.TextMuted)
            }
        }
    }
}

@Composable
private fun RemoteKeyboardView(onSendCommand: (RemoteCommand) -> Unit, enabled: Boolean) {
    val accent = LocalNexusAccent.current.color
    var typedText by remember { mutableStateOf("") }
    var tamilMode by remember { mutableStateOf(false) }

    val tamilVowels = listOf("அ", "ஆ", "இ", "ஈ", "உ", "ஊ", "எ", "ஏ", "ஐ", "ஒ", "ஓ", "ஔ")
    val tamilConsonants1 = listOf("க", "ங", "ச", "ஞ", "ட", "ண")
    val tamilConsonants2 = listOf("த", "ந", "ப", "ம", "ய", "ர")
    val tamilConsonants3 = listOf("ல", "வ", "ழ", "ள", "ற", "ன")
    val tamilModifiers = listOf("ா", "ி", "ீ", "ு", "ூ", "ெ", "ே", "ை", "ொ", "ோ", "்")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Live text input sending directly to TV
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = typedText,
                onValueChange = { newText ->
                    if (enabled) {
                        if (newText.length > typedText.length) {
                            val added = if (newText.startsWith(typedText)) {
                                newText.substring(typedText.length)
                            } else {
                                val prefixLen = newText.commonPrefixWith(typedText).length
                                val suffixLen = newText.substring(prefixLen).commonSuffixWith(typedText.substring(prefixLen)).length
                                newText.substring(prefixLen, newText.length - suffixLen)
                            }
                            if (added.isNotEmpty()) {
                                onSendCommand(RemoteCommand(type = RemoteCommandType.TEXT_INPUT, text = added))
                            }
                        } else if (newText.length < typedText.length) {
                            val deleteCount = typedText.length - newText.length
                            repeat(deleteCount) {
                                onSendCommand(RemoteCommand(type = RemoteCommandType.BACKSPACE))
                            }
                        }
                    }
                    typedText = newText
                },
                modifier = Modifier.weight(1f),
                label = { Text("Type here to send text to TV") },
                placeholder = { Text("Search, URLs, Tamil, English...") },
                singleLine = true,
                colors = nexusFieldColors()
            )
            GlassButton("Send", {
                if (typedText.isNotEmpty() && enabled) {
                    onSendCommand(RemoteCommand(type = RemoteCommandType.TEXT_INPUT, text = typedText))
                    typedText = ""
                }
            })
            GlassButton("Clear", { typedText = "" })
        }

        // Action buttons: Backspace, Space, Enter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GlassButton("⌫ Backspace", {
                if (typedText.isNotEmpty()) typedText = typedText.dropLast(1)
                onSendCommand(RemoteCommand(type = RemoteCommandType.BACKSPACE))
            })
            GlassButton("␣ Space", {
                typedText += " "
                onSendCommand(RemoteCommand(type = RemoteCommandType.TEXT_INPUT, text = " "))
            })
            GlassButton("⏎ Enter", {
                onSendCommand(RemoteCommand(type = RemoteCommandType.ENTER))
            })
        }

        // Toggle English / Tamil Keyboard
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (tamilMode) "தமிழ் விசைப்பலகை (Tamil Keyboard)" else "Quick Keypad", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (tamilMode) accent.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f))
                    .clickable { tamilMode = !tamilMode }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(if (tamilMode) "Switch to ABC" else "தமிழ் (Tamil)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = accent)
            }
        }

        if (tamilMode) {
            // Tamil vowels row
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(tamilVowels) { v ->
                    KeyTile(v, enabled) {
                        typedText += v
                        onSendCommand(RemoteCommand(type = RemoteCommandType.TEXT_INPUT, text = v))
                    }
                }
            }
            // Tamil consonants row 1
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(tamilConsonants1) { c ->
                    KeyTile(c, enabled) {
                        typedText += c
                        onSendCommand(RemoteCommand(type = RemoteCommandType.TEXT_INPUT, text = c))
                    }
                }
            }
            // Tamil consonants row 2
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(tamilConsonants2) { c ->
                    KeyTile(c, enabled) {
                        typedText += c
                        onSendCommand(RemoteCommand(type = RemoteCommandType.TEXT_INPUT, text = c))
                    }
                }
            }
            // Tamil consonants row 3
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(tamilConsonants3) { c ->
                    KeyTile(c, enabled) {
                        typedText += c
                        onSendCommand(RemoteCommand(type = RemoteCommandType.TEXT_INPUT, text = c))
                    }
                }
            }
            // Tamil diacritics / modifiers
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(tamilModifiers) { m ->
                    KeyTile(m, enabled) {
                        typedText += m
                        onSendCommand(RemoteCommand(type = RemoteCommandType.TEXT_INPUT, text = m))
                    }
                }
            }
        } else {
            // Number row
            val numbers = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(numbers) { num ->
                    KeyTile(num, enabled) {
                        typedText += num
                        onSendCommand(RemoteCommand(type = RemoteCommandType.TEXT_INPUT, text = num))
                    }
                }
            }

            // Common symbols
            val symbols = listOf(".", "/", ":", "-", "_", "@", "www.", ".com")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(symbols) { sym ->
                    KeyTile(sym, enabled) {
                        typedText += sym
                        onSendCommand(RemoteCommand(type = RemoteCommandType.TEXT_INPUT, text = sym))
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyTile(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 40.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, NexusColors.GlassBorder, RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.White)
    }
}
