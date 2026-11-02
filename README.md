# NEXUS - Personal Intelligence System

Native Kotlin Android project: **NEXUS TV** (launcher-style dashboard for the Motorola DVM4KA01 Android TV stick) and **NEXUS Mobile** (control-centre foundation), sharing one design system in `core/common`.

> **Honest status.** Everything here was written without access to a compiler, an emulator, the phone or the TV. The first GitHub Actions run is the first time it will ever be compiled. Nothing is claimed as built, installed or tested until the build log and your own checks say so. See `docs/TESTING.md`.

NEXUS TV is a **normal installable TV app**, not firmware and not an operating system. It does not replace your launcher unless you switch on the optional, reversible Launcher mode and Android lets you choose it.

## Release 1 status

| Area | State |
|---|---|
| TV: real installed-app discovery (PackageManager, icons/banners, launch intents) | Implemented, untested on device |
| TV: favourites, recents, persistence (DataStore) | Implemented, untested on device |
| TV: search (on-screen D-pad keyboard) | Implemented, untested on device |
| TV: categories (from the category each app declares, plus System/Apps) | Implemented. Many TV apps declare none, so most land in "Apps" or "System" |
| TV: 10 theme presets that change layout, shape, type, focus effect, nav position and background | Implemented, untested on device |
| TV: Theme Studio (apply/save presets, Custom Studio with accent, backdrop, corner radius, glass) | Partial: no multiple named custom themes, duplicate or rename yet |
| TV: Background Studio (8 types, colours, speed, intensity, brightness, quality, direction, animation on/off, custom image) | Partial: current settings persist and can be reset, but no named background presets (save/duplicate/rename/delete) yet |
| TV: web shortcuts (add, rename, remove, open in an installed browser) | Partial: no categories, favourites or recents for websites yet |
| TV: Launcher mode (HOME entry point, off by default) | Implemented. Whether the firmware lets you pick it as default is **unverified** |
| TV: device information screen (SoC, board, RAM, ...) | Implemented. Evidence for the OS-feasibility research |
| TV: custom app tiles, per-app overrides, install/uninstall flows | Not implemented (Release 3) |
| Mobile: shell, dashboard, devices screen, local-command assistant, AI-provider settings (key encrypted in the Android Keystore), feature map | Implemented, untested on device |
| Phone-to-TV pairing, remote, touchpad, keyboard, synchronisation | **Not implemented** (Release 2). The apps never show a fake connection |
| AI, voice, memory, automation | **Not implemented** (Release 4) |
| Genuine NEXUS firmware | Research only, see `docs/OS_RESEARCH.md`. Nothing is flashed or modified |

Full requirement-by-requirement mapping: `docs/ROADMAP.md`.

## Repository layout

```
settings.gradle.kts, build.gradle.kts, gradle.properties, gradle/libs.versions.toml
core/common      shared theme, 10 presets, animated backgrounds, glass components, time helpers (+ tests)
app-tv           NEXUS TV (com.nexus.personalintelligence.tv)
app-mobile       NEXUS Mobile (com.nexus.personalintelligence.mobile)
.github/workflows/android-build.yml   tests + both debug APKs
docs/            ROADMAP, OS_RESEARCH, TESTING
```

Toolchain (chosen conservatively, not benchmarked): JDK 17, Android Gradle Plugin 8.13.2, Gradle 8.14.3 (pinned in CI), Kotlin 2.2.0, Compose BOM 2025.06.00, minSdk 26, compileSdk 36, targetSdk 35. No Gradle wrapper is included because its binary jar cannot be created from a phone; CI installs the pinned Gradle itself. No `tv-material`, no Room, no networking libraries. Neither app requests any permission.

## Get the code into GitHub (phone + Codespaces)

1. Download `nexus-release1.zip` to your phone.
2. Open your repo `NEXUS-Personal-Intelligence` in **Codespaces** (browser VS Code).
3. In the Explorer, long-press the empty area, choose **Upload...**, select the ZIP.
4. In the Codespaces terminal:
   ```
   unzip -o nexus-release1.zip && rm nexus-release1.zip
   git add -A && git commit -m "NEXUS Release 1 foundation" && git push
   ```
   If your default branch is not `main` or `master`, edit the `branches:` line in `.github/workflows/android-build.yml`, or just use the manual run in the next step.

## Build the APKs (all on the phone)

1. GitHub mobile website, repo, **Actions**, **Android build**, **Run workflow** (a push also starts it). Use "Desktop site" in the browser menu if the button is missing.
2. Open the finished run. The **summary** shows how many unit tests ran, the package/feature info of each APK, and which signing key was used.
3. Download either way:
   - **Releases**, **Latest NEXUS debug build**: `app-tv-debug.apk` and `app-mobile-debug.apk` directly, no unzip.
   - Or the run's **Artifacts**: `app-tv-debug-apk`, `app-mobile-debug-apk` (zip files; unzip with Samsung My Files).
4. **If the run is red:** open the failed step, or download the artifact `build-logs-and-test-reports`, and send me the first `error:` lines. A first build of never-compiled code can fail on a small mistake.

## Install on the Samsung Galaxy F54

1. Open `app-mobile-debug.apk` from the download. Allow "Install unknown apps" for the browser or My Files when asked.
2. If install is blocked: Settings, Security and privacy, **Auto Blocker** off (it blocks sideloading), and tap "Install anyway" if Play Protect warns about an unknown debug build.

## Install on the Motorola TV stick

Unverified on your device; this is the safest route known.
1. On the phone, open **Send Files to TV**, choose `app-tv-debug.apk`, send it to the TV stick.
2. On the TV, open **Send Files to TV**, received files, select the APK. When Android asks, allow "Install unknown apps" for Send Files to TV, then Install.
3. Open **NEXUS TV** from the Apps row. It works without the phone.
4. If the APK will not open or install, tell me what the TV shows; do not change firmware settings.

## Launcher mode and getting back

Default: NEXUS TV is just an app and your Motorola launcher is untouched.
- Optional: NEXUS TV, Settings, **Launcher mode**, Turn on. This only makes Android list NEXUS as a possible Home app. Press Home and see whether the stick offers a choice. Many Android TV builds do not.
- **Return to normal:** Settings, Launcher mode, Turn off. Or Android Settings, Apps, NEXUS TV, **Clear defaults** / **Uninstall**. If the screen is stuck, hold Home on the remote, or unplug and replug the stick and open Android Settings.
- NEXUS never roots, flashes, or touches the bootloader.

## Stable signing

Without setup, CI signs with a throw-away debug key, so **each new build needs the old NEXUS app uninstalled first** (you lose its settings).
To keep one key for all future builds:
1. GitHub, repo, Settings, Secrets and variables, Actions, **New repository secret**: name `NEXUS_KEYSTORE_PASSWORD`, value any strong password (12+ characters). Keep a copy.
2. Run the workflow. The first run creates a keystore and stores it, password-protected, as an asset of a private release called `signing-key`. Later runs reuse it.
The password never appears in the repo. Do not make the repo public while that release exists. If setup fails, the build continues with the debug key and prints a warning.

## Known limitations (see docs/ROADMAP.md for the full list)

- Nothing has been run on a real device. Performance on the 2 GB stick is unmeasured; the background engine has Low/Balanced/High quality, a frame cap, pauses off-screen and steps down when frames arrive late, but that is a design, not a result.
- Android 10 blocks apps from starting other apps while in the background, so future phone-to-TV "open app" commands will only be reliable while NEXUS TV is on screen.
- Many Android TV boxes have no file picker, so "Choose image" may report an error.
- No DRM, sign-in or streaming-app internals are touched or can be modified.
