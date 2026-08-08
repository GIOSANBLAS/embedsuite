# Roadmap — EmbedSuite + Xibalba

Visión a largo plazo del ecosistema companion + firmware. Primera release pública: **v1.0.0**.

---

## Phase 1 — Foundation ✅ (v1.0.0)

**Estado: completado**

| Entrega | Descripción |
|---------|-------------|
| EmbedSuite v1.0.0 | Companion Android API 31+, arquitectura Core/Engine/UI/Data |
| Xibalba v1.0.0 | Firmware Bruce + TEH-Link v3, UI Maya/cyber español |
| TEH-Link v3 USB | Protocolo NDJSON nativo, pairing GPIO6 |
| Device profiles | XIBALBA vs UNKNOWN |
| Dashboard + terminal | Radar operativo, consola JSON |
| OTA USB | SHA256 verified flash |
| Hardening panel | 6 flags de seguridad firmware |
| Documentación v1.0.0 | README, arquitectura, manual, hardware, testing |

---

## Phase 2 — Workflows & Automation

| Feature | Descripción |
|---------|-------------|
| **Workflows `.ewf`** | Formato declarativo de secuencias TEH-Link; editor in-app |
| **Macro engine v2** | Condicionales, loops, variables de sesión |
| **bruce.json sync** | Sincronización bidireccional config firmware ↔ app |
| **Smart OTA rollback** | Rollback automático si post-OTA health check falla |
| **Engine package refactor** | `core/device`, `core/tehlink`, `engine/workflow` |

---

## Phase 3 — Autopilot & Intelligence

| Feature | Descripción |
|---------|-------------|
| **Autopilot** | Operaciones de campo desatendidas, triggers por evento TEH-Link |
| **Predictive analysis** | Correlación RF/WiFi/BLE, scoring de anomalías |
| **Advanced terminal NL** | Terminal con comandos en lenguaje natural → TEH-Link |
| **WiFi / BLE TEH-Link stable** | Transportes experimentales → producción |

---

## Phase 4 — Cloud & Customization

| Feature | Descripción |
|---------|-------------|
| **Firmware Customizer cloud** | Build Xibalba personalizado (plugins, tema, hardening) |
| **Fleet management** | Multi-dispositivo, inventario, políticas |
| **Signed workflow marketplace** | Workflows `.ewf` firmados y compartidos |
| **Telemetry opt-in** | Métricas anónimas de estabilidad (opt-in estricto) |

---

## Firmware (Xibalba) — paralelo

| Phase | Entrega |
|-------|---------|
| 1 ✅ | TEH-Link v3, UI español, target `lilygo-t-embed-cc1101` |
| 2 | Plugin API estable, bruce.json remoto |
| 3 | Autopilot hooks en firmware |
| 4 | OTA A/B con rollback inteligente |

---

## Principios

1. **TEH-Link only** — sin protocolos legacy.
2. **USB first** — WiFi/BLE como complemento, no sustituto.
3. **AGPL compliance** — Xibalba mantiene atribución Bruce y fuentes abiertas.
4. **Security by default** — Modo Auditoría, gating, SHA256 OTA, SQLCipher.
5. **Field-ready** — diseñado para operación offline en pentest.

---

## Contribuir

Issues y PRs en:

- App: https://github.com/GIOSANBLAS/embedsuite
- Firmware: https://github.com/GIOSANBLAS/xibalba-bruce

---

*Roadmap · EmbedSuite v1.0.0 · actualizado en release inicial*
