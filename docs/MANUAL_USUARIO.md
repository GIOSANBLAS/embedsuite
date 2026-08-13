# EmbedSuite — Manual de usuario

**Companion Android · LilyGO T-Embed CC1101 Plus · Firmware Xibalba · TEH-Link v3**

Versión app: **2.2.0** · Firmware: **Xibalba v0.20.0** · GIOSANBLAS

Manual operativo para pentesters, red teamers e investigadores de seguridad. Uso exclusivo en entornos autorizados.

> Manual interactivo in-app: **Ajustes → Acerca de → Manual** (`assets/manual/index.html`).

---

## 1. Requisitos

- Teléfono **Android 10 o superior (API 29+)**
- **USB OTG** (transporte prioritario)
- **LilyGO T-Embed CC1101 Plus** con firmware **[Xibalba v0.20.0](https://github.com/GIOSANBLAS/xibalba-bruce)**
- Binario merged: `Bruce-lilygo-t-embed-cc1101.bin` / `xibalba-t-embed-cc1101.bin` @ **0x0**

Detalle de pines y flasheo: [GUIA_HARDWARE.md](GUIA_HARDWARE.md)

---

## 2. Primer arranque

1. Instala EmbedSuite v2.2.0.
2. Conecta T-Embed por USB OTG.
3. Concede permiso USB.
4. Mantén pulsado el botón lateral del T-Embed (**GPIO6**) ~2 s para **emparejar TEH-Link**.
5. Verifica en Dashboard: perfil **XIBALBA**, estado **LINK**, batería y SD.

Si aparece **UNKNOWN**, flashea Xibalba desde **Tools → Firmware**.

Idioma: Ajustes → Español / English / 中文 (el sistema del teléfono si eliges «Sistema»).

---

## 3. Dashboard y radar

El dashboard centraliza el estado operativo:

| Elemento | Función |
|----------|---------|
| **Estado de enlace** | USB / LINK / batería (BQ27220) / heap / `sd_status` |
| **Radar** | Vista rápida de actividad RF y plugins |
| **Hardening** | TWDT, BOD, Secure Boot, Flash/NVS encryption, stack canaries |
| **OTA banner** | Actualización Xibalba disponible |
| **Coredump** | Alerta tras reset anormal; opción de borrado |

---

## 4. Navegación

| Tab | Función |
|-----|---------|
| **INICIO** | Dashboard, favoritos TX, hardening |
| **RF** | Captura Sub-GHz, biblioteca, replay |
| **WiFi** | Scanner del teléfono, wardriving |
| **NFC** | NFC / IR vía plugins TEH-Link |
| **CLI** | Terminal JSON TEH-Link |
| **SCRIPTS** | Presets que ejecutan `run_action` en el firmware (ver §5) |
| **Ops** | Workflows, Autopilot, config, fleet |
| **AI** | Asistente local / remoto (auto-ejecutar OFF por defecto) |
| **Tools** | Mapas, flash OTA, Link Debug, conexión, microSD |

---

## 5. Scripts (funcional)

La pestaña **SCRIPTS** no es un editor JS: es un catálogo de **acciones TEH-Link** (`plugin_id` + `action` + params) que EmbedSuite envía al T-Embed cuando hay LINK y token.

- Evil Portal, beacon/BLE spam, Sub-GHz, NFC clone, BadUSB, Mousejack, etc.
- Las herramientas ofensivas piden **Modo Auditoría** (Ajustes → Seguridad).
- Cada fila muestra plugin, acción y resultado (`ok` / error del firmware).
- Macros JSON de varias líneas siguen en **Tools → Macros**.

Si un script falla con `unknown_cmd` o `missing_plugin_id`, actualiza el firmware a Xibalba v0.20.0.

---

## 6. Terminal TEH-Link

Consola JSON línea a línea. La app **solo parsea líneas que empiezan por `{`**. Los logs USB `[INFO] [SD] …` se muestran en Link Debug y no rompen el protocolo.

```json
{"cmd":"ping"}
{"cmd":"get_info"}
{"cmd":"list_files","params":{"path":"/xibalba/subghz"}}
{"cmd":"download_file","params":{"path":"/xibalba/subghz/captura.sub"}}
{"cmd":"run_action","plugin_id":"subghz_analyzer","action":"capture_start","params":{"seconds":30}}
```

`get_info` incluye `hardware`, `firmware`, `battery.voltage` / `percentage`, `sd_status`.

---

## 7. MicroSD del T-Embed

Árbol creado al boot:

```
/xibalba/subghz  /xibalba/ir  /xibalba/wardrive  /xibalba/logs  /xibalba/gps
```

En **Ajustes → SD STORAGE**:

- Estado de la tarjeta
- Listar `/xibalba`
- Descargar un archivo de `/xibalba/subghz` (chunks Base64 de 512 B)

---

## 8. Sub-GHz

| Acción | TEH-Link |
|--------|----------|
| Captura RX | `capture_start` / `capture_stop` → `.sub` en `/xibalba/subghz` |
| TX RAW | `subghz_tx` (requiere `confirm:true`) |
| Replay | `subghz_replay` (archivo en SD del dispositivo) |

Sin microSD montada, la captura remota responde `SD card not mounted`.

---

## 9. WiFi, BLE, NFC, IR

Plugins firmware vía `run_action`:

- `wifi_toolkit` — scan, probes
- `ble_toolkit` — scan BLE del dispositivo
- `nfc_toolkit` — read / emulate (PN532 I2C SDA=8 SCL=18)
- `ir_toolkit` — send / rx

**Modo Auditoría** desbloquea herramientas ofensivas. Pairing: GPIO6.

---

## 10. OTA de firmware

1. **Tools → Firmware**.
2. Selecciona release Xibalba o importa `.bin`.
3. OTA USB vía TEH-Link o Flash USB (recovery: Encoder + RST).
4. Confirma **`sha256_verified=true`** antes de reiniciar.

Solo USB. No reinicies si la verificación falla.

---

## 11. Hardening, wardriving, backup

Panel de flags (TWDT, BOD, Secure Boot, cifrado flash/NVS, canaries). Wardriving: plugin + GPS del teléfono. Backup JSON/CSV local; DB cifrada (SQLCipher).

---

## 12. Uso legal

Solo en sistemas y redes propias o con autorización escrita. Eres responsable del cumplimiento de leyes locales sobre radiofrecuencia, interceptación y acceso a sistemas.

EmbedSuite no está afiliado a LilyGO ni BruceDevices.

---

## 13. Soporte

| Recurso | URL |
|---------|-----|
| App | https://github.com/GIOSANBLAS/embedsuite |
| Firmware | https://github.com/GIOSANBLAS/xibalba-bruce |
| Hardware | [GUIA_HARDWARE.md](GUIA_HARDWARE.md) |

---

*Manual de usuario · EmbedSuite v2.2.0 · Xibalba v0.20.0*
