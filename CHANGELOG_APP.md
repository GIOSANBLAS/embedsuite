# EmbedSuite · Changelog App Android

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
