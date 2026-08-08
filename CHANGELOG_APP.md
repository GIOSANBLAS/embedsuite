# EmbedSuite · Changelog App Android

## v4.4.0 — Offensive Toolkit: BLE Spam, WiFi Deauth+Probe, Mousejack, SubGHz Spectrum & NFC Clone
> 📅 2026-08-06 · Integración de las 5 herramientas ofensivas top con firmware
> Xibalba (TEH-Link v3 streaming NDJSON) y Modo Auditoría con gating por plugin.

### 🆕 Nuevas 5 Herramientas Ofensivas (FIRMWARE ↔ APP)
- **BLE AD SPAM**: Campañas AppleJuice, SwiftPair, FindMy, HomeKit sin reiniciar stack BLE. Widget Dashboard + ScriptCategory BLE_SPAM.
- **WiFi OFFENSIVE**: Deauth Broadcast raw 802.11 + Probe Sniffer con OUI vendor lookup offline y auto-apagado 5 min. Nueva `ProbeSnifferScreen` con tarjetas RSSI coloreadas.
- **MOUSEJACK NRF24**: Wrapper Logitech/MS 2.4 GHz (scan/inject ducky/clear) integrado en MapTools y Dashboard.
- **SUBGHZ TOOLS**: Spectrum analyzer heatmap (rango/step/pps parametrizables) + auto-decoder (Keeloq/Somfy/Nice/PT2262) + export CSV. Nueva `SpectrumScreen`.
- **NFC CLONE + WRITE**: Mifare Classic 1K read/write (6 keys default, protección UID block 0) + NTAG NDEF URL/WiFi WSC QR. Nueva `NfcCloneScreen` con 3 tabs + preview.

### 🧭 App · Navegación & Pantallas
- NavHost MainScreen rutas `probe_sniffer`, `spectrum_analyzer`, `nfc_clone` + factories ViewModel custom.
- Dashboard sección **OFENSIVE TOOLS · AUDIT MODE**: 5 GlassCards (BLE Spam / WiFi Deauth / Mousejack / Spectrum / NFC Clone) con botón directo a pantalla y acciones TEH-Link inline.
- `ScriptCategory`: 5 categorías nuevas (BLE_SPAM, WIFI_OFFENSIVE, MOUSEJACK, SUBGHZ_TOOLS, NFC_CLONE).
- `BuiltInScriptRepository`: 30 scripts nuevos built-in con `requiresAuditUnlock` en acciones TX.
- `DeviceEvent`: 6 eventos streaming nuevos `BleAdSpamProgress / WifiProbe / MousejackDongle / SubGhzSample / SubGhzDecodedFrame / NfcCloneProgress`.

### 🔒 Modo Auditoría & Optimizations (Firmware)
- 5 plugins Xibalba registrados con `TRY_REGISTER (is_tx_path=true)` respetando gating `settings_plugin_is_allowed`.
- TEH-Link v3 whitelist `action_supported()` + handlers `run_action`/`get_action_state` para los 5 plugins.
- REQUIRES CMake sincronizados en `teh_link` y `plugin_manager` (undefined reference prevenido).
- Optimizaciones: `ensure_bluedroid_up()` sin restart stack, `WIFI_OFFENSIVE_AUTO_OFF_SEC=300`, NFC escritura por sectores de 4 bloques, UID block 0 protegido.
- Fix acción `rx_decode_stop` faltante en lista blanca `subghz_tools` teh_link.

### 🏗 Build
- `versionCode`: 31 → **32**
- `versionName`: "4.3.0" → **"4.4.0"**
- APK debug: 37.77 MB (arm64-v8a + x86_64 incluidos).

---

## v4.3.0 — Evil Portal, Beacon Spam & Modo Auditoría con Xibalba v0.18.0
> 📅 2026-08-06 · Integración completa con firmware Xibalba v0.18.0 con soporte
> para Evil Portal, Beacon Spam, auditoría en tiempo real, fix de plugins Gradle AGP9.

### 🆕 Nuevas Funcionalidades · Evil Portal & Beacon Spam
- **Evil Portal**: Captura y auditoría de portales maliciosos WiFi (EvilPortal)
- **Beacon Spam**: Detección y registro de envío masivo de beacons WiFi
- **Modo Auditoría**: Nueva vista de auditoría en tiempo real con filtros y búsqueda
- **Integración Xibalba v0.18.0**: API firmware totalmente sincronizada

### 🔧 Correcciones Build & Gradle
- **Gradle Plugin AGP9**: Corrección de compatibilidad con AGP 9.0+
- **libs.versions.toml**: Actualización de versiones y dependencias
- **KSP/Compose**: Optimizaciones en compilación Kotlin Symbol Processor
- `versionCode`: 28 → **31**
- `versionName`: "4.1.0" → **"4.3.0"**

### 🧪 Estabilidad & Testing
- Auditoría de eventos de red en tiempo real desde la app
- Dashboard mejorado con vistas de Evil Portal y Beacon Spam
- Integración completa de datos con sistema de auditoría del firmware

---

## v4.2.0 — Documentación & Preparación v4.3.0
> 📅 2026-08-06 · Actualización completa de documentación y sincronización
> con Xibalba v0.18.0 en preparación para features de Evil Portal y Beacon Spam.

### 📚 Documentación
- Documentación completa actualizada para v4.3.0
- Guías para Evil Portal, Beacon Spam y modo Auditoría
- Integración Xibalba v0.18.0 documentada
- Changelog actualizado con changelog del firmware

### 🔀 Sincronización con Firmware
- Alineación con Xibalba v0.18.0 compatible con v4.3.0
- Preparación de modelos de datos para Evil Portal
- Nuevas estructuras de parseo para eventos de auditoría
- `versionCode`: 28 (sin cambio en APK, solo docs)
- `versionName`: "4.1.0" (sin cambio en APK, solo docs)

---

## v4.1.0 — Simbiosis Firmware Xibalba v0.17 "Spark"
> 📅 2026-08-04 · Complementa los hardening features integrados en `t-embed-xibalba`:
> Task Watchdog 30s global, BOD 3.0V, SHA256 OTA verify en lado dispositivo,
> flags Secure Boot V2 + Flash/NVS Encryption, soak/stress test y coredump remoto.

### ✅ Correcciones de estabilidad y compatibilidad
- **Parser TEH-Link alineado con Xibalba 0.17**: soporte para claves modernas
  `twdt_timeout_seconds`, `bod_voltage` y `wdt_panic_reason`, manteniendo
  compatibilidad hacia atrás con firmwares anteriores.
- **Hardening dashboard corregido**: la UI ahora marca estado incompleto cuando
  cualquier flag crítico de seguridad está desactivado.
- **USB CDC más robusto**: `UsbTransport` ahora reconstruye líneas NDJSON parciales
  y evita pérdida de paquetes durante OTA, coredumps y respuestas largas.
- **Fixes anti-crash**: reemplazados varios usos inseguros de `.first()` y
  `.lines().first()` por rutas seguras con `firstOrNull()` y fallback.
- **Hardware Bringup** actualizado con enlaces válidos al test suite,
  script PowerShell y checklist CSV del firmware.
- **Compilación Android Studio estabilizada**: corregidos errores en
  `HardwareBringupScreen`, `ConsoleViewModel` y `DashboardViewModel`.

### 🛡 Seguridad y Hardening Dashboard (nueva tarjeta)
- **HardeningInfo** recibido por `get_info → data.hardening` se representa en `DashboardScreen`
  con 6 flags visuales con ✅ / ⚠️ OFF + color de acento dinámico (verde = ok, naranja = incompleto).
- **TWDT timeout** visible y **Brownout 3.0V** envoltura de voltaje.
- **Secure Boot V2 RSA-3072**, **Flash Encryption XTS-AES-256**, **NVS Encryption**,
  **Stack Canaries + Heap Poisoning** todos auditables en vivo desde el móvil.
- **Banner rojo coredump** si `get_status → coredump_present = true` con botón
  `Borrar coredump` que llama `TehLinkClient.clearCoredump() → cmd="clear_coredump"`.
- **Aviso automático TehLinkNotice** al conectar si detecta
  `wdt_panic_reason != null` (reset anormal previo).

### 📡 Capa Conexión · TEH-Link v3
- **TehLinkHardeningInfo** data class nueva; extendidos `TehLinkDeviceInfo(hardening)`,
  `TehLinkDeviceStatus(heapFreeBytes, psramFreeBytes, coredumpPresent, wdtPanicReason)`,
  `TehLinkActionState(ota : TehLinkOtaStatus?, soak : TehLinkSoakResult?)`.
- **TehLinkOtaStatus** nuevo: `state / bytesWritten / totalSize / sha256Verified` con
  helpers `progressPct`, `isComplete`, `hasError`.
- **TehLinkClient** añade:
  - `getOtaStatus(transport) → Result<TehLinkOtaStatus>`
  - `clearCoredump(transport) → Result<Boolean>`
  - `runSoakStress(transport, iterations, perStepSeconds) → Result<TehLinkSoakResult>`
- **TehLinkOtaUploader.upload()** firma cambió de `Result<String>` a `Result<OtaResult>`
  que incluye `sha256Verified: Boolean`. Tras `ota_finish` espera 600 ms y llama
  `getOtaStatus()` para confirmar que el dispositivo marcó `sha256_verified=true`.
  **Si no se verifica** devuelve `Result.failure("⚠️ NO reinicies. Flash USB.")`.
- **SystemInfo** extendido con campos: `freeHeapBytes`, `freePsramBytes`, `hardening`,
  `coredumpPending`, `wdtPanicReason`, `lastOta: TehLinkOtaStatus`.
- **DeviceConnectionManager.uploadFirmwareOta** reescrito: propaga el resultado del
  uploader a `_systemInfo.lastOta` y emite `DeviceEvent.OtaCompleted(status)`.
- **DeviceConnectionManager.refreshTehLinkSystemInfo** ahora copia hardening, heap/psram,
  coredump y wdtPanicReason desde `get_info` y `get_status`.
- **DeviceEvent.OtaCompleted(val status: TehLinkOtaStatus)** añadido al sealed class.

### ⚡ FirmwareFlashCard (MapToolsScreen)
- Parámetros nuevos `lastOta` y `hardening` propagados desde `systemInfo`.
- **AssistChip SHA256 VERIFIED** cuando `lastOta.sha256Verified == true`.
- **AssistChip ⚠️ NO VERIFICADO** si progresión == 100 pero `sha256_verified = false`.
- **Banner perfil RELEASE** (`NeonPurple`) sugiere aplicar Secure Boot V2 / Flash Encryption
  cuando detecta `secureBoot == false || flashEncryption == false`.

### 🧪 Soak/Stress test desde la app
- **DashboardViewModel.runSoakStress()** expone `TehLinkClient.runSoakStress(500, 0)`
  equivalente al `hardware_test_suite.ps1 #T22 Soak Test 500 iter` del script PowerShell.
- **TehLinkSoakResult.isHealthy** devuelve falso si `failures > 0 || leakBytes ≥ 4096`.

### 🧪 Tests Unitarios (app/src/test/java)
- **TehLinkResponseParserTest** ampliado con tests para claves modernas y backward-compat:
  - `parseDeviceInfo_readsHardeningFlags` (9 flags)
  - `parseHardeningInfo_nullObject_producesAllFalse`
  - `parseDeviceInfo_readsHardeningFlags_backwardCompatOldKeys`
  - `parseDeviceStatus_readsHeapCoredumpAndWdtReason`
  - `parseDeviceStatus_wdtPanic_backwardCompatOldKey`
  - `parseOtaStatus_sha256Verified_producesProgressAndState` (100% OK)
  - `parseOtaStatus_inProgress_partialBytes` (10%)
  - `parseOtaStatus_errorState_hasErrorFlag` (sha256_mismatch)
  - `parseActionState_containsOtaAndSoakFields` (anidados)
  - `parseSoakResult_healthyWhenLeakUnderThreshold` (244 B leak)
  - `parseSoakResult_unhealthyWhenFailuresPresent` (3 fallos)
- Total tests parser: 24.

### 🆙 Metadatos
- `versionCode`: 27 → **28**
- `versionName`: "4.0.7" → **"4.1.0"**

### 🔗 Simbiosis asegurada App ↔ Firmware ↔ Hardware
| Capa | Feature | EmbedSuite Android | t-embed-xibalba firmware | Hardware LilyGO T-Embed C1101 Plus |
|------|---------|---------------------|---------------------------|-------------------------------------|
| Superv. | TWDT 30s global | Tarjeta Hardening + Notice | `esp_task_wdt_init(30,true)` + 7 tasks suscritas | ESP32-S3 WDT integrado |
| Energía | BOD 3.0V nivel 5 | Hardening flag brownout | `CONFIG_ESP32S3_BROWNOUT_DET_LVL5` | LiPo 1S PMIC BQ25896 |
| OTA | SHA256 chunk verify | Chip `✅ SHA256 VERIFIED` post OTA | `mbedtls_sha256_update` → `sha256_verified=true` | Flash 16 MB dual-slot |
| Seguridad | Secure Boot V2 RSA-3072 | Recomendación perfil secure | `sdkconfig.defaults.secure` | Efuses ESP32-S3 (una sola escritura) |
| Seguridad | Flash + NVS Enc | Aviso hardening incompleto | `CONFIG_SECURE_FLASH_ENC_ENABLED=y + NVS_ENC` | WROOM-1 Flash Enc XTS-256 |
| Memoria | Heap leak soak | `runSoakStress()` UI | 500 iter roundtrip pings / refresh | PSRAM 8MB OPI DRAM |
| Estabilidad | Coredump remoto | Banner + Clear button | `esp_core_dump_image_erase()` | Partición `coredump` en flash |
| Post-mortem | WDT panic reason | `wdtPanicReason` en UI | `esp_reset_reason()` + task dump | HW RTC reset causes |

---

## v4.0.7 — Anterior
- OTA TEH-Link básico sin SHA256 verify explícito
- Dashboard sin tarjeta Hardening
- Parser solo cubría plugins, status, hash, BLE/WiFi
