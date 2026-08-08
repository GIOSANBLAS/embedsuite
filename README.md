# EmbedSuite

**Plataforma de control Android para LilyGO T-Embed CC1101 Plus y firmware [Xibalba](https://github.com/GIOSANBLAS/xibalba-bruce)** (derivado de Bruce, AGPL-3.0).

**Versión:** 1.0.0 · **Package:** `com.embedsuite.app`

EmbedSuite es el companion oficial del ecosistema Xibalba. Comunicación exclusiva vía **TEH-Link v3** (JSON NDJSON sobre USB OTG). Sin protocolos legacy.

---

## Capacidades (v1.0.0)

| Módulo | Función |
|--------|---------|
| **Dashboard** | Estado de enlace, radar operativo, hardening, OTA |
| **Sub-GHz** | Captura, TX, replay, biblioteca RF |
| **WiFi / BLE / NFC / IR** | Toolkit vía plugins TEH-Link + escaneo del teléfono |
| **Terminal TEH-Link** | Consola JSON directa al firmware |
| **OTA** | Actualización USB con verificación SHA256 |
| **Wardriving** | Mapa GPS con sesiones de campo |
| **Perfiles de dispositivo** | Detección automática XIBALBA vs UNKNOWN |

---

## Arquitectura

Cuatro capas: **Core** · **Engine** · **UI** · **Data**. Detalle en [ARCHITECTURE.md](ARCHITECTURE.md).

```
UI (Compose)  →  Engine (workflows / autopilot)  →  Core (device + TEH-Link)  →  Data (Room)
                                      ↕
                         T-Embed CC1101 + Xibalba (TEH-Link v3)
```

---

## Requisitos

| Componente | Especificación |
|------------|----------------|
| **Teléfono** | Android 12 o superior (API 31+) |
| **Conexión** | USB OTG (recomendado y prioritario) |
| **Hardware** | LilyGO T-Embed CC1101 Plus |
| **Firmware** | [Xibalba v1.0.0](https://github.com/GIOSANBLAS/xibalba-bruce) — asset `xibalba-t-embed-cc1101.bin` @ 0x0 |

> **Stock Bruce** sin parche TEH-Link se detecta como perfil **UNKNOWN** (funcionalidad limitada). Flashea Xibalba para simbiosis completa.

---

## Compilación rápida

```bash
./gradlew clean assembleDebug
```

Instrucciones completas: [BUILD_INSTRUCTIONS.md](BUILD_INSTRUCTIONS.md)

---

## Documentación

| Documento | Contenido |
|-----------|-----------|
| [Manual de usuario](docs/MANUAL_USUARIO.md) | Guía operativa para pentesters |
| [Guía de hardware](docs/GUIA_HARDWARE.md) | T-Embed CC1101 Plus, flasheo, OTG |
| [Arquitectura](ARCHITECTURE.md) | Capas, TEH-Link, perfiles |
| [Build](BUILD_INSTRUCTIONS.md) | Debug / release desde cero |
| [Testing](docs/TESTING.md) | Plan de pruebas unitarias e integración |
| [Roadmap](docs/ROADMAP.md) | Fases futuras del producto |
| [Manual interactivo](docs/manual/index.html) | HTML embebido in-app |

---

## Transporte TEH-Link

1. **USB OTG** — uso diario, captura, TX, OTA, terminal
2. WiFi TEH-Link — experimental
3. BLE TEH-Link — experimental

Emparejamiento: mantén pulsado el botón lateral del T-Embed ~2 s (ventana 120 s).

---

## Repositorios

| Proyecto | URL |
|----------|-----|
| EmbedSuite (esta app) | https://github.com/GIOSANBLAS/embedsuite |
| Xibalba (firmware) | https://github.com/GIOSANBLAS/xibalba-bruce |
| Bruce upstream | https://github.com/BruceDevices/firmware |

---

## Licencia

Companion de uso personal / profesional autorizado. No afiliado a LilyGO ni BruceDevices.

Ver política de privacidad en [Manual de usuario](docs/MANUAL_USUARIO.md).
