# 🏗️ EmbedSuite — Documentación Técnica / Arquitectura

**EmbedSuite v4.4.0 · Firmware Xibalba (TEH-Link v3) · GIOSÁNBLAS**

Este documento describe la arquitectura de la app Android **EmbedSuite**, su integración con el firmware **Xibalba** del T-Embed CC1101 Plus, el protocolo **TEH-Link v3** y el flujo de detección de firmwares.

---

## 1. Visión general

```
┌─────────────────────────────────────────────────────────────────┐
│                        EMBED SUITE (Android)                     │
│                                                                  │
│  ┌──────────────┐   ┌──────────────┐   ┌─────────────────────┐  │
│  │    UI (Compose)│   │  ViewModels  │   │  DeviceConnection   │  │
│  │  Screens/Widget│◄─►│  (StateFlow) │◄─►│  Manager (corrutinas)│  │
│  └──────────────┘   └──────────────┘   └──────────┬──────────┘  │
│                                                   │             │
│  ┌────────────────────────────────────────────────▼──────────┐  │
│  │            Transportes (USB / WiFi / BLE / Mock)           │  │
│  │            TEmbedTransport + X Transport                    │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                               │ TEH-Link v3 (NDJSON)
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│              FIRMWARE XIBALBA (T-Embed CC1101 Plus)             │
│  ┌────────────┐  ┌────────────┐  ┌──────────┐  ┌─────────────┐  │
│  │ plugin_mgr │  │  teh_link  │  │  evil_   │  │  subghz/wi- │  │
│  │  (gating)  │  │  (v3 API)  │  │  portal  │  │  fi/nfc/ir  │  │
│  └────────────┘  └────────────┘  └──────────┘  └─────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. Capas de la app

### 2.1 Capa UI (Compose)

- **Screens** (`app/ui/screen/`): `NfcCloneScreen`, `ProbeSnifferScreen`, `SpectrumScreen`, `ScriptExplorerScreen`, etc.
- **Components** (`app/ui/components/`): `GlassCard`, `NeonButton`, `FirmwareFlashCard`, `HardeningRow`, etc.
- **Theme** (`app/ui/theme/`): colores hacker (MatrixGreen, NeonCyan, NeonRed, KaliBlue).

### 2.2 Capa ViewModel

- Exponen `StateFlow` para la UI.
- `DashboardViewModel`, `ConsoleViewModel`, `NfcCloneViewModel`, `ProbeSnifferViewModel`, `SpectrumViewModel`, `ScriptExplorerViewModel`.
- Se crean mediante `EmbedViewModelFactory` con inyección manual.

### 2.3 Capa de conexión (`app/connection/`)

| Clase | Responsabilidad |
|-------|-----------------|
| `DeviceConnectionManager` | Orquesta transporte, estado, eventos, TEH-Link, OTA |
| `TEmbedTransport` (interfaz) | Contrato común de transporte |
| `UsbTransport` / `WifiTransport` / `BleTransport` / `MockTransport` | Implementaciones de transporte |
| `TehLinkClient` | Cliente del protocolo TEH-Link v3 |
| `TehLinkResponseParser` | Parsea respuestas NDJSON |
| `TehLinkOtaUploader` | Sube firmware OTA con verificación SHA256 |
| `FirmwareCatalog` / `FirmwareRepository` | Catálogo de releases |
| `OtaUpdateChecker` | Comprueba actualizaciones OTA |
| `TehLinkActionPolicy` / `TehLinkCommandPolicy` | Políticas de seguridad (gating) |

### 2.4 Capa de datos (`app/data/`)

- **Room** (`EmbedDatabase`, `Daos.kt`, `Entities.kt`).
- **SQLCipher** para cifrado de la base de datos.
- Repositorios: `SignalRepository`, `MacroRepository`, `ProfileRepository`, `TxHistoryRepository`, `NfcDumpRepository`, `BleProfileRepository`, `RfAutomationRepository`.
- `BackupManager`, `ExportHelper`, `SessionReportGenerator`.

### 2.5 Capas de dominio / utilidades

- `rf/` — análisis RF, decodificadores, replay.
- `nfc/` — lógica NFC.
- `macro/` — motor de macros TEH-Link.
- `scripting/` — scripts built-in.
- `security/` — `SecureStore` (tokens cifrados).
- `field/` — modo campo.
- `map/` — mapas (osmdroid).
- `scan/` — escaneo WiFi/BLE del teléfono + GPS.
- `widget/` — widget de escritorio.

---

## 3. Protocolo TEH-Link v3

TEH-Link v3 es un protocolo **JSON NDJSON** (una línea = un mensaje) sobre USB CDC / WiFi / BLE.

### 3.1 Comandos principales

| Comando | Descripción |
|---------|-------------|
| `ping` | Comprobación de vida |
| `get_info` | Info del dispositivo + plugins + hardening |
| `get_status` | Estado (SD, UI, batería, heap, coredump, sim, capabilities) |
| `get_screen` | Pantalla actual |
| `list_actions` | Lista acciones de plugins |
| `run_action` | Ejecuta una acción de plugin |
| `get_action_state` | Estado de una acción |
| `ota_status` / `ota_finish` | Gestión OTA |
| `clear_coredump` | Borra coredump |
| `pair` | Emparejamiento TEH-Link |

### 3.2 Formato

```json
// Request
{"cmd":"run_action","id":1,"plugin_id":"subghz_analyzer","action":"capture_start","params":{"seconds":15}}

// Response (NDJSON)
{"type":"response","id":1,"ok":true,"data":{...}}
```

### 3.3 Eventos streaming

El firmware emite eventos NDJSON que la app parsea en `DeviceEvent`:

| Evento | Uso |
|--------|-----|
| `RawLine` | Línea cruda |
| `SubGhzSignal` | Señal Sub-GHz capturada |
| `SystemInfoUpdate` | Info del sistema |
| `OtaCompleted` | OTA completada (+ `sha256Verified`) |
| `BleAdSpamProgress` | Progreso BLE spam |
| `WifiProbe` | Probe WiFi detectado |
| `MousejackDongle` | Dongle Mousejack detectado |
| `SubGhzSample` / `SubGhzDecodedFrame` | Muestras RF |
| `NfcCloneProgress` | Progreso clonado NFC |
| `TehLinkNotice` | Avisos (pairing, seguridad) |

---

## 4. Detección automática de firmware

`DeviceConnectionManager` detecta el perfil del firmware conectado:

1. Al conectar un transporte, se ejecuta `detectFirmwareProfile(transport)`.
2. Se envía `ping` TEH-Link.
3. Si responde → `FirmwareProfile.XIBALBA`.
4. Si no → `FirmwareProfile.UNKNOWN` (Bruce / VARSYS sin TEH-Link).

```kotlin
private suspend fun detectFirmwareProfile(transport: TEmbedTransport): FirmwareProfile {
    val pingOk = tehLinkClient.ping(transport).getOrElse { false }
    return if (pingOk) FirmwareProfile.XIBALBA else FirmwareProfile.UNKNOWN
}
```

### Perfiles soportados

| Perfil | TEH-Link | Uso |
|--------|----------|-----|
| `XIBALBA` | ✅ | Funcionalidad completa (dashboard, RF, plugins, OTA) |
| `UNKNOWN` | ❌ | Bruce / VARSYS — solo escaneo del teléfono |

---

## 5. Sistema de plugins y gating (Modo Auditoría)

### 5.1 Registro de plugins

Los plugins ofensivos se registran en el firmware con `TRY_REGISTER(is_tx_path=true)`:

- `ble_ad_spam`
- `wifi_offensive`
- `mousejack`
- `subghz_tools`
- `nfc_clone`

### 5.2 Gating por plugin

Cada acción TX verifica `action_supported()` y `settings_plugin_is_allowed`:

```kotlin
// En la app (TehLinkActionPolicy)
TehLinkActionPolicy.validate(pluginId, action).getOrElse {
    return Result.failure(it)
}
```

- Si el plugin no está en la whitelist → rechazado.
- Si `requiresAuditUnlock` y el modo auditoría está desactivado → bloqueado.
- El desbloqueo es por plugin y se revoca al desactivar el modo.

### 5.3 Modo Auditoría

- Se activa en **Ajustes → Seguridad**.
- Gestiona el acceso a las 5 herramientas ofensivas.
- La UI muestra la sección **OFENSIVE TOOLS · AUDIT MODE** cuando está activo.

---

## 6. Flujo de conexión y OTA

### 6.1 Conexión USB

1. `UsbTransport.connect()` → `UsbSerialManager.conectar()`.
2. Se solicita permiso USB (con `FLAG_IMMUTABLE` para Android 14+).
3. Se abre el puerto serie a 115200 baud.
4. `DeviceConnectionManager` detecta firmware y empareja TEH-Link.
5. Se refresca `get_info` / `get_status` → `SystemInfo`.

### 6.2 OTA con verificación SHA256

1. `uploadFirmwareOta()` verifica SHA256 del `.bin` local.
2. Uploader envía chunks por TEH-Link.
3. Tras `ota_finish`, consulta `getOtaStatus()`.
4. Si `sha256Verified == true` → UI muestra ✅ VERIFIED.
5. Si no → **NO reinicies**, flashea por USB.

---

## 7. Seguridad

| Mecanismo | Detalle |
|-----------|---------|
| **SQLCipher** | Base de datos cifrada AES-256 |
| **SecureStore** | Token TEH-Link cifrado (Android Keystore / EncryptedSharedPreferences) |
| **Redacción** | `TehLinkResponseParser.redactSensitiveResponse` oculta contraseñas/llaves |
| **Gating** | `TehLinkActionPolicy` + `TehLinkCommandPolicy` validan acciones/JSON |
| **Hardening** | Dashboard muestra 6 flags de seguridad del firmware |
| **PendingIntent** | `FLAG_IMMUTABLE` para compatibilidad Android 14+ |

---

## 8. Build

- **AGP**: 9.3.1 · **Kotlin**: 2.3.10 · **KSP**: 2.3.10
- **Compose BOM**: 2026.02.01
- Ver `gradle/libs.versions.toml` y `app/build.gradle.kts`.

```bash
./gradlew clean assembleDebug
```

---

*Documentación técnica v4.4.0 · EmbedSuite · GIOSÁNBLAS*