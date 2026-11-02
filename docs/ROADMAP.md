# NEXUS roadmap: every requirement of the Master Build Specification

Status words: **Done** (code written, not device-tested) / **Partial** / **Planned** (release) / **Blocked** (Android or hardware) / **Research**. Nothing is "Tested" yet; see `docs/TESTING.md`.

## Release 1 (this repository)
| Spec section | Requirement | Status |
|---|---|---|
| 4 TV environment | Dashboard, app launcher, favourites, recents, search, settings, Theme Studio, Background Studio, web apps, device info, connection status, 720p/1080p, safe margins, D-pad, focus, Back | Done |
| 4 | Application categories | Partial (declared app category + System/Apps; no user-defined categories) |
| 4 | Remote-control pairing screen, assistant panel | Planned R2 / R4. Dashboard shows "Phone link: not paired" only |
| 5 App manager | Dynamic PackageManager discovery, names, icons/banners, launch intents, refresh on install/uninstall | Done |
| 5 | Search, favourites, recents | Done |
| 5 | Sort, custom order, grid/horizontal/list layouts, custom card size/icon size/shape per app, rename, hide, per-app background | Partial: presets change card shape/size/aspect globally; per-app customisation is Planned R3 |
| 5 | Install/uninstall/app-info via system flows | Planned R3 |
| 6 Web apps | Add, edit name, delete, open in installed browser, missing-browser message | Done |
| 6 | Categories, favourites, recents, custom icons, installable web apps | Planned R3 |
| 7 Design system | 10 presets differing in layout, shape, spacing, type, glass, focus, navigation, background | Done |
| 7 | Theme Studio: preview+apply (live), persist, Custom Studio, restore defaults | Done |
| 7 | Save/duplicate/rename/delete multiple custom themes | Planned R3 |
| 8 Background Studio | Fluid waves, aurora, flowing gradient, light waves, layered gradients, particles, colour drift, custom image | Done |
| 8 | Colours, speed, direction, intensity, brightness, quality Low/Balanced/High, particle density, animation on/off, restore default, image import | Done (image needs a TV file picker: device-dependent) |
| 8 | Named background presets (save/duplicate/rename/delete); separate transparency control; extra colour layers; gradient angle | Planned R3 |
| 8, 17 Performance | Canvas only, frame cap, pause when not resumed, automatic step-down | Done (design). **Not measured on the stick** |
| 9 Mobile | Home, Connection Manager UI, Assistant (local), AI Provider Settings, About | Done |
| 9 | Remote, Touchpad, Keyboard, Applications, Web Apps, Theme/Background Studio, Device Settings, Memory Settings (phone side) | Planned R2 / R4. Listed honestly in the app's "More" tab |
| 15 Architecture | Kotlin, Compose, Material 3, DataStore, coroutines; modules app-tv, app-mobile, core-common | Done. Room deferred until memory exists |
| 16 Security | No permissions requested, no network code, API key encrypted with Keystore, input validation, safe resets | Done for what exists |
| 19 Build | Pinned toolchain, version catalog, Actions: tests, both APKs, logs, optional stable signing | Done, **never run yet** |
| 20 Launcher | HOME registration, off by default, runtime toggle, Back handling, documented recovery | Done. Default-launcher selection is **unverified** on this firmware |
| 3 Approach B | Feasibility report | Research: `docs/OS_RESEARCH.md` |

## Release 2 - connected phone and TV
Secure pairing (QR or PIN, TV approval), TLS or signed WebSocket (Android 10 cleartext rules), session tokens, reconnect, heartbeat-based truthful status, confirmed settings sync (phone shows success only after the TV acknowledges), D-pad, media keys, app shortcuts, text entry into NEXUS TV fields, touchpad inside NEXUS TV.
Input limits (spec section 10): inside NEXUS TV all commands can work. In other apps only keys Android lets a normal app inject are available; global pointer/keyboard injection needs an Accessibility service or system privileges and is **Blocked** without them.
Android 10 also blocks starting activities from the background, so "open app" from the phone is only dependable while NEXUS TV is in front.

## Release 3 - advanced TV management
Per-app customisation, custom categories, ordering, install/uninstall/app-info flows, web categories/favourites/recents, named theme and background presets, more layouts, profiling on the stick.

## Release 4 - NEXUS Intelligence
Provider integrations (OpenAI-compatible, Gemini, Anthropic, others behind one interface), voice (SpeechRecognizer/TextToSpeech with English, Tamil; Tanglish only as far as recognition allows), user-controlled memory (Room), planning, approved automation, action verification. No always-on wake word is promised.
