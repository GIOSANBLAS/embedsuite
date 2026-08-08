# Roadmap — EmbedSuite + Xibalba

Visión del ecosistema companion + firmware. Release actual: **v2.0.0** (fases 1–4 en app).

---

## Phase 1 — Foundation ✅ (v1.0.0)

| Entrega | Estado |
|---------|--------|
| Arquitectura Core/Engine/UI/Data | ✅ |
| TEH-Link v3 USB | ✅ |
| Device profiles + Dashboard radar/hardening | ✅ |
| OTA USB SHA256 | ✅ |
| Docs v1.0.0 | ✅ |

---

## Phase 2 — Workflows & Automation ✅ (v2.0.0)

| Feature | Estado |
|---------|--------|
| Workflows `.ewf` (motor + UI + import/export) | ✅ |
| Macro / action runner condicional | ✅ |
| `bruce.json` sync + shadow backup | ✅ |
| Smart OTA + rollback si health check falla | ✅ |
| Reconnect backoff + cola de comandos | ✅ |

---

## Phase 3 — Autopilot & Intelligence ✅ (v2.0.0)

| Feature | Estado |
|---------|--------|
| Autopilot AUDIT / DEFENSIVE / STEALTH | ✅ |
| Predictive analysis + countermeasures ES | ✅ |
| Terminal NL → TEH-Link + autocomplete | ✅ |
| RiskScorer en radar/autopilot | ✅ |

---

## Phase 4 — Cloud & Customization ✅ app / 🟡 cloud

| Feature | Estado |
|---------|--------|
| Firmware Customizer **local** (manifest + LOCAL_STAGED) | ✅ |
| Fleet registry (perfiles, nick, inventario JSON) | ✅ |
| Workflow marketplace soft-sign (`embedsuite-local`) | ✅ |
| Firmware Customizer **cloud** build pipeline | 🟡 futuro (manifest listo para API) |
| Telemetry opt-in | 🟡 futuro |
| WiFi/BLE TEH-Link producción | 🟡 estabilización continua |

---

## Firmware (Xibalba) — paralelo

| Phase | Entrega | Estado |
|-------|---------|--------|
| 1 ✅ | TEH-Link v3, UI ES, T-Embed CC1101 | ✅ |
| 2 | Plugin API / bruce.json remoto completo | 🟡 app lista; firmware amplía actions |
| 3 | Autopilot hooks nativos | 🟡 app-driven hoy |
| 4 | OTA A/B nativo | 🟡 app rollback vía flash previo |

---

## Principios

1. **TEH-Link only**
2. **USB first**
3. **AGPL** — Xibalba atribuye Bruce
4. **Security by default**
5. **Field-ready / offline**

---

*Roadmap · EmbedSuite v2.0.0*
