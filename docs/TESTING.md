# Test status and physical checklist

Only fill a result after you actually saw it. Status words: **Compiled** / **Installed** / **Emulator** / **Phone** / **TV stick**.

## Automated (JVM unit tests, run by GitHub Actions)
Core: presets (10, structurally distinct, custom rules), background codec, time/greeting formatting.
TV: favourites/recents list logic, category mapping, search ranking, web-address validation, shortcut encoding.
Mobile: provider validation and status, local command router.
These do **not** cover Compose UI, D-pad focus, app discovery, launching, rendering or performance.

| Item | Result |
|---|---|
| CI build green (both APKs) | not run yet |
| Unit tests passed (count in run summary) | not run yet |

## Physical TV checklist (Motorola DVM4KA01)
| # | Check | Result |
|---|---|---|
| 1 | APK installs; NEXUS TV appears in Apps with banner | |
| 2 | App starts; Home shows greeting, clock, status chips | |
| 3 | D-pad reaches every tile and button; focus ring always visible | |
| 4 | Back from a sub-screen returns Home; Back on Home leaves the app | |
| 5 | All apps lists your real installed apps with icons/banners | |
| 6 | OK opens an app (try 3 different apps); failures show a message | |
| 7 | Hold OK on a tile adds/removes the star; Edit favourites works | |
| 8 | Favourites and Recents survive closing and reopening NEXUS | |
| 9 | Search: typing narrows results; OK opens an app | |
| 10 | Category chips filter the grid | |
| 11 | Themes: each of the 10 presets visibly changes layout/shape/focus; choice survives restart | |
| 12 | Backgrounds: each type renders; speed/intensity/brightness/quality respond; Animation off stops it | |
| 13 | Background looks smooth at Balanced; note any stutter, heat, or lag in other apps | |
| 14 | Choose image: opens a picker or shows the "no file picker" message | |
| 15 | Web apps: add a site (keyboard), Open launches a browser (or says none installed) | |
| 16 | Settings: 12/24-hour, clear data, device info shows real values | |
| 17 | Launcher mode on/off: note whether Home offers a choice. Turn off and confirm the original launcher is back | |
| 18 | Text readable at 720p and 1080p; nothing cut off at screen edges (old LG overscan) | |

## Physical phone checklist (Galaxy F54)
| # | Check | Result |
|---|---|---|
| 1 | APK installs; app opens edge to edge | |
| 2 | Six-section nav works (Home, Assistant, Devices, Settings, More) | |
| 3 | Assistant: "time", "date", "tv", "ai", "help" answer; anything else says it does not understand | |
| 4 | Devices shows Not paired and a disabled Pair button | |
| 5 | Settings: save name (greeting changes), choose language, save a provider; key shows "saved" and is never displayed | |
| 6 | Restart app: name, language, provider choice persist | |
