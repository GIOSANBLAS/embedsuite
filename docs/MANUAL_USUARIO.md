# EmbedSuite — Manual de usuario

**Companion Android · LilyGO T-Embed CC1101 Plus · Firmware Xibalba · TEH-Link v3**

Versión: **1.0.0** · GIOSANBLAS

Manual operativo para pentesters, red teamers e investigadores de seguridad. Uso exclusivo en entornos autorizados.

> Manual interactivo in-app: **Ajustes → Acerca de → Manual** (`assets/manual/index.html`).

---

## 1. Requisitos

- Teléfono **Android 12 o superior (API 31+)**
- **USB OTG** (transporte prioritario)
- **LilyGO T-Embed CC1101 Plus** con firmware **[Xibalba v1.0.0](https://github.com/GIOSANBLAS/xibalba-bruce)**
- Binario: `xibalba-t-embed-cc1101.bin` @ 0x0

Detalle hardware: [GUIA_HARDWARE.md](GUIA_HARDWARE.md)

---

## 2. Primer arranque

1. Instala EmbedSuite v1.0.0.
2. Conecta T-Embed por USB OTG.
3. Concede permiso USB.
4. Mantén pulsado el botón lateral del T-Embed ~2 s para **emparejar TEH-Link**.
5. Verifica en Dashboard: perfil **XIBALBA**, estado **LINK**.

Si aparece **UNKNOWN**, flashea Xibalba desde **Tools → Firmware**.

---

## 3. Dashboard y radar

El dashboard centraliza el estado operativo:

| Elemento | Función |
|----------|---------|
| **Estado de enlace** | USB / LINK / batería / heap |
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
| **AI** | Asistente local / remoto (auto-ejecutar OFF por defecto) |
| **Tools** | Mapas, flash OTA, Link Debug, conexión |

---

## 5. Terminal TEH-Link

Consola JSON línea a línea. Comandos frecuentes:

```json
{"cmd":"ping"}
{"cmd":"get_info"}
{"cmd":"get_status"}
{"cmd":"run_action","plugin_id":"subghz_analyzer","action":"capture_start","params":{"seconds":30}}
```

Chips rápidos en UI: ping, get_info, acciones de plugins.

Historial ↑↓. Macros ejecutan secuencias TEH-Link almacenadas localmente.

---

## 6. Sub-GHz

| Acción | TEH-Link |
|--------|----------|
| Captura RX | `capture_start` / `capture_stop` |
| TX RAW | `subghz_tx` (requiere `confirm:true`) |
| Replay | `subghz_replay` (archivo en SD del dispositivo) |

Biblioteca local en el teléfono para señales favoritas. TX requiere enlace activo y confirmación explícita.

---

## 7. WiFi, BLE, NFC, IR

Plugins firmware vía `run_action`:

- `wifi_toolkit` — scan, probes
- `ble_toolkit` — scan BLE del dispositivo
- `nfc_toolkit` — read / emulate
- `ir_toolkit` — send / rx

**Modo Auditoría** (Ajustes → Seguridad) desbloquea herramientas ofensivas. Cada plugin verifica permiso en firmware.

---

## 8. OTA de firmware

1. **Tools → Firmware**.
2. Selecciona release **Xibalba v1.0.0** o importa `.bin`.
3. OTA USB vía TEH-Link o Flash USB (recovery).
4. Confirma **`sha256_verified=true`** antes de reiniciar.

Solo USB. No reinicies si la verificación falla — repite flash.

---

## 9. Hardening

Panel de 6 flags reportados por Xibalba:

- Task Watchdog (TWDT)
- Brown-out detector (BOD)
- Secure Boot
- Flash encryption
- NVS encryption
- Stack canaries / heap poisoning

Útil para auditorías de superficie de ataque del firmware en campo.

---

## 10. Wardriving y mapas

- Plugin `wardriving` + GPS del teléfono.
- MapTools: heatmap WiFi/BLE, Link Debug (200 líneas serial).
- Export de sesión vía Data layer.

---

## 11. Backup y export

- Backup JSON: señales, IR, macros, perfiles.
- Export CSV / informes de sesión.
- Base de datos cifrada (SQLCipher).

---

## 12. Widget de escritorio

Acceso rápido a captura RX 15 s y TX favorito. Requiere app activa, LINK y token anti-broadcast. Preferir USB conectado.

---

## 13. Uso legal

Solo en sistemas y redes propias o con autorización escrita. Eres responsable del cumplimiento de leyes locales sobre radiofrecuencia, interceptación y acceso a sistemas.

EmbedSuite no está afiliado a LilyGO ni BruceDevices.

---

## 14. Soporte

| Recurso | URL |
|---------|-----|
| App | https://github.com/GIOSANBLAS/embedsuite |
| Firmware | https://github.com/GIOSANBLAS/xibalba-bruce |
| Hardware | [GUIA_HARDWARE.md](GUIA_HARDWARE.md) |

---

*Manual de usuario · EmbedSuite v1.0.0*
