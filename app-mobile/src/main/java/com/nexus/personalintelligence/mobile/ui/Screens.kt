package com.nexus.personalintelligence.mobile.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexus.personalintelligence.core.common.ui.GlassPanel
import com.nexus.personalintelligence.core.common.ui.LocalNexusAccent
import com.nexus.personalintelligence.core.common.ui.NexusColors
import com.nexus.personalintelligence.core.common.ui.NexusWordmark
import com.nexus.personalintelligence.core.common.ui.StatusChip
import com.nexus.personalintelligence.core.common.ui.StatusKind
import com.nexus.personalintelligence.core.common.util.NexusTime
import com.nexus.personalintelligence.mobile.ai.ProviderDraft
import com.nexus.personalintelligence.mobile.ai.ProviderType
import com.nexus.personalintelligence.mobile.ai.providerStatus
import com.nexus.personalintelligence.mobile.assistant.AssistantContext
import com.nexus.personalintelligence.mobile.assistant.LocalCommandRouter
import com.nexus.personalintelligence.mobile.data.AssistantLanguage
import com.nexus.personalintelligence.mobile.data.MobileSettings
import java.time.LocalDateTime

// ---------------------------------------------------------------- Home

@Composable
fun HomeScreen(settings: MobileSettings, onNavigate: (Tab) -> Unit) {
    val now = remember { LocalDateTime.now() }
    val hello = NexusTime.greetingFor(now.hour)
    val name = settings.displayName
    val provider = settings.provider

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        NexusWordmark()
        Column {
            Text(if (name.isBlank()) hello else "$hello, $name", fontSize = 30.sp, fontWeight = FontWeight.Light)
            Text(NexusTime.formatDate(now.toLocalDate()), fontSize = 15.sp, color = NexusColors.TextSecondary)
        }

        GlassPanel(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Orb()
                Column(Modifier.padding(start = 16.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Assistant", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text(providerStatus(provider), fontSize = 13.sp, color = NexusColors.TextSecondary)
                    StatusChip("Local commands only", StatusKind.Alert)
                }
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusTile("Television", "Not paired", "No connection", StatusKind.Alert, Modifier.weight(1f))
            StatusTile("Memory", "Nothing stored", "Release 4", StatusKind.Planned, Modifier.weight(1f))
        }

        SectionLabel("Quick actions")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassButton("Ask NEXUS", { onNavigate(Tab.Assistant) })
            GlassButton("TVs", { onNavigate(Tab.Devices) })
            GlassButton("AI provider", { onNavigate(Tab.Settings) })
        }

        SectionLabel("What exists in this build")
        GlassPanel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FeatureRow("Local command assistant", "Foundation", StatusKind.Foundation)
                FeatureRow("AI provider settings", "Foundation", StatusKind.Foundation)
                FeatureRow("Device screen", "Foundation", StatusKind.Foundation)
                FeatureRow("Phone to TV pairing", "Release 2", StatusKind.Planned)
                FeatureRow("Remote and keyboard", "Release 2", StatusKind.Planned)
                FeatureRow("Voice (English, Tamil, Tanglish)", "Release 4", StatusKind.Planned)
                FeatureRow("Memory", "Release 4", StatusKind.Planned)
                FeatureRow("Automation", "Release 4", StatusKind.Planned)
            }
        }
    }
}

@Composable
private fun Orb() {
    val accent = LocalNexusAccent.current.color
    val transition = rememberInfiniteTransition(label = "orb")
    val s by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "orbScale",
    )
    Canvas(Modifier.size(56.dp)) {
        val r = size.minDimension / 2f * s
        drawCircle(
            Brush.radialGradient(listOf(accent.copy(alpha = 0.55f), accent.copy(alpha = 0f)), center = center, radius = r),
            radius = r,
        )
        drawCircle(accent.copy(alpha = 0.9f), radius = size.minDimension / 9f)
    }
}

@Composable
private fun StatusTile(title: String, value: String, chip: String, kind: StatusKind, modifier: Modifier) {
    GlassPanel(modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontSize = 12.sp, color = NexusColors.TextSecondary)
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            StatusChip(chip, kind, fontSize = 11.sp)
        }
    }
}

@Composable
private fun FeatureRow(name: String, chip: String, kind: StatusKind) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(name, Modifier.weight(1f), fontSize = 14.sp)
        StatusChip(chip, kind, fontSize = 11.sp)
    }
}

// ---------------------------------------------------------------- Assistant

private data class ChatLine(val fromUser: Boolean, val text: String)

@Composable
fun AssistantScreen(settings: MobileSettings) {
    val accent = LocalNexusAccent.current.color
    val router = remember { LocalCommandRouter() }
    val lines = remember {
        mutableStateListOf(ChatLine(false, "NEXUS local mode. No AI model is connected. Type \"help\" to see what works."))
    }
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val ctx = AssistantContext(settings.provider)

    fun send() {
        val text = input.trim()
        if (text.isEmpty()) return
        lines.add(ChatLine(true, text))
        lines.add(ChatLine(false, router.reply(text, ctx)))
        input = ""
    }

    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) listState.animateScrollToItem(lines.lastIndex)
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text("Assistant", fontSize = 28.sp, fontWeight = FontWeight.Light)
        Spacer(Modifier.height(8.dp))
        StatusChip("Local command mode - no AI model connected", StatusKind.Alert, fontSize = 11.sp)
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(lines) { line ->
                Box(Modifier.fillMaxWidth(), contentAlignment = if (line.fromUser) Alignment.CenterEnd else Alignment.CenterStart) {
                    GlassPanel(
                        Modifier.widthIn(max = 300.dp),
                        borderColor = if (line.fromUser) accent.copy(alpha = 0.6f) else NexusColors.GlassBorder,
                    ) {
                        Text(line.text, Modifier.padding(12.dp), fontSize = 15.sp)
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().imePadding(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text("Try: time, date, tv, ai, help") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { send() }),
                colors = nexusFieldColors(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            )
            GlassButton("Send", { send() })
        }
    }
}

// ---------------------------------------------------------------- Devices

@Composable
fun DevicesScreen() {
    ScreenScaffold("Devices", "TV pairing is not implemented in this release, so no connection can be shown.") {
        GlassPanel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("NEXUS TV", Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    StatusChip("Not paired", StatusKind.Alert)
                }
                InfoRow("Device name", "none")
                InfoRow("Connection", "none verified")
                InfoRow("Last synchronisation", "never")
            }
        }
        GlassButton("Pair a TV (Release 2)", {}, enabled = false)
        SectionLabel("Set up the TV app")
        GlassPanel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("1. Install app-tv-debug.apk on the TV box (see the README).", fontSize = 14.sp)
                Text("2. Open NEXUS TV from the Apps row. It works on its own, without this phone.", fontSize = 14.sp)
                Text("3. Pairing, remote control and live sync arrive in Release 2. Nothing here pretends otherwise.", fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, Modifier.weight(1f), fontSize = 14.sp, color = NexusColors.TextMuted)
        Text(value, fontSize = 14.sp)
    }
}

// ---------------------------------------------------------------- Settings

@Composable
fun SettingsScreen(
    settings: MobileSettings,
    onName: (String) -> Unit,
    onLanguage: (AssistantLanguage) -> Unit,
    onSaveProvider: (ProviderDraft, (String?) -> Unit) -> Unit,
    onRemoveKey: () -> Unit,
) {
    val context = LocalContext.current
    val accent = LocalNexusAccent.current.color
    val saved = settings.provider
    var name by remember(settings.displayName) { mutableStateOf(settings.displayName) }
    var type by remember(saved.type) { mutableStateOf(saved.type) }
    var baseUrl by remember(saved.baseUrl, saved.type) { mutableStateOf(saved.baseUrl) }
    var model by remember(saved.model) { mutableStateOf(saved.model) }
    var apiKey by remember(saved.hasApiKey) { mutableStateOf("") }
    var feedback by remember { mutableStateOf<String?>(null) }
    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "unknown"
    }

    ScreenScaffold("Settings") {
        SectionLabel("Profile")
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Your name (used in the greeting)") },
            colors = nexusFieldColors(),
        )
        GlassButton("Save name", { onName(name) })

        SectionLabel("Assistant language")
        AssistantLanguage.entries.forEach { l ->
            Row(
                Modifier.fillMaxWidth().clickable { onLanguage(l) }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = l == settings.language,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(selectedColor = accent, unselectedColor = NexusColors.TextMuted),
                )
                Spacer(Modifier.width(10.dp))
                Text(l.label, fontSize = 15.sp)
            }
        }
        Text(
            "Saved for later. Speech input and voice output arrive in Release 4; this setting changes nothing yet.",
            fontSize = 12.sp,
            color = NexusColors.TextMuted,
        )

        SectionLabel("AI provider")
        Text(providerStatus(saved), fontSize = 13.sp, color = NexusColors.TextSecondary)
        ProviderType.entries.forEach { t ->
            Row(
                Modifier.fillMaxWidth().clickable {
                    type = t
                    baseUrl = t.defaultBaseUrl
                    feedback = null
                }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = t == type,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(selectedColor = accent, unselectedColor = NexusColors.TextMuted),
                )
                Spacer(Modifier.width(10.dp))
                Text(t.label, fontSize = 15.sp)
            }
        }
        if (type != ProviderType.NONE) {
            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Base URL") },
                colors = nexusFieldColors(),
            )
            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Model name") },
                colors = nexusFieldColors(),
            )
            if (type.needsKey) {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(if (saved.hasApiKey) "API key (saved - type to replace)" else "API key") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = nexusFieldColors(),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GlassButton("Save provider", {
                onSaveProvider(ProviderDraft(type, baseUrl, model, apiKey, saved.hasApiKey)) { error ->
                    feedback = error ?: "Saved on this phone. Not verified: this build never contacts the provider."
                    if (error == null) apiKey = ""
                }
            })
            if (saved.hasApiKey) GlassButton("Remove key", { onRemoveKey() })
        }
        feedback?.let { Text(it, fontSize = 13.sp, color = NexusColors.TextSecondary) }
        Text(
            "The API key is encrypted with a key held in the Android Keystore. This app requests no network permission, so nothing is sent anywhere.",
            fontSize = 12.sp,
            color = NexusColors.TextMuted,
        )

        SectionLabel("About NEXUS")
        GlassPanel(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                InfoRow("NEXUS Mobile", versionName)
                InfoRow("Release", "1 (foundation)")
                InfoRow("Permissions requested", "none")
            }
        }
    }
}

// ---------------------------------------------------------------- More (feature map)

private data class Section(val name: String, val detail: String, val chip: String, val kind: StatusKind, val tab: Tab? = null)

private val sections = listOf(
    Section("Home", "Dashboard showing the real status of every part.", "Foundation", StatusKind.Foundation, Tab.Home),
    Section("Remote", "D-pad and media remote for NEXUS TV. Needs pairing.", "Release 2", StatusKind.Planned),
    Section("Touchpad", "Pointer control. Android limits this to NEXUS TV itself unless extra permissions exist.", "Release 2", StatusKind.Planned),
    Section("Keyboard", "QWERTY and numeric entry into NEXUS TV text fields.", "Release 2", StatusKind.Planned),
    Section("Applications", "Manage TV app order, favourites and categories from the phone. The TV app already has them locally.", "Release 2", StatusKind.Planned),
    Section("Web Apps", "Edit TV website shortcuts from the phone. TV-side shortcuts exist.", "Release 2", StatusKind.Planned),
    Section("Theme Studio", "Choose TV presets from the phone. The 10 presets exist on the TV.", "Release 2", StatusKind.Planned),
    Section("Background Studio", "Tune TV backgrounds from the phone. The studio exists on the TV.", "Release 2", StatusKind.Planned),
    Section("Device Settings", "Remote sensitivity, animation quality and TV display preferences.", "Release 2", StatusKind.Planned),
    Section("Connection Manager", "Pairing status. The screen exists; pairing itself does not.", "Foundation", StatusKind.Foundation, Tab.Devices),
    Section("Assistant", "Keyword commands only. No AI model.", "Foundation", StatusKind.Foundation, Tab.Assistant),
    Section("AI Provider Settings", "Stores provider, URL, model and an encrypted key. Never contacts the provider yet.", "Foundation", StatusKind.Foundation, Tab.Settings),
    Section("Memory Settings", "User-controlled memory. Nothing is stored today.", "Release 4", StatusKind.Planned),
    Section("About NEXUS", "Version and honest status.", "Implemented", StatusKind.Implemented, Tab.Settings),
)

@Composable
fun MoreScreen(onOpen: (Tab) -> Unit) {
    ScreenScaffold("Control centre", "Every planned section and its real status. Tap one that exists to open it.") {
        sections.forEach { s ->
            GlassPanel(Modifier.fillMaxWidth()) {
                Column(
                    Modifier
                        .then(if (s.tab != null) Modifier.clickable { onOpen(s.tab) } else Modifier)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(s.name, Modifier.weight(1f), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        StatusChip(s.chip, s.kind, fontSize = 11.sp)
                    }
                    Text(s.detail, fontSize = 13.sp, color = NexusColors.TextSecondary)
                }
            }
        }
    }
}
