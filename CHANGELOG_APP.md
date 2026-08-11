# EMBED SUITE — Changelog (App)

## v2.1.0 — Simbiosis Xibalba 0.20 (RF scanner, jammer, NFC real, SD, audio)
> 📅 Ago 2026 · TEH-Link v3 extendido + eventos streaming firmware → app

### ✨ Cambios
- **XibalbaAdapter** (`connection/`): fachada tipada con TODOS los comandos —
  sistema (time_sync, set_language, set_mode, power_status), RF (scan/jammer),
  NFC (read/reader/write), IR (capture/transmit), SD (mount/list/save), audio (beep)
- **Eventos streaming** `{"type":"event",...}`: parser en DeviceConnectionManager →
  `RfScanSample`, `RfScanStateChanged`, `RfJammerStateChanged`, `NfcCardDetected`,
  `NfcReaderStateChanged`
- **Servicios** (`services/`): NfcService (lector continuo + persistencia de dumps),
  SdCardService (montaje/listado/guardado de sesiones), AudioService (beeps semánticos
  sincronizados con el firmware), IrService (learn & replay)
- **HybridLocationProvider** (`scan/`): GPS Android + muestras RF del T-Embed con
  reloj sincronizado vía `time_sync`
- **RfScannerScreen** (nueva): barrido RSSI CC1101 con gráfica en vivo + mapa de
  calor osmdroid; acceso desde RF Hub
- **JammerScreen** (nueva): control del jammer headless (frecuencia, potencia,
  continuo/ráfaga, cutoff) con aviso legal; acceso desde RF Hub
- **NfcIrScreen**: lector continuo PN532 con tarjetas en tiempo real + escritura
  NDEF texto
- **Settings**: sección Hardware (toggles NFC/IR/audio/SD, montar SD, test beep) y
  sincronización de idioma app → dispositivo
- Consola: log en vivo de muestras de escaneo, jammer y tarjetas NFC

## v2.0.0 — Phases 2–4 UI + engine wiring
> 📅 Ago 2026 · Workflows, NL console, Autopilot, Bruce sync, Smart OTA, Customizer, Fleet

### ✨ Cambios
- Ops Center tab with hub screens for Workflows, Autopilot, Bruce Config, Firmware Customizer, Fleet
- Workflow UI: built-ins + stored `.ewf`, run/import/export, result display
- Console NL: Spanish/English phrases → TEH-Link JSON via NaturalLanguageTranslator
- Bruce config sync UI: pull/push/backup/restore bruce.json shadow
- Autopilot UI: AUDIT/DEFENSIVE/STEALTH profile picker, start/stop, event log
- Smart OTA guard in Map Tools: flashWithRollback preferred over raw OTA
- Firmware customizer: module checkboxes, manifest JSON, local build queue
- Fleet registry UI: list profiles, set active, nickname, export inventory
- versionCode 2 · versionName 2.0.0

## v1.0.0 — Foundation platform reset
> 📅 Ago 2026 · EmbedSuite v1.0.0 architecture baseline

### ✨ Cambios
- Core device layer: DeviceProfile, capabilities, hardware resolver, SharedPreferences store
- Core TEH-Link protocol constants + migration bridge typealiases
- Engine stubs: workflows (.ewf), autopilot, threat predictor, risk scorer
- UI: SpectrumRadar + HardeningPanel on dashboard via RadarHardeningSection
- Firmware catalog: Xibalba-0.19.0 Maya only (+ custom local)
- minSdk 31 · versionCode 1 · versionName 1.0.0
