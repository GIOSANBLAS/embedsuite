# EMBED SUITE — Changelog (App)

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
