# NEXUS RELEASE 2 - ARCHITECTURAL & SPEC CONFLICT DECISIONS

This document records architectural decisions, spec conflicts, and their resolutions for NEXUS Release 2 in accordance with `EXECUTION_CONSTRAINTS.md`.

---

## 1. Application IDs and Test Coexistence

### Conflict
The Release 1 specification established default application IDs:
- NEXUS TV: `com.nexus.personalintelligence.tv`
- NEXUS Mobile: `com.nexus.personalintelligence.mobile`

Earlier Release 2 coexistence guidelines required that test builds coexist with installed Release 1 builds on physical hardware (Motorola TV stick and Samsung Galaxy F54) so users do not have to uninstall their existing R1 installation to test R2 builds. However, the repository must not break Release 1 package names or default/release configurations.

### Resolution
- Release 1 default/release application IDs remain unchanged:
  - `com.nexus.personalintelligence.tv`
  - `com.nexus.personalintelligence.mobile`
- For debug builds, an `applicationIdSuffix = ".r2debug"` is added to `buildTypes.debug` in both `app-tv/build.gradle.kts` and `app-mobile/build.gradle.kts`.
- This generates debug test package names:
  - TV: `com.nexus.personalintelligence.tv.r2debug`
  - Mobile: `com.nexus.personalintelligence.mobile.r2debug`
- Release configurations, namespaces, and internal package structures remain identical.

---

## 2. Pairing Codes and Security Review Gate

### Conflict
Pairing mechanisms must remain user-friendly on TV screens with a D-pad remote while preventing unauthorized devices on the local network from hijacking TV control. Some proposals suggested short 4-digit codes or silent approval for repeated connections. Furthermore, the master specification (`nexus-user-master-spec.txt`) mandates "Pairing PIN or approval", "Invalid PIN", and "no re-entering the PIN on every reopen", but does not specify the exact digit length, attempt limits, or format for the pairing PIN.

### AGY Decision: 6-Digit Secure PIN Length and Range
- **6-Digit Secure PIN**: Pairing codes are strictly 6 numeric digits generated in the range `100000`–`999999` using `(100000 + secureRandom.nextInt(900000)).toString()`.
  - **No Leading-Zero Mismatch**: Starting strictly at `100000` ensures every PIN has exactly 6 digits without leading zeros, eliminating any mismatch or parsing ambiguity between string and integer representations across phone and TV.
  - **Security Entropy**: 900,000 combinations provides strong protection against local network brute forcing while remaining quick and intuitive to input on a TV screen via remote or phone keypad.
  - **Attempt Limits and Expiry**: Pending pairings expire after 60 seconds. A maximum of 3 failed PIN verification attempts is enforced; on the 3rd failed attempt, the pending pairing is revoked and purged immediately.

### Resolution & Protocol
- **Security Review Gate**:
  - The TV will never automatically approve a new pairing request.
  - When a phone initiates pairing, the TV displays a modal Security Review Gate dialog showing:
    - Requesting device name (e.g., "Samsung Galaxy F54")
    - Client IP address and timestamp
    - The 6-digit verification code
  - The user must explicitly approve the pairing request using the TV remote control (D-pad + OK/Select).
- **Session Tokens**:
  - Upon user approval and successful PIN verification, the TV issues a cryptographically secure session token with an expiry window (default 7 days).
  - The TV persists the approved device identity and token in its local paired devices registry.
  - The phone stores the session token in secure storage (encrypted with Android Keystore) and uses it for authenticated reconnection without re-entering the PIN.
  - The user can revoke any paired device at any time from TV Settings or Mobile Settings.
- **Command Authentication**:
  - Every remote command or AI action payload must contain the valid session token. Unauthenticated or expired requests are rejected immediately with a 401/UNAUTHORIZED status.

---

## 3. Communication Transport and Persistent Connection

### Conflict
Frequent D-pad presses and text typing require low latency (< 50ms), whereas connection state must survive mobile backgrounding, screen locking, and app switching without permanent battery-draining services or flaky HTTP polling.

### Resolution
- **Persistent Socket Transport**:
  - A lightweight TCP server runs on the TV on a dedicated local port (default 8765) using standard Java `ServerSocket` and coroutine-driven connection handlers.
  - Framed JSON protocol with unique message/command IDs, timestamps, and structured acknowledgement payloads.
  - Avoids heavyweight third-party server runtimes that inflate APK size or memory footprint on the 2 GB TV stick.
- **Heartbeat and Keep-Alive**:
  - Ping/pong heartbeat every 5 seconds to detect dropped connections promptly.
  - Connection states truthfully tracked and broadcast: `DISCONNECTED`, `CONNECTING`, `CONNECTED`, `RECONNECTING`, `AUTHENTICATION_FAILED`.
- **Mobile Lifecycle Management**:
  - Mobile maintains the connection in an app-scoped coroutine worker / service manager.
  - Screen transitions within NEXUS Mobile retain the active connection.
  - If the user moves NEXUS Mobile to the background, the connection remains established for remote control until OS termination; upon returning to foreground, reconnection occurs seamlessly using the cached session token.
  - Force-stops or network drops cleanly transition to `DISCONNECTED` with automatic safe retry upon network recovery.

---

## 4. Cross-Device AI Architecture (Phone Gateway vs TV Direct)

### Conflict
Cloud AI providers (Gemini, Groq, OpenAI) require API keys. Typing API keys on TV using a remote D-pad is cumbersome, and storing API keys on the shared TV stick presents security concerns.

### Resolution
- **Primary Mode (Phone Gateway)**:
  - Phone acts as the credential manager and AI engine. API keys are stored in phone Keystore.
  - Voice/text is processed by the phone; intent parsing produces a structured action proposal (`RemoteCommand` / `NexusAction`).
  - Only the structured, validated action (not raw API keys or open-ended prompts) is sent across the local network to the TV.
- **Secondary Mode (TV Direct)**:
  - TV also includes an AI Provider settings interface with masked key entry for standalone TV operation when no phone is connected.
- **Whitelist Enforcement**:
  - TV independently validates every action against a strict whitelist before execution.
  - Destructive or sensitive actions require explicit confirmation.

---

## 5. TV Stick Performance & Memory Budget

### Conflict
Motorola Android TV Stick has 2 GB RAM and Amlogic S905Y2 quad-core Cortex-A53 processor. Complex features (voice TTS, animations, background services, caching) could trigger out-of-memory errors or sluggish UI frame drops.

### Resolution
- Zero local LLMs on the TV.
- Bounded image and icon caches (max 16 MB memory cache; disk cache with LRU eviction).
- Lean background server: single worker thread / coroutine per connection; closed when TV enters deep sleep if network drops.
- Animations default to lightweight Compose hardware-accelerated transitions; reduced motion toggle provided.
- Build system: source `nexus-env.sh` setting `-Xmx1536m` and `--no-daemon` to ensure stable JVM execution.
