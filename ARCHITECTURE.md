# EmbedSuite — Architecture

**v1.0.0 · Bruce firmware companion**

Technical reference for the Android app. User docs: `docs/`.

---

## Overview

EmbedSuite controls the **LilyGO T-Embed CC1101 Plus** via **Bruce CLI** (USB serial, BLE GATT, WiFi WebUI) and file upload over WiFi.

```
┌──────────────────────────────────────────────────────────────────┐
│                    UI (Compose + ViewModels)                      │
│   Dashboard · Tools · Library · Forge · Settings                  │
└─────────────────────────────┬────────────────────────────────────┘
                              │
┌─────────────────────────────▼────────────────────────────────────┐
│              Orchestrator (intents → upload → CLI)                │
│   BadUSB · Sub-GHz async · IR · Spam loader                       │
└─────────────────────────────┬────────────────────────────────────┘
                              │
┌─────────────────────────────▼────────────────────────────────────┐
│                    Connection / Transport                         │
│   USB · BLE · WiFi · auto-discovery · flash USB                   │
└─────────────────────────────┬────────────────────────────────────┘
                              │
┌─────────────────────────────▼────────────────────────────────────┐
│              Data (Room, repositories, export)                    │
└─────────────────────────────┬────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────┐
│         Bruce firmware on T-Embed CC1101 Plus                     │
└──────────────────────────────────────────────────────────────────┘
```

---

## Modules (app)

| Package | Role |
|---------|------|
| `core/orchestrator` | Intent pipeline, discovery, RX completion |
| `connection` | Transports, Bruce link client, device manager |
| `ui` | Screens, navigation, companion tools |
| `data` | Room entities, IRDB, signals, backup |
| `engine` | Payloads, config sync, terminal helpers |
| `flash` | USB firmware update (esptool) |

---

## Transport selection

1. Prefer active transport (USB > BLE > WiFi per user/settings).
2. Heavy files: WiFi upload then CLI on device.
3. Long RX (Sub-GHz): async intent + event-driven completion.

---

## Versioning

- App version: `app/build.gradle.kts` (`versionName`, `versionCode`)
- Changelog: `CHANGELOG_APP.md` (validated at build time)
