# Guía de hardware — T-Embed CC1101 Plus

**EmbedSuite v1.0.0 · Firmware Xibalba v1.0.0 · GIOSANBLAS**

Requisitos y procedimientos para operar EmbedSuite con el LilyGO T-Embed CC1101 Plus.

---

## 1. Requisitos

| Componente | Especificación |
|------------|----------------|
| **Teléfono** | Android 12 o superior (API 31+) |
| **Cable / adaptador** | USB OTG (datos, no solo carga) |
| **Dispositivo** | LilyGO T-Embed CC1101 Plus |
| **Firmware** | [Xibalba v1.0.0](https://github.com/GIOSANBLAS/xibalba-bruce) |
| **Binario** | `xibalba-t-embed-cc1101.bin` — merged @ offset **0x0** |
| **microSD** | FAT32 recomendada (wardriving, biblioteca RF) |

---

## 2. Especificaciones T-Embed CC1101 Plus

| Subsistema | Detalle |
|------------|---------|
| SoC | ESP32-S3 (dual-core 240 MHz, WiFi + BLE 5) |
| Flash | 16 MB (dual OTA slot) |
| PSRAM | 8 MB |
| Pantalla | 1.9" TFT 170×320 |
| Sub-GHz | CC1101 (315 / 433 / 868 / 915 MHz) |
| NFC | PN532 (ISO14443A/B, Mifare Classic) |
| IR | TX + RX |
| Batería | LiPo 1S, PMIC BQ25896 |
| Conector | USB-C (OTG) |
| Botón lateral | GPIO6 — pairing TEH-Link |

---

## 3. Firmware Xibalba

Xibalba es el firmware oficial para EmbedSuite. Deriva de [Bruce](https://github.com/BruceDevices/firmware) (AGPL-3.0) con parche **TEH-Link v3** e interfaz Maya/cyber en español.

### Descarga

Releases: https://github.com/GIOSANBLAS/xibalba-bruce/releases

Asset: **`xibalba-t-embed-cc1101.bin`**

### Flasheo desde EmbedSuite (recomendado)

1. Instala EmbedSuite v1.0.0 en el teléfono.
2. Conecta T-Embed por USB OTG.
3. **Tools → Map & Tools → Firmware**.
4. Selecciona **Xibalba v1.0.0**.
5. **OTA USB** (si ya tienes Xibalba) o **Flash USB** (primera vez / recovery).

### Flasheo desde PC (esptool)

```bash
esptool.py --chip esp32s3 -p COMx -b 460800 write_flash 0x0 xibalba-t-embed-cc1101.bin
```

En Linux/macOS sustituye `COMx` por `/dev/ttyACM0` o equivalente.

### Perfil en la app

| Firmware | Perfil EmbedSuite |
|----------|-------------------|
| Xibalba (TEH-Link v3) | **XIBALBA** — simbiosis completa |
| Stock Bruce sin TEH-Link | **UNKNOWN** — funcionalidad limitada |

---

## 4. Conexión USB OTG

1. Conecta cable OTG al teléfono y USB-C al T-Embed.
2. Acepta permiso USB cuando la app lo solicite.
3. EmbedSuite prioriza USB sobre WiFi/BLE.
4. **Emparejamiento TEH-Link:** mantén pulsado botón lateral ~2 s (ventana 120 s).
5. Dashboard debe mostrar perfil **XIBALBA** y estado **LINK**.

### Checklist de conexión

- [ ] Cable OTG con líneas de datos
- [ ] T-Embed encendido (batería o USB)
- [ ] Firmware Xibalba flasheado @ 0x0
- [ ] Permiso USB concedido
- [ ] TEH-Link emparejado (long-press GPIO6)

---

## 5. microSD

| Uso | Ruta típica |
|-----|-------------|
| Replay Sub-GHz | `/subghz/` |
| Wardriving | `/wardriving/` |
| Scripts | `/scripts/` |

Formatea en FAT32. Expulsa de forma segura antes de retirar.

---

## 6. Modo recovery (Flash USB)

Si OTA falla o el dispositivo no arranca:

1. Mantén **Encoder** + pulsa **RST** (entrada bootloader).
2. Conecta USB al teléfono.
3. EmbedSuite → Flash USB con `xibalba-t-embed-cc1101.bin`.
4. Espera verificación SHA256 antes de desconectar.

---

## 7. Solución de problemas

| Síntoma | Acción |
|---------|--------|
| Perfil UNKNOWN | Flashea Xibalba — stock Bruce no incluye TEH-Link |
| OFFLINE / timeout | Reconectar USB; re-emparejar TEH-Link; revisar Link Debug |
| Sin permiso USB | Ajustes Android → Apps → EmbedSuite → permisos USB |
| OTA falla | Verificar emparejamiento, cable datos, espacio flash; usar Flash USB |
| CC1101 no responde | Revisar antena; confirmar hardware CC1101 Plus (no variante sin radio) |

---

## 8. Enlaces

| Recurso | URL |
|---------|-----|
| Firmware Xibalba | https://github.com/GIOSANBLAS/xibalba-bruce |
| App EmbedSuite | https://github.com/GIOSANBLAS/embedsuite |
| Manual de usuario | [MANUAL_USUARIO.md](MANUAL_USUARIO.md) |

---

*Guía de hardware · EmbedSuite v1.0.0*
