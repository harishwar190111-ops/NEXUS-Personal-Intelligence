# NEXUS RELEASE 2 — REPOSITORY AUDIT & GAP INVENTORY

**Date**: 2026-11-04  
**Target Repository**: NEXUS-Personal-Intelligence  
**Target Television**: Motorola Android TV Stick DVM4KA01 (Amlogic S905Y2, 2 GB RAM, Android 10)  
**Target Mobile**: Samsung Galaxy F54 (Android 16)  
**Baseline Tests**: 42 passed, 0 failed, 0 skipped across 13 test classes.

---

## 1. Executive Summary

This repository audit evaluates the existing codebase against the **NEXUS Release 2 Master Development Specification** (51,755 bytes) and `EXECUTION_CONSTRAINTS.md`. 

The repository currently contains the foundation of Release 1:
- A functioning Android TV launcher application (`:app-tv`) with Leanback declarations, dynamic package discovery, local favorites/recents, theme/background studio, web shortcuts, and a toggleable `HomeAlias`.
- A foundational Android mobile application (`:app-mobile`) with local navigation, keystore-backed API key storage, a deterministic local assistant router, and static status tiles.
- Multiple isolated core modules (`:core:common`, `:core:remote`, `:core:ai`, `:core:voice`, `:core:memory`, `:core:automation`, `:core:storage`, `:core:design`).

However, cross-device connectivity and intelligence are almost entirely unimplemented in practice:
- `:app-tv` and `:app-mobile` only depend on `:core:common`. None of the remote, AI, voice, memory, automation, or storage libraries are wired into either application.
- Mobile has a disabled button: `"Pair a TV (Release 2)"` with `enabled = false`.
- The TV application has no background server, network receiver, pairing gate dialog, or command executor.
- Neither application requests `android.permission.INTERNET` in their manifests, preventing any local Wi-Fi or AI provider communication.
- Cross-device voice control (English, Tamil, Tanglish) and TTS feedback are absent from the UI and application lifecycles.

---

## 2. Detailed Module Inspection

### 2.1 `:app-tv`
- **Application ID**: `com.nexus.personalintelligence.tv`
- **Namespace**: `com.nexus.personalintelligence.tv`
- **Compile / Target / Min SDK**: 36 / 35 / 26
- **Dependencies**: `:core:common`, Jetpack Compose BOM 2025.06.00, Material 3, Datastore Preferences, Coroutines.
- **Manifest**:
  - `android.software.leanback` required=true; `android.hardware.touchscreen` required=false.
  - Queries for `LEANBACK_LAUNCHER`, `LAUNCHER`, `VIEW https`.
  - `MainActivity`: exported, `launchMode="singleTask"`, landscape orientation, `LEANBACK_LAUNCHER` & `LAUNCHER` intent filters.
  - `HomeAlias` (`activity-alias`): disabled by default (`enabled="false"`), targets `MainActivity`, with `ACTION_MAIN`, `CATEGORY_HOME`, `CATEGORY_DEFAULT`.
- **Existing Features Implemented**:
  - Application discovery via `PackageManager.queryIntentActivities`.
  - Icon loading and disk caching in `cacheDir/wallpaper.jpg`.
  - Local favorites and recent launches saved via `TvPrefsRepository` (DataStore).
  - Theme Studio & Background Studio (presets, custom accent/backdrop, wallpaper image import via `ActivityResultContracts.GetContent()`).
  - Web shortcuts manager with browser resolution.
  - Device info readout (Model, Android version, heap, storage, network).
  - Runtime toggle for `HomeAlias` via `PackageManager.setComponentEnabledSetting`.
- **Gaps**:
  - **No Network Server**: No socket or persistent listener running to accept commands or pairing requests from the phone.
  - **No Security Review Gate**: No 6-digit PIN display or TV approval dialog for pairing.
  - **No Command Dispatcher**: No execution engine for remote D-pad, media keys, app launch, or text injection.
  - **No Session Registry**: Cannot remember or revoke paired phones.
  - **No Text-to-Speech Engine**: No TTS engine wired to read out assistant responses over TV speakers.
  - **No AI Provider Settings on TV**: TV has no interface to configure Gemini/Groq/OpenAI directly.
  - **No Quick Settings Panel**: Spec #11 requires a quick settings panel.
  - **No Screensaver Engine**: Spec #12, #33 lightweight clock/date screensaver with idle timeout is not implemented.
  - **No Universal Search**: Spec #29 requires searching apps, settings, and shortcuts together.
  - **No Backup / Restore Engine**: Spec #37 backup/restore is not connected to UI or disk.
  - **Missing Permissions**: Manifest lacks `INTERNET` and `ACCESS_NETWORK_STATE`.

### 2.2 `:app-mobile`
- **Application ID**: `com.nexus.personalintelligence.mobile`
- **Namespace**: `com.nexus.personalintelligence.mobile`
- **Compile / Target / Min SDK**: 36 / 35 / 26
- **Dependencies**: `:core:common`, Jetpack Compose, Material 3, Datastore Preferences.
- **Manifest**:
  - `MainActivity`: exported with `ACTION_MAIN` and `CATEGORY_LAUNCHER`.
  - **Zero permissions requested** (no `INTERNET`, `RECORD_AUDIO`, `ACCESS_NETWORK_STATE`, or `VIBRATE`).
- **Existing Features Implemented**:
  - Navigation scaffolding: Home, Assistant, Devices, Settings, More.
  - `SecretCipher`: Android Keystore-backed AES-256 GCM encryption for API keys.
  - `MobileSettingsRepository`: Datastore persistence for display name, language, encrypted key.
  - `LocalCommandRouter`: Simple deterministic string matching for `"time"`, `"date"`, `"tv"`, `"ai"`, `"help"`.
  - `ProviderDraft` validator.
- **Gaps**:
  - **Fake / Disabled Pairing Button**: `DevicesScreen` has `GlassButton("Pair a TV (Release 2)", {}, enabled = false)`.
  - **No Network Client**: No socket or persistent transport to connect to TV stick over Wi-Fi.
  - **No Pairing Flow**: No IP entry / discovery, 6-digit PIN input, or approval polling.
  - **No Full Remote Control UI**: Spec #18 (D-pad, OK, Back, Home, Menu, Touchpad, Keyboard) is completely missing.
  - **No Mobile TV Management**: Spec #19 (view TV apps, launch apps, change TV theme/wallpaper from phone) is missing.
  - **No Speech Pipeline**: Spec #21 push-to-talk microphone, SpeechRecognizer, and Tamil/Tanglish/English recognition are not implemented.
  - **No Connection Persistence Service**: Spec addition #1223 requires persistent connection across app switching and screen lock.
  - **No AI Provider Test**: Only "Save provider" exists; no live connection testing.

### 2.3 `:core:common`
- **Package**: `com.nexus.personalintelligence.core.common`
- **Contents**:
  - UI palette (`NexusColors`), accents (`NexusAccent`), backdrops (`NexusBackdrop`), themes (`NexusTheme`), styles (`NexusStyle`).
  - UI components (`GlassPanel`, `GlassButton`, `StatusChip`, `NexusWordmark`).
  - Time utilities (`NexusTime`).
  - Result wrapper (`NexusResult`).
- **Status**: Stable, tested, and shared.

### 2.4 `:core:remote`
- **Package**: `com.nexus.personalintelligence.core.remote`
- **Contents**:
  - Data models: `RemoteCommandType`, `RemoteCommand`, `RemoteLayoutConfig`, `AppCapability`, `ThirdPartyAppCapabilityRegistry`.
- **Gaps**:
  - Contains only models and registry; lacks network client/server implementation, protocol serialization, heartbeat, session tokens, or command acknowledgement payloads.

### 2.5 `:core:ai`
- **Package**: `com.nexus.personalintelligence.core.ai`
- **Contents**:
  - `AiProviderType` (`GEMINI`, `OPENAI`, `GROK`, `LOCAL_CUSTOM`).
  - `AiProviderConfig`, `RecognizedAiIntent`, `NexusAiIntentParser`.
  - `NexusAiEngine`: Basic OkHttp caller for Gemini and OpenAI/Grok endpoints.
- **Gaps**:
  - Not integrated into `:app-tv` or `:app-mobile`.
  - Missing provider connection test method.
  - Missing secret redaction in error messages and logs.
  - Lacks structured command generation output for TV action routing.

### 2.6 `:core:voice`
- **Package**: `com.nexus.personalintelligence.core.voice`
- **Contents**:
  - `VoiceListeningState`, `VoiceInteractionEntry`, `WakeWordCapability`, `NexusVoiceSessionManager`.
- **Gaps**:
  - Contains purely in-memory models and mock state flow.
  - No real Android `SpeechRecognizer` integration or audio permission handling.
  - No TTS engine for speech synthesis.

### 2.7 `:core:memory`
- **Package**: `com.nexus.personalintelligence.core.memory`
- **Contents**:
  - `NexusMemoryItem`, `MemoryCategory`, `NexusMemoryRepository`.
- **Gaps**:
  - In-memory only; no disk persistence across app restarts.
  - Not hooked up to the assistant or TV settings.

### 2.8 `:core:automation`
- **Package**: `com.nexus.personalintelligence.core.automation`
- **Contents**:
  - `AutomationStepStatus`, `AutomationActionPlan`, `NexusAutomationEngine`.
- **Gaps**:
  - Completely isolated; not wired to remote commands or scheduled tasks.

### 2.9 `:core:storage`
- **Package**: `com.nexus.personalintelligence.core.storage`
- **Contents**:
  - `CustomTile`, `NexusFullBackup`, `NexusSettingsRepository`.
- **Gaps**:
  - Not wired to `:app-tv` or `:app-mobile` preferences.

### 2.10 `:core:design`
- **Package**: `com.nexus.personalintelligence.core.design`
- **Contents**:
  - 10 theme presets (`NexusThemePreset`, `ThemeRegistry`), `BackgroundConfig`.
- **Gaps**:
  - Spec Section 4 mandates 12 complete theme presets (Midnight Violet, Electric Blue, Aurora Cyan, Obsidian Black, Cyber Neon, Luxury Purple, Minimal Graphite, Dark Cinematic, OLED Black, Glass Inspired, Premium Gradient, Custom Theme).
  - Currently exists independently from `:core:common.ui.ThemePresets`.

---

## 3. Gap Inventory Against Master Spec

| Spec Section | Feature Description | Current Repo Status | Gap / Required Action |
|---|---|---|---|
| **4. Design System** | 12 theme presets, typography, focus indicators, persistent settings, import/export | Partial | Unify `:core:design` and `:core:common`, implement full 12 presets, theme export/import. |
| **5. TV Home** | Dashboard, typography, clock/date, shortcuts, universal search, loading/error states | Implemented (R1) | Add assistant entry point, quick settings, universal search integration. |
| **6. Default HOME** | `ACTION_MAIN`, `CATEGORY_HOME`, `CATEGORY_DEFAULT`, Leanback, recovery docs | Implemented (R1) | Preserved. HomeAlias activity-alias working. |
| **7. App Library** | Package visibility, search, categories, favorites, move, hide/restore, uninstall flow | Partial | Add hide/restore apps, custom app categories, package visibility audit. |
| **8. Wallpaper Engine** | Built-in wallpapers, file picker, dimming, preview, reset, cache limits | Partial | Add built-in gradient/pattern wallpapers, cache limit eviction. |
| **9. Home Layout Editor** | Configurable sections, app rows, clock/search position, card styles | Missing | Implement visual layout editor in TV Settings. |
| **10. Startup Experience** | Logo, startup animation, optional sound, skip, reduced motion | Partial | Basic wordmark exists; add startup splash transition, sound toggle, fast startup mode. |
| **11. Quick Settings** | Quick access panel (network, display, sound, assistant, device info) | Missing | Implement modal/drawer quick settings panel accessible on TV. |
| **12 & 33. Screensaver** | Clock, date, wallpaper, idle timeout, reduced-motion, power management | Missing | Implement TV screensaver with configurable timeout and wake on D-pad. |
| **13 & 34. Device Dashboard** | Real hardware stats: model, Android version, storage, memory, network | Partial | Existing `DeviceInfo` is good; add live memory and network refresh. |
| **14. Safe Android Settings** | Intent links to system settings, app details, permissions | Partial | Expand system settings shortcuts. |
| **15. Mobile Dashboard** | TV connection card, actual status, TV name, quick remote, assistant shortcut | Partial | Replace static tiles with live connection card, pairing CTA, quick remote shortcut. |
| **16. Mobile Navigation** | Bottom nav: Home, Remote, Touchpad, Keyboard, Assistant, Apps, Themes, Settings | Partial | Add Remote, Touchpad, Keyboard, TV Apps tabs/screens. |
| **17. Phone-to-TV Pairing** | Local network, 6-digit PIN, TV security review gate, session tokens, revocation | Missing / Fake | **Critical R2**: Replace fake button with real persistent socket client/server, PIN dialog on TV, session tokens. |
| **18. Full Mobile Remote** | D-pad, OK, Back, Home, Menu, Touchpad gestures, Keyboard, Volume | Missing | **Critical R2**: Implement real touch remote and send validated commands over socket. |
| **19. Mobile TV Management**| Manage TV apps, themes, wallpapers, and settings from phone | Missing | Implement two-way synced state over authenticated connection. |
| **20. One Shared Assistant** | Single assistant on phone and TV, voice/text command routing to TV | Missing | **Critical R2/R4**: Wire `NexusAiEngine` to structured commands sent to TV. |
| **21. Voice Pipeline** | Speech recognition (EN, TA, Tanglish), push-to-talk, TTS speech output | Missing | Implement Android `SpeechRecognizer` + TTS audio synthesis with language fallback. |
| **22. TV-Only Assistant** | Direct assistant on TV with TV speaker responses | Missing | Implement TV voice/text assistant screen with Android TextToSpeech. |
| **23. Structured Commands** | Whitelist: open app, home, library, settings, wallpaper, theme, info, hide app | Partial | Expand `RemoteCommand` to cover full whitelist; TV validation & execution confirmation. |
| **24 & 25. AI Provider Mgr** | Gemini, Groq, OpenAI, custom endpoints, API key entry on phone & TV, test call | Partial | Add connection test, error reporting (rate limit, model unavailable), TV settings UI. |
| **26. Cross-Device AI Arch** | Phone gateway default; TV-direct option; secret redaction | Partial | Wire Phone Gateway: phone calls provider -> sends command to TV -> returns TV result. |
| **27. Assistant Memory** | View, edit, delete memories, clear all, enable/disable | Missing | Persist `NexusMemoryRepository` to disk; connect to assistant prompts. |
| **28. Offline Local Commands** | Full launcher, remote, app library, theme engine usable offline | Implemented (R1) | Ensure all non-AI actions operate without network or API keys. |
| **29. Universal Search** | Search apps, settings, shortcuts, and assistant actions | Partial | Unify search across apps and settings. |
| **30. Smart Shortcuts** | Custom shortcuts for apps, web, settings, actions | Partial | Support user-defined shortcuts. |
| **31. Recent Activity** | Launch history, command history, timestamps, clear history | Partial | Track command execution log in DataStore/memory. |
| **32. Scheduled Actions** | Schedule theme change, screensaver, etc. | Missing | Implement local scheduler via coroutines / AlarmManager. |
| **35. Diagnostics** | Command log, latency, memory, network diagnostics, export report | Missing | Implement diagnostic log collector and sanitized export. |
| **36. Accessibility** | Large text, high contrast, reduced motion, remote accessibility | Partial | Support reduced motion toggle and high-contrast theme. |
| **37. Backup & Restore** | JSON export/import of themes, layout, shortcuts, preferences | Missing | Wire `NexusSettingsRepository` backup export/restore to file. |
| **38. Multi-Device Sessions**| List paired phones, disconnect, revoke, multiple active sessions | Missing | Add paired devices manager in TV settings. |
| **40. Sound & Feedback** | Navigation, selection, error, startup sounds, volume control | Partial | Integrate `TvAudioEngine` across all TV interactions. |
| **41. Connection Recovery** | Auto-reconnect, network change handling, timeout handling | Missing | Add reconnection loop with exponential backoff on Mobile. |
| **44. Feature Manager** | Settings page displaying honest implementation and availability status | Partial | Replace static list in More tab with dynamic Feature Manager. |
| **Add. 1-10 Persistent Conn**| Background persistence, low-latency socket, real-time keyboard, touchpad throttle | Missing | **Critical Addition**: Persistent TCP socket, keyboard text transmission, touchpad batching. |

---

## 4. Immediate Architectural Plan (Implementation Batches)

Following the constraints, all features will be implemented incrementally in focused batches, accompanied by unit tests at every stage:

1. **Batch 1: Core Networking, Pairing Protocol & Security Review Gate**
   - Implement `core:remote` socket client/server protocol with JSON framing, heartbeat, 6-digit PIN handshake, and session tokens.
   - Implement TV-side background server and modal Security Review Gate.
   - Implement Mobile-side pairing UI, device discovery/manual IP entry, and persistent session storage.
   - Add unit tests for handshake, invalid PIN, session expiry, and command framing.

2. **Batch 2: Full Mobile Remote, Touchpad, Real-Time Keyboard & Persistent Connection**
   - Implement Mobile remote UI: D-pad, media keys, touchpad with motion batching, QWERTY/numeric keyboard.
   - Implement TV command dispatcher (injecting key events, text entry into active focus, app launch).
   - Implement Mobile connection persistence (foreground service / app-lifecycle watcher for background survival).
   - Add unit tests for remote command serialization, keyboard unicode/Tamil handling, and connection lifecycle transitions.

3. **Batch 3: Shared Assistant, Voice Pipeline & Cross-Device Command Engine**
   - Implement `SpeechRecognizer` and Android `TextToSpeech` engines on Phone and TV with Tamil/Tanglish/English handling.
   - Implement Phone Gateway AI flow: voice input -> AI provider / local router -> structured TV command -> execution -> TV speaker & phone feedback.
   - Implement TV-only assistant interface.
   - Implement AI Provider Manager with live connection testing and secret redaction.
   - Add unit tests for command whitelisting, speech error handling, provider connection testing, and memory storage.

4. **Batch 4: TV Environment Expansion (12 Themes, Quick Settings, Screensaver, Layout Editor)**
   - Complete 12 spec-mandated theme presets in `:core:design` / `:core:common`.
   - Build TV Quick Settings panel and Screensaver with idle timeout.
   - Implement Home layout editor and app hide/restore.
   - Add unit tests for layout configuration, theme persistence, and screensaver triggers.

5. **Batch 5: Advanced Features, Multi-Device Management, Diagnostics & Verification**
   - Multi-device pairing management and device revocation on TV.
   - Universal search across apps and settings.
   - Scheduled actions and backup/restore.
   - Diagnostics export with secret redaction.
   - Full automated test suite verification and dual debug APK assembly (`testDebugUnitTest`, `:app-tv:assembleDebug`, `:app-mobile:assembleDebug`).
