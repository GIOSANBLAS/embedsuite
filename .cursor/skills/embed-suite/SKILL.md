---
name: embed-suite
description: Skill oficial para EmbedSuite — companion Android para LilyGO T-Embed CC1101 Plus con firmware Bruce. Multi-transporte BLE/WiFi/USB. Sin espejo de pantalla.
---

# EmbedSuite — Skill de desarrollo (v1.0)

## 1. Identidad del Proyecto

- **Nombre**: EmbedSuite
- **Repositorio**: https://github.com/GIOSANBLAS/embedsuite
- **Package**: `com.embedsuite.app`
- **Firmware objetivo**: [Bruce](https://github.com/BruceDevices/firmware)
- **Hardware**: LilyGO T-Embed CC1101 Plus
- **Control**: Bruce CLI (USB serial, BLE GATT, WiFi WebUI)

---

## 2. Filosofía Central

> **El T-Embed es la Espada (ejecución táctica); Android es la Forja y el Escudo (análisis, gestión, creación).**

**NO** replicamos la interfaz del dispositivo. La app es un **Centro de Inteligencia C2**:

- Analiza espectros y decodifica protocolos (Princeton, EM4100, CAME).
- Gestiona bases de datos (IRDB, señales, tags RFID).
- Forja payloads (DuckyScript, EvilTwin, BLE Spam).
- Control remoto vía **CLI Bruce** (USB / BLE / WiFi WebUI).
- **USB** = CLI serial + flasheo esptool.

---

## 3. Protocolo Bruce BLE (oficial)

Referencia firmware: `src/modules/ble_api/` en BruceDevices/firmware.

| Servicio | UUID | Uso |
|----------|------|-----|
| Serial Service | `4371ec0b-3d43-49f9-b731-7c72a4a7bb91` | Comandos CLI texto (READ/WRITE/NOTIFY) |
| Serial Char | `d555ed97-bf2a-4f46-b3eb-d1fcdd7325e9` | Líneas UTF-8 terminadas en `\n` |
| Battery Service | `0000180f-0000-1000-8000-00805f9b34fb` | Estándar BLE |
| Battery Level | `00002a19-0000-1000-8000-00805f9b34fb` | NOTIFY/READ — % batería |

**Requisito hardware**: Activar **BLE API** en Config de Bruce antes de conectar desde Android.

**Implementación app**:
- `core/bruce/BruceGatt.kt` — UUIDs
- `connection/BleTransport.kt` — GATT Bruce (sin Nordic UART)
- `connection/BruceLinkClient.kt` — ping, telemetría, mapeo CLI C2

**Comandos C2 mapeados (parcial)**:
- `reboot`, `poweroff`
- `subghz tx`, `subghz play`, `subghz scan`, `subghz stop`
- `wifi portal`, `ble spam`
- `wifi scan`, `nfc read`, `ir send`, `ir rx`
- Acciones no mapeadas → error claro *"pendiente de mapeo CLI"*

**Prohibido**: `display start`, parser de framebuffer, clon de menús Bruce.

---

## 4. Estado Actual vs Skill (Gap Analysis — Ago 2026)

### ✅ Implementado (código presente, compila)

| Área | Archivos / pantallas | Notas |
|------|---------------------|-------|
| **Multi-transporte** | `DeviceConnectionManager`, `TransportOrchestrator`, `TransportSelector`, `PendingCommandQueue` | USB/BLE/WiFi abstractos |
| **Bruce BLE** | `BruceGatt`, `BleTransport`, `BruceLinkClient` | Battery GATT + serial CLI |
| **BLE infra** | `BleScanner`, `BleGattManager`, `BleTelemetry`, `BleTelemetryPoller`, `BleCommand` | Telemetría desde `getInfo` |
| **WiFi infra** | `WifiApManager`, `WifiFileTransfer` | Upload + WebUI `/cm` |
| **Dashboard C2** | `DashboardScreen`, wizards BLE/WiFi, Archivos Bruce | CLI companion |
| **Navegación skill** | `MainScreen` — Inicio \| Espectro \| Biblioteca \| Forja | Bottom nav 4 tabs |
| **Espectro UI local** | `SubGhzScreen`, decoders | Análisis `.sub` local — sin stream remoto |
| **Decoders Sub-GHz** | `SubGhzDecoder`, `PrincetonDecoder`, `Em4100Decoder`, `CameDecoder`, `SubFileParser` | Tests unitarios |
| **IRDB sync** | `IrdbSync`, `IrdbParser` | GitHub API Flipper-IRDB |
| **Forja payloads** | `DuckyEditor`, `WifiSpamConfig`, `PayloadForgeScreen` | Editor + config |
| **Biblioteca** | `LibraryScreen` | Señales / IR / exports |
| **Flipper export** | `FlipperZipExporter`, `FlipperFileManager` | `.zip` señales |
| **Flasheo USB** | `EsptoolFlasher`, `FirmwareFlashCoordinator`, `FirmwareFlashCard` | esptool, no OTA TEH-Link |
| **Workflows / Ops** | `WorkflowEngine`, `OpsCenterScreen`, `ScriptExplorerScreen` | Solo scripts `BruceCliScripts` |
| **Headless service** | `EmbedHeadlessService` | Notificaciones + acciones |
| **Idiomas** | ES / EN / zh-rCN | strings.xml |
| **Purga legacy** | Sin TcpTransport, sin pantallas TEH huérfanas | `BruceLinkClient` activo |

### ⚠️ Parcial (existe código, falta E2E hardware o mapeo CLI)

| Área | Qué falta |
|------|-----------|
| **Telemetría BLE real** | Validar batería NOTIFY en T-Embed + Bruce stock; temp/uptime vía CLI si Bruce lo expone |
| **Mapeo CLI Bruce** | ~80% acciones C2 aún sin comando CLI confirmado (`runAction` → null) |
| **Sub-GHz remoto** | Captura TX/replay vía CLI — probar frecuencias reales CC1101 |
| **WiFi streaming** | No existe en Bruce stock — eliminado `WifiWebSocket` |
| **SD remota** | `storage list/read` + upload WiFi ✅ |
| **Encoder remoto** | Skill menciona control encoder — **no implementado** (y no es espejo) |
| **NFC/IR remoto** | UI + modelos OK; inyección depende de CLI Bruce |
| **Strings legacy** | Muchos strings aún dicen "TEH-Link" en ES/EN/ZH |
| **Modelos legacy** | `TehLinkModels.kt`, `TehLinkResponseParser.kt` — renombrar a `Bruce*` en limpieza futura |
| **Autopilot TEH** | Reemplazado por `NoOpAutopilotEngine` |

### ❌ Pendiente (no existe o explícitamente fuera de alcance)

| Área | Estado |
|------|--------|
| **TEH-Link JSON** | Eliminado — no reintroducir |
| **Espejo pantalla TFT** | Fuera de alcance por diseño |
| **OTA inalámbrica TEH-Link** | Eliminada — solo esptool USB |
| **Pairing / crypto v4** | Eliminado |
| **Xibalba / forks legacy** | Eliminado |
| **E2E validado hardware** | Pendiente prueba usuario (BLE API + emparejamiento BT) |
| **Waterfall tiempo real desde Bruce** | Requiere protocolo streaming WiFi/BLE no confirmado en Bruce stock |
| **Control encoder virtual** | No iniciado — definir si Bruce CLI lo soporta |

---

## 5. Los 4 Módulos Estratégicos

| Módulo | Misión | Canal | Estado |
|--------|--------|-------|--------|
| **A** Telemetría C2 | Dashboard, notificaciones, acciones rápidas | BLE | ⚠️ Código OK, E2E pendiente |
| **B** Analizador Sub-GHz | Waterfall, decode, replay | WiFi stream + BLE start/stop | ⚠️ UI/decode OK; stream remoto pendiente |
| **C** IR / RFID | IRDB sync, edición, inyección | WiFi download + BLE write | ⚠️ Sync OK; write remoto pendiente |
| **D** Forja payloads | Ducky, EvilTwin, BLE Spam | WiFi upload + BLE exec | ⚠️ Editor OK; exec remoto parcial |

---

## 6. Estructura de Carpetas (actual)

```
app/src/main/java/com/embedsuite/app/
├── core/
│   ├── bruce/           # BruceGatt.kt ✅
│   ├── ble/             # Scanner, GATT, Telemetry ✅
│   ├── wifi/            # AP, FileTransfer, WebSocket ✅
│   ├── connection/      # Orchestrator, Selector, Queue ✅
│   └── device/          # Perfiles hardware ✅
├── connection/
│   ├── BruceLinkClient.kt ✅
│   ├── BleTransport.kt  ✅ (Bruce GATT)
│   ├── DeviceConnectionManager.kt ✅
│   └── TehLinkModels.kt ⚠️ (legacy naming)
├── engine/
│   ├── decoder/         ✅
│   ├── sync/IrdbSync.kt ✅
│   └── payload/         ✅
├── flipper/             ✅
└── ui/
    ├── screen/          # Spectrum, Library, Forge ✅
    └── components/      # TransportStatusIndicators ✅
```

---

## 7. Orden de Implementación (próximos pasos)

### Prioridad 1 — Validación hardware (bloqueante)
- [ ] Emparejar T-Embed en Android BT
- [ ] Activar BLE API en Bruce Config
- [ ] Conectar EmbedSuite por BLE → ver batería en Dashboard
- [ ] Probar 3 CLI: `reboot`, `subghz scan`, `wifi scan`

### Prioridad 2 — Mapeo CLI Bruce
- [ ] Inventariar comandos serial Bruce (`serialcmds.cpp` upstream)
- [ ] Completar `BruceLinkClient.buildCliCommand()` para toolkit Sub-GHz, WiFi, NFC, IR
- [ ] Renombrar `TehLinkModels` → `BruceModels`, limpiar strings

### Prioridad 3 — WiFi pesado
- [ ] Conectar AP Bruce real + `WifiFileTransfer`
- [ ] Probar descarga `.sub` / `.ir` desde SD del dispositivo

### Prioridad 4 — Espectro live
- [ ] Confirmar si Bruce expone stream; si no, captura por chunks vía SD/WiFi
- [ ] Integrar `WifiWebSocket` con `SpectrumViewModel`

### Prioridad 5 — Limpieza docs
- [ ] README, ARCHITECTURE, MANUAL sin referencias TEH-Link/Xibalba
- [ ] Checklist E2E en `docs/TESTING.md`

---

## 8. Directrices UI/UX

- **Tema**: Oscuro `#0D0D0D`, acentos verde `#00FF88` y cian `#00D4FF`
- **Dashboard**: Tarjeta **ESP32-S3 / CC1101** con chips BATERÍA | TEMP | TRANSPORTE
- **Indicadores**: USB (flash) / BLE (control) / WiFi (archivos) — `TransportStatusIndicators`
- **Navegación**: Inicio | Espectro | Biblioteca | Forja
- **NO** barra superior amontonada con telemetría
- **NO** réplica del menú Bruce en pantalla

---

## 9. Checklist de Validación (hardware real)

- [ ] BLE: escanea, conecta, batería NOTIFY visible
- [ ] BLE: comando CLI responde en consola/log
- [ ] USB: flasheo esptool OK
- [ ] WiFi: join AP Bruce + transferencia archivo
- [ ] Espectro: import `.sub` + decode Princeton/EM4100/CAME
- [ ] IRDB: sync GitHub + búsqueda
- [ ] Ducky: editor + envío CLI (cuando mapeado)
- [ ] Export `.zip` Flipper con señal real capturada

---

## 10. Uso en CURSOR

Activa con `@embed-suite` en el chat.

Ejemplos:
- `@embed-suite, mapea el comando Bruce CLI para subghz tx en BruceLinkClient.`
- `@embed-suite, valida BleTransport contra Bruce ble_api UUIDs.`
- `@embed-suite, diseña control C2 sin espejo para Evil Portal.`

---

## 11. Reglas para el agente

1. **Bruce oficial** — firmware pr3y/Bruce; no forks Xibalba ni TEH-Link custom.
2. **BLE es canal cómodo**; USB y WiFi también ejecutan CLI Bruce.
3. **No espejo de pantalla** — nunca implementar `display start` ni parser framebuffer.
4. **No TEH-Link** — rechazar JSON NDJSON; usar CLI texto o GATT estándar.
5. **Hardware real** — validar en T-Embed CC1101 Plus antes de marcar checklist.
6. **Código útil existente**: `DeviceConnectionManager`, `BruceLinkClient`, `BleTransport`, `SubGhzDecoder`, `IrdbSync`, `FlipperZipExporter`, `TransportOrchestrator`.
7. **Minimizar scope** — completar mapeo CLI acción por acción, no reintroducir capas JSON.

---

## 12. Auditoría de compatibilidad — Bruce stock vs EmbedSuite (Ago 2026)

**Fuente Bruce:** [Serial Wiki](https://github.com/BruceDevices/firmware/wiki/Serial), `serialcmds.cpp`, `ble_api.cpp`, `util_commands.cpp`, `rf_commands.cpp`, `rfid_commands.cpp`.

### Arquitectura Bruce real

| Canal | Cómo funciona en Bruce | EmbedSuite |
|-------|------------------------|------------|
| **USB 115200** | `serialDevice->readStringUntil('\n')` → mismo CLI | `UsbTransport` ✅ compatible (mismo CLI) |
| **BLE API** | `serialDevice = &serial_service` cuando BLE API ON | `BleTransport` UUIDs ✅ coinciden con upstream |
| **WebUI WiFi** | `POST http://bruce.local/cm` body `cmnd=...` | `WifiTransport` ✅ correcto |
| **TCP :8888 NDJSON** | **No existe** en Bruce stock | Eliminado `TcpTransport` ✅ |

**Requisito BLE:** Config Bruce → activar **BLE API** (`enableBLEAPI()` en `settings.cpp`). Consume RAM; adv name `"Bruc"`.

### Comandos — matriz de verdad

| EmbedSuite (`BruceLinkClient`) | Bruce stock CLI | ¿Funciona? |
|-------------------------------|-----------------|------------|
| GATT Battery `0x2A19` | BatteryService NOTIFY (~60s) | ⚠️ Conectar OK; lectura inicial + NOTIFY lenta |
| GATT Serial UUIDs | BLESerialService | ✅ |
| `info` | `info` | ✅ respuesta multilínea vía NOTIFY |
| `reboot` | `reboot` | ✅ |
| `poweroff` | `poweroff` / `power off` | ⚠️ verificar alias exacto |
| `tone F D` | `tone` | ✅ |
| `date` | `date` | ✅ |
| `free` | `free` (heap) | ✅ no usado aún en UI |
| `uptime` | `uptime` | ✅ no usado aún en UI |
| `nav Esc` | `nav esc` (minúsculas) | ❌ **case-sensitive en firmware** |
| `loader open WiFi` | `loader open WiFi` | ✅ (equalsIgnoreCase) |
| `loader open RF` | nombre menú exacto del board | ⚠️ confirmar con `loader list` |
| `ir tx PROTO ADDR CMD` | `ir tx` | ✅ |
| `ir tx_raw FREQ HEX` | `ir tx_raw` | ✅ |
| `ir rx` | `ir rx [seconds]` | ✅ |
| `rfid read TIMEOUT` | `rfid read [timeout]` | ✅ |
| `rfid emulate UID` | `rfid emulate` | ⚠️ parcial |
| `rf tx_from_file PATH false` | `subghz tx_from_file` (alias `rf`) | ✅ |
| `subghz tx KEY FREQ TE COUNT` | formato Flipper CLI | ⚠️ app devuelve null en raw hex TX |
| `rf scan 300 928` | `subghz scan START STOP` en **Hz** (×1e6) | ❌ **formato MHz incorrecto** |
| `wifi portal` | **no existe** en CLI stock | ❌ |
| `ble spam` | **no existe** en CLI stock | ❌ |
| `wifi scan` | **no existe** (hay `wifi on`, `arp`, `sniffer`) | ❌ |
| `storage list /path` | `storage list` / `ls` | ✅ |
| `storage free sd` | revisar subcomando exacto | ⚠️ |
| `badusb` / Ducky | `badusb run_from_file` / `run_from_buffer` | ❌ app aún genera JSON TEH-Link |
| `display start` | existe (espejo) | 🚫 fuera de alcance por diseño |

### Bugs de integración en la app (bloqueantes antes de más features)

1. **`sendCli()` devuelve `"OK"` sin esperar respuesta Bruce** — telemetría/consola no leen NOTIFY tras escribir.
2. **`TcpTransport :8888`** — no funciona con Bruce stock; confundir con WiFi WebUI.
3. **Comandos inventados** (`wifi portal`, `ble spam`, `wifi scan`) — fallarán siempre en Bruce oficial.
4. **`subghz scan`** — frecuencias deben ser Hz (`433920000`), no MHz (`433.92`).
5. **`nav Esc`** — debe ser `nav esc` (minúsculas).
6. **USB tratado como “solo flash”** — el CLI USB **sí funciona** igual que BLE; subutilizado.
7. **`WifiWebSocket` `ws://…:8080/stream`** — **no documentado** en Bruce wiki; asumir no funcional hasta prueba.
8. **`DuckyEditor.toTehLinkExecuteDucky()`** — JSON obsoleto; Bruce usa `badusb run_from_buffer`.
9. **Módulos UI locales** (decoders `.sub`, IRDB sync, Flipper zip) — ✅ funcionan **sin** dispositivo.

### Veredicto

| Capa | ¿Funcional con Bruce stock hoy? |
|------|--------------------------------|
| Conectar BLE + batería | ⚠️ Probable tras emparejar + BLE API ON |
| Control C2 básico (info, tone, reboot, nav, loader) | ⚠️ Tras fixes nav + await respuesta |
| Sub-GHz / IR / NFC remoto | ❌ Parcial — mapeo CLI incorrecto o incompleto |
| Evil Portal / BLE Spam remoto | ❌ No expuesto en CLI stock |
| WiFi WebUI `/cm` | ✅ Mejor canal para comandos hasta BLE estable |
| Espectro live / WiFi stream | ❌ Sin protocolo Bruce confirmado |
| Forja Ducky ejecución remota | ❌ Sigue atada a TEH-Link JSON |
| Análisis local (decode, IRDB, export zip) | ✅ |

**Regla agente:** No añadir módulos nuevos hasta corregir mapeo CLI + lectura de respuestas + eliminar `TcpTransport` TEH-Link y comandos fantasma.

---

## 13. Recursos

- Bruce Firmware: https://github.com/BruceDevices/firmware
- Bruce Serial CLI: https://github.com/BruceDevices/firmware/wiki/Serial
- Bruce BLE API: `src/modules/ble_api/services/BLESerialService.cpp`
- Flipper formats: https://docs.flipperzero.one/
- IRDB: https://github.com/Flipper-XFW/Flipper-IRDB
- Repo: https://github.com/GIOSANBLAS/embedsuite
