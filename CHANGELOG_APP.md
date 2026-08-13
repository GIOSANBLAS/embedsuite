# EMBED SUITE — Changelog (App)

## v2.2.1 — Catálogo Xibalba v0.20.1
> 📅 Ago 2026 · Detecta y recomienda el firmware actual

### ✨ Cambios
- Catálogo oficial: **Xibalba-0.20.1** (splash ilustrado, TEH-Link v3) como release recomendada
- `get_info` / mock alineados a `Xibalba-0.20.1`
- versionCode 5 · versionName 2.2.1

## v2.2.0 — Simbiosis Xibalba v0.20.0
> 📅 Ago 2026 · SD files, i18n zh, scripts TEH-Link, sin guía Día 1

### ✨ Cambios
- `list_files` / `download_file`: listar `/xibalba` y descargar capturas desde Ajustes
- `get_info` parser: `battery` (voltaje/%) y `sd_status` en Dashboard
- Idiomas: español, inglés y **chino simplificado**. Se retira portugués
- Tab **Scripts** documentado y operativo: presets que llaman `run_action` TEH-Link (auditoría, params)
- Se elimina la pantalla in-app «Guía Día 1 · Hardware»
- Manual de usuario y manual HTML alineados a Xibalba v0.20.0
- versionCode 4 · versionName 2.2.0

## v2.1.0 — Engine, security, transport, API 29, i18n
> 📅 Ago 2026 · Workflow state machine, Autopilot triggers, ECDH/AES-GCM, TCP transport, tests

### ✨ Cambios
- Workflow engine: formal state machine (IDLE/RUNNING/COMPLETED/FAILED/CANCELLED) + run events
- Autopilot trigger dispatcher: ON_CONNECT, ON_SIGNAL, SCHEDULED workflow auto-run
- TEH-Link security: ECDH P-256 handshake + AES-256-GCM payload encryption (optional)
- Multi-transport: TransportFactory + TcpTransport (NDJSON @ :8888) for WiFi
- Unit tests: MockK + coroutines-test; JaCoCo coverage gate ≥80% on engine/security/connection
- minSdk 29 (Android 10) with core library desugaring
- i18n: explicit `values-es` + synced `values-en`
- LICENSE GPL-3.0 + README license section
- versionCode 3 · versionName 2.1.0

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
