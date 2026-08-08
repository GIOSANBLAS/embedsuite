# Plan de pruebas — EmbedSuite v1.0.0

Estrategia de testing unitario e integración por módulo. Objetivo: release estable del companion Xibalba.

---

## 1. Alcance

| Módulo | Capa | Prioridad |
|--------|------|-----------|
| Core TEH-Link | Core | P0 |
| Device profiles | Core | P0 |
| Engine workflows | Engine | P1 |
| UI radar / dashboard | UI | P1 |
| Data export / backup | Data | P1 |
| Device profiles (hardware) | Core | P0 |

---

## 2. Core — TEH-Link

### Unit tests

| Test | Verifica |
|------|----------|
| `TehLinkResponseParserTest` | Parse NDJSON válido / malformado |
| `TehLinkResponseParserTest.redact` | Redacción de campos sensibles |
| `TehLinkActionPolicyTest` | Whitelist plugin/action |
| `TehLinkActionPolicyTest.auditGate` | Bloqueo sin Modo Auditoría |
| `TehLinkCommandPolicyTest` | Validación JSON terminal |
| `TehLinkModelsTest` | Serialización request/response |

### Integration tests

| Test | Verifica |
|------|----------|
| `TehLinkClientMockTransportTest` | ping → get_info → run_action |
| `TehLinkOtaUploaderTest` | Chunking + SHA256 handshake |
| `DeviceConnectionManagerTest` | Detección XIBALBA vs UNKNOWN |

**Mock:** `MockTransport` con fixtures NDJSON en `src/test/resources/tehlink/`.

---

## 3. Core — Device profiles

| Test | Input | Expected |
|------|-------|----------|
| `detectProfile_xibalba` | Mock responde `ping` OK | `FirmwareProfile.XIBALBA` |
| `detectProfile_stockBruce` | Sin respuesta TEH-Link | `FirmwareProfile.UNKNOWN` |
| `detectProfile_timeout` | Timeout 3 s | `UNKNOWN`, UI warning |
| `usbReconnect` | Desconectar / reconectar USB | Auto-reconnect + re-pair opcional |

---

## 4. Engine — Workflows

v1.0.0: macro engine y scripting.

| Test | Verifica |
|------|----------|
| `MacroEngineTest.executeSequence` | Pasos TEH-Link en orden |
| `MacroEngineTest.abortOnError` | Stop en respuesta `ok:false` |
| `ScriptRepositoryTest.builtinScripts` | Scripts embebidos cargables |
| `ScriptExplorerViewModelTest` | Estado UI durante ejecución |

**Future (Phase 2):** parser `.ewf`, workflow DAG, retry policies.

---

## 5. UI — Radar y dashboard

### Unit / ViewModel

| Test | Verifica |
|------|----------|
| `DashboardViewModelTest.linkState` | OFFLINE → LINK transitions |
| `DashboardViewModelTest.hardeningFlags` | 6 flags mapeados a UI |
| `DashboardViewModelTest.otaBanner` | Banner cuando release > device version |
| `ConsoleViewModelTest.history` | Historial comandos ↑↓ |

### UI / Screenshot (opcional)

- Dashboard con perfil XIBALBA mock
- Radar con actividad RF simulada
- Terminal con chip rápido `ping`

**Herramienta:** Compose UI tests (`createComposeRule`).

---

## 6. Data — Export y backup

| Test | Verifica |
|------|----------|
| `ExportHelperTest.signalsCsv` | CSV RFC4180 |
| `SessionReportGeneratorTest.fieldReport` | Informe wardriving |
| `BackupManagerTest.roundTrip` | Export → import sin pérdida |
| `EmbedDatabaseTest.migration` | Migraciones Room |
| `RepositoriesTest.crud` | CRUD señales, macros, perfiles |

**Fixture DB:** in-memory Room + SQLCipher test key.

---

## 7. Flash / OTA

| Test | Verifica |
|------|----------|
| `FirmwareImageAnalyzerTest` | Identifica merged bin Xibalba @ 0x0 |
| `FirmwareRepositoryTest.catalog` | Parse releases GitHub |
| `FirmwareCatalogTest.embeddedAsset` | Asset v1.0.0 presente |
| `SlipEncoderTest` | Codificación SLIP esptool |

---

## 8. Integración end-to-end (manual / CI device farm)

Checklist en dispositivo real (T-Embed + teléfono API 31+):

- [ ] USB OTG connect + permiso
- [ ] TEH-Link pair (GPIO6)
- [ ] Dashboard XIBALBA
- [ ] Captura Sub-GHz 15 s
- [ ] Terminal `ping` / `get_status`
- [ ] OTA USB (misma versión → rollback test)
- [ ] Flash USB recovery
- [ ] Export backup JSON
- [ ] Widget TX favorito

---

## 9. Ejecución

```bash
# Unit tests
./gradlew test

# Instrumented (API 31+ device)
./gradlew connectedAndroidTest

# Cobertura (opcional)
./gradlew jacocoTestReport
```

---

## 10. Criterios de release v1.0.0

| Criterio | Umbral |
|----------|--------|
| Unit tests Core TEH-Link | 100% pass |
| Device profile detection | 100% pass |
| Zero crashes en smoke E2E | Obligatorio |
| OTA SHA256 verify | Obligatorio en hardware |
| P0 bugs abiertos | 0 |

---

*Testing plan · EmbedSuite v1.0.0*
