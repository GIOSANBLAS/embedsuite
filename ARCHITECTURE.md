# EmbedSuite — Architecture

**EmbedSuite v1.0.0 · TEH-Link v3 · Xibalba firmware companion**

Technical reference for the Android control platform. User-facing docs are in Spanish under `docs/`.

---

## 1. Overview

EmbedSuite is a four-layer Android application that controls the **LilyGO T-Embed CC1101 Plus** exclusively through **TEH-Link v3** (newline-delimited JSON over USB CDC).

```
┌──────────────────────────────────────────────────────────────────┐
│                         UI Layer (Compose)                        │
│   Screens · ViewModels · Theme · Widgets · Radar / Dashboard      │
└─────────────────────────────┬────────────────────────────────────┘
                              │
┌─────────────────────────────▼────────────────────────────────────┐
│                        Engine Layer                               │
│   workflow/ — scripted TEH-Link sequences, macros, .ewf (future)  │
│   autopilot/ — field automation, scheduled actions (future)       │
└─────────────────────────────┬────────────────────────────────────┘
                              │
┌─────────────────────────────▼────────────────────────────────────┐
│                         Core Layer                                  │
│   device/ — profiles, connection manager, transports              │
│   tehlink/ — client, parser, OTA uploader, action/command policy  │
└─────────────────────────────┬────────────────────────────────────┘
                              │ TEH-Link v3 (NDJSON)
┌─────────────────────────────▼────────────────────────────────────┐
│                        Data Layer                                   │
│   Room + SQLCipher · repositories · export · backup               │
└──────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│              Xibalba firmware (T-Embed CC1101 Plus)               │
│   Bruce runtime + TEH-Link v3 + Maya/cyber Spanish UI             │
└──────────────────────────────────────────────────────────────────┘
```

---

## 2. Layer responsibilities

### 2.1 Core (`core/device`, `core/tehlink`)

| Package / module | Responsibility |
|------------------|----------------|
| `core/device` | `DeviceConnectionManager`, transport abstraction, firmware profile detection |
| `core/tehlink` | `TehLinkClient`, `TehLinkResponseParser`, `TehLinkOtaUploader`, policies |
| `connection/` *(current)* | USB / WiFi / BLE / Mock transports, firmware catalog |
| `flash/` | USB esptool path, image analysis, flash coordinator |
| `security/` | `SecureStore`, TEH-Link pairing token (Android Keystore) |

**Device profiles**

| Profile | Condition | TEH-Link | Capability |
|---------|-----------|----------|------------|
| **XIBALBA** | Xibalba firmware responds to `ping` | ✅ Full | Dashboard, plugins, OTA, hardening, terminal |
| **UNKNOWN** | Stock Bruce or third-party firmware without TEH-Link | ❌ None / limited | Phone-side scan only; flash Xibalba for full symbiosis |

Detection flow:

1. Transport connects (USB preferred).
2. `TehLinkClient.ping()` over NDJSON.
3. Valid TEH-Link response → `FirmwareProfile.XIBALBA`.
4. Timeout / invalid → `FirmwareProfile.UNKNOWN`.

Official firmware: [GIOSANBLAS/xibalba-bruce](https://github.com/GIOSANBLAS/xibalba-bruce) — merged binary `xibalba-t-embed-cc1101.bin` @ flash offset `0x0`.

### 2.2 Engine (`engine/workflow`, `engine/autopilot`)

| Package | Status | Purpose |
|---------|--------|---------|
| `engine/workflow` | Foundation in v1.0.0 (`macro/`, `scripting/`) | Multi-step TEH-Link sequences, macro engine, future `.ewf` workflows |
| `engine/autopilot` | Planned | Unattended field operations, conditional triggers |

v1.0.0 ships macro/script execution; full workflow and autopilot engines are Phase 2+ (see [ROADMAP](docs/ROADMAP.md)).

### 2.3 UI (`ui/`)

- **Screens** — Dashboard, Sub-GHz, WiFi, NFC/IR, Console, Map & Tools, Settings.
- **ViewModels** — `StateFlow` exposure; factory injection via `EmbedViewModelFactory`.
- **Components** — `GlassCard`, radar widgets, hardening rows, firmware flash card, link debug panel.
- **Theme** — Matrix green / neon cyan cyber palette aligned with Xibalba device UI.

Radar dashboard aggregates link state, battery, device screen mirror, plugin shortcuts, and OTA banners.

### 2.4 Data (`data/`)

- **Room** + **SQLCipher** — encrypted local store.
- **Entities** — signals, macros, profiles, TX history, NFC dumps, BLE profiles, IR payloads.
- **Repositories** — domain access for UI and Engine.
- **Export** — `ExportHelper`, `SessionReportGenerator`, `BackupManager`.

---

## 3. TEH-Link v3 protocol

Single transport protocol. No alternate serial CLI.

### 3.1 Wire format

One JSON object per line (NDJSON). Default baud: 115200 on USB CDC.

```json
{"cmd":"run_action","id":1,"plugin_id":"subghz_analyzer","action":"capture_start","params":{"seconds":30}}
{"type":"response","id":1,"ok":true,"data":{...}}
```

### 3.2 Core commands

| Command | Purpose |
|---------|---------|
| `ping` | Liveness |
| `pair` | TEH-Link pairing (GPIO6 long-press on device) |
| `get_info` | Device info, plugins, hardening flags |
| `get_status` | Battery, heap, SD, capabilities, coredump |
| `get_screen` | Current device UI frame |
| `list_actions` / `run_action` / `get_action_state` | Plugin execution |
| `ota_begin` / `ota_chunk` / `ota_finish` / `ota_status` | USB OTA with SHA256 verify (dual-slot OTA desde Xibalba 0.20) |
| `clear_coredump` | Post-crash cleanup |
| `time_sync` | Android epoch → device clock (shared RF+GPS timeline) |

### 3.2.1 Extended plugins (Xibalba 0.20+)

| Plugin | Actions | Description |
|--------|---------|-------------|
| `rf_scanner` | `start`/`stop`/`status` | Headless CC1101 RSSI sweep (freq_start/freq_end/step/rssi_threshold/dwell_ms) |
| `rf_jammer` | `start`/`stop`/`status` | Headless jammer (continuous/burst) with safety cutoff |
| `nfc_toolkit` | `read`/`reader_start`/`reader_stop`/`write`/`status` | Real headless PN532 (stub in 0.19) |
| `sd_storage` | `mount`/`list`/`save`/`status` | Remote microSD; sessions under `/embedsuite/` |
| `audio` | `beep`/`status` | NS4168 speaker (freq/duration) |
| `device` | + `set_language`/`set_mode`/`power_status` | ES/EN, stealth mode, power state |

App-side facade: `connection/XibalbaAdapter` + services in `services/`
(NfcService, SdCardService, AudioService, IrService) + `scan/HybridLocationProvider`.

### 3.3 Streaming events

Firmware emits NDJSON events parsed into `DeviceEvent`: `SubGhzSignal`, `WifiProbe`, `OtaCompleted`, `TehLinkNotice`, etc.

Xibalba 0.20+ also emits unsolicited `{"type":"event","event":...,"ts":...,"data":{...}}`
lines that `DeviceConnectionManager.handleStreamingEvent` maps to:
`RfScanSample` (sweep), `RfScanStateChanged`, `RfJammerStateChanged`,
`NfcCardDetected`, `NfcReaderStateChanged`. `ts` uses the clock synced via
`time_sync`, shared with the phone GPS timeline.

### 3.4 Security policies

- `TehLinkActionPolicy` — plugin/action whitelist, audit-mode gating for offensive tools.
- `TehLinkCommandPolicy` — raw JSON validation in terminal.
- `TehLinkResponseParser.redactSensitiveResponse` — redacts secrets in logs/UI.

---

## 4. Connection & OTA flow

### USB (primary)

1. `UsbTransport.connect()` → permission + serial open.
2. Profile detection + TEH-Link pair if needed.
3. `get_info` / `get_status` → dashboard refresh.

### OTA

1. Resolve firmware from GitHub catalog or embedded asset.
2. Verify SHA256 locally.
3. Stream chunks via TEH-Link OTA commands.
4. Confirm `sha256_verified` on device before reboot.

---

## 5. Package layout (target)

```
app/src/main/java/com/embedsuite/app/
├── core/
│   ├── device/          # connection manager, profiles (evolving from connection/)
│   └── tehlink/         # protocol client (evolving from connection/)
├── engine/
│   ├── workflow/        # macros, scripts, .ewf (Phase 2)
│   └── autopilot/       # field automation (Phase 3)
├── ui/
│   ├── screen/
│   ├── viewmodel/
│   ├── components/
│   └── theme/
├── data/
│   ├── Entities.kt, Daos.kt, EmbedDatabase.kt
│   └── Repositories.kt, ExportHelper.kt, BackupManager.kt
├── rf/, nfc/, macro/, scripting/, scan/, field/, map/, widget/
└── flash/, security/
```

v1.0.0 maps most Core logic to `connection/` and `flash/`; refactor into `core/device` and `core/tehlink` is incremental.

---

## 6. Build stack

| Tool | Version (reference) |
|------|---------------------|
| AGP | 9.x |
| Kotlin | 2.3.x |
| Compose BOM | 2026.x |
| minSdk | 31 (Android 12) |

```bash
./gradlew clean assembleDebug   # debug
./gradlew assembleRelease       # release (signed)
```

---

## 7. Firmware compatibility matrix

| Firmware | Profile | EmbedSuite v1.0.0 |
|----------|---------|-------------------|
| **Xibalba** (xibalba-bruce) | XIBALBA | Full symbiosis |
| **Stock Bruce** (no TEH-Link patch) | UNKNOWN | Limited — phone tools only |
| **Other / VARSYS** | UNKNOWN | Limited |

---

*Architecture doc · EmbedSuite v1.0.0 · GIOSANBLAS*
