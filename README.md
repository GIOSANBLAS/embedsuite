# EmbedSuite

Android companion app for **T-Embed Xibalba** firmware (TEH-Link over USB/WiFi/BLE).

**Version:** 4.5.0 · **Firmware:** Xibalba-0.19.0 Maya (Bruce runtime + TEH-Link v3)  
Package: `com.embedsuite.app`

## Features (v4.5.0)

- **Offensive Toolkit (Audit Mode):** BLE AD Spam (AppleJuice/SwiftPair/FindMy/HomeKit), WiFi Deauth + Probe Sniffer, Mousejack NRF24, SubGHz Spectrum Analyzer, NFC Clone/Write
- **Evil Portal Detection:** Capture and audit malicious WiFi portals
- **Beacon Spam Detection:** Real-time detection of WiFi beacon spam
- **Multi-Firmware Support:** Automatic detection — **XIBALBA** (Bruce + TEH-Link) vs **UNKNOWN** (stock Bruce / VARSYS without TEH-Link)
- **Hardening Dashboard:** 6 security flags (TWDT, BOD, Secure Boot, Flash/NVS Encryption, Stack Canaries)
- **Sub-GHz RF:** Capture, TX, Replay, library with favorites
- **WiFi/BLE/NFC/IR:** Scanning and capture (phone + T-Embed)
- **OTA Firmware:** USB update with SHA256 verification
- **Wardriving:** GPS heatmap with field session reports
- **TEH-Link CLI:** Direct JSON console to firmware

## Build

```bash
./gradlew clean assembleDebug
```

Or open in Android Studio and Run.

## Documentation

- [Manual Usuario](docs/MANUAL_USUARIO.md) - Guía completa (v4.5.0)
- [Guía Día 1](docs/GUIA_DIA_1.md) - Puesta en marcha rápida
- [Guía de Hardware](docs/GUIA_HARDWARE.md) - T-Embed CC1101 Plus
- [Arquitectura](ARCHITECTURE.md) - Documentación técnica
- [Changelog](CHANGELOG_APP.md) - Historial de versiones
- [Manual Interactivo](docs/manual/index.html) - In-app HTML guide

## Hardware

- **Android Phone** (26+)
- **LilyGO T-Embed CC1101 Plus** (ESP32-S3 + CC1101 + PN532 + IR + SD)
- **Firmware (recomendado):** [xibalba-bruce](https://github.com/GIOSANBLAS/xibalba-bruce) **v0.19.0 Maya** — asset `xibalba-t-embed-cc1101.bin` (merged @ 0x0)
- **Legacy rollback:** [te-embed-xibalba](https://github.com/GIOSANBLAS/te-embed-xibalba) v0.18.0 Iron Shield (ESP-IDF) — solo si necesitas volver atrás

## Transport Priority

1. **USB OTG** (Recommended) - Daily use, capture, TX, OTA
2. **WiFi TEH-Link** (Experimental)
3. **BLE TEH-Link** (Experimental)

## License

Personal use companion · Not affiliated with LilyGO or Flipper.

See [Privacy Policy](docs/MANUAL_USUARIO.md) in-app.
