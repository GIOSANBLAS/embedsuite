# 🚀 GUÍA DÍA 1 — Puesta en marcha de EmbedSuite + Xibalba

**EmbedSuite v4.4.0 · Firmware Xibalba v0.18.0 Iron Shield · T-Embed CC1101 Plus**

Esta guía te lleva desde cero hasta tener tu T-Embed funcionando con la app en menos de una hora.

---

## 1. Qué necesitas

| Elemento | Detalle |
|----------|---------|
| **Teléfono Android** | Android 8.0 (API 26) o superior, con soporte USB OTG |
| **LilyGO T-Embed CC1101 Plus** | ESP32-S3 + CC1101 + PN532 + IR + microSD |
| **Cable USB-C** | Con **datos + OTG** (no solo carga). Ver [GUIA_HARDWARE](./GUIA_HARDWARE.md) |
| **microSD** | FAT32, máximo 32 GB (recomendado 8–16 GB) — **obligatoria** para plugins y wardriving |
| **Firmware objetivo** | **Xibalba v0.18.0 Iron Shield** (TEH-Link v3) |
| **EmbedSuite APK** | v4.4.0 — `./gradlew assembleDebug` o release |

---

## 2. Flasheo del firmware Xibalba v0.18.0

> **Versión actual:** v0.18.0 Iron Shield — incluye Evil Portal, Beacon Spam, Modo Auditoría, 5 plugins ofensivos y hardening avanzado.

### Opción A — Desde el PC (primera vez o recuperación)

1. Obtén el `.bin` desde el release de [te-embed-xibalba](https://github.com/GIOSANBLAS/te-embed-xibalba).
2. **Merged (instalación limpia, estilo Bruce Web Flasher):**
   ```bash
   esptool.py --chip esp32s3 -p COMx -b 460800 write_flash 0x0 te-embed-xibalba-merged.bin
   ```
3. **Solo app** (si ya tienes bootloader/particiones Xibalba o Bruce compatibles):
   ```bash
   esptool.py --chip esp32s3 -p COMx -b 460800 write_flash 0x10000 te-embed-xibalba.bin
   ```
4. Modo download (T-Embed CC1101): **Encoder + RST** (igual que [Bruce Flasher](https://bruce.computer/flasher)).
5. Inserta microSD FAT32 antes del primer arranque completo.

### Opción B — Desde la app (OTA USB o Flash USB)

1. Conecta el T-Embed por **USB OTG** al teléfono.
2. Abre **Map & Tools** → sección **Firmware / OTA**.
3. La app carga automáticamente el catálogo embebido; debe aparecer **v0.18.0 Iron Shield ★ RECOMENDADO**.
4. Si GitHub no responde (repo privado / sin red): pulsa **Importar .bin custom** y selecciona `te-embed-xibalba.bin` descargado en PC.
5. **OTA TEH-Link:** requiere Xibalba ya operativo + SHA256 verificado → pulsa **Flash OTA**.
6. **Flash USB (bootloader):** mantén **Encoder + RST** al conectar (como Bruce) → pulsa **Flash USB**.
   - La app detecta automáticamente **merged @ 0x0** vs **app @ 0x10000**.
   - Instalación limpia desde Bruce/otro firmware: importa el **merged .bin** del release.
7. Tras OTA exitosa: espera **✅ SHA256 VERIFIED** antes de reiniciar.

> ⚠️ Builds anteriores (v0.17.1 Spark, v0.16.x) aparecen como **[BETA]** en el selector — no las uses salvo rollback explícito.

---

## 3. Primeros pasos con el Dashboard

1. **Conecta el T-Embed** por USB OTG.
2. Abre **EmbedSuite**. La app:
   - Detecta el firmware (Xibalba / Bruce / VARSYS / UNKNOWN).
   - Inicia emparejamiento TEH-Link (mantén botón lateral GPIO6 ~2 s).
   - Muestra **LINK** en verde.
3. Revisa en el **Dashboard**:
   - **LINK / SISTEMA:** uptime, memoria, batería, versión firmware (debe ser **0.18.0+**).
   - **HARDENING 0.18.0+:** TWDT, BOD, Secure Boot, Flash/NVS Encryption, Stack Canaries.
   - **XIBALBA PLUGINS:** lista dinámica desde firmware.
   - **Última señal:** capturas Sub-GHz recientes.
4. Prueba captura básica:
   - Pulsa **RX 15s** en Acciones rápidas.
   - Apunta hacia un mando 433 MHz (solo en entorno autorizado).
   - La señal aparece en **Última señal** y en la biblioteca RF.

---

## 4. Modo Auditoría (herramientas ofensivas)

> 🔒 BLE Spam, WiFi Deauth, Mousejack, Spectrum y NFC Clone requieren **Modo Auditoría** activo.

1. **Ajustes → Seguridad → Modo Auditoría → ON**.
2. Vuelve al **Dashboard** → sección **OFENSIVE TOOLS · AUDIT MODE**.
3. También disponible en **Scripts** (categorías BLE_SPAM, WIFI_OFFENSIVE, etc.).
4. Usa solo en tus dispositivos o con autorización explícita.

**Gating doble:**
- App: bloquea scripts ofensivos si Modo Auditoría está OFF.
- Firmware: `settings_plugin_is_allowed` por plugin TX.

---

## 5. Compatibilidad CC1101 Plus — qué funciona en hardware real

| Función | Hardware | Firmware mín. | Transporte | Notas |
|---------|----------|---------------|------------|-------|
| TEH-Link USB + pairing | ✅ CC1101 Plus | v0.16+ | USB OTG | Prioritario |
| Sub-GHz RX/TX/Replay | ✅ CC1101 | v0.16+ | TEH-Link | 315/433/868/915 MHz |
| WiFi scan (T-Embed) | ✅ ESP32-S3 | v0.16+ | TEH-Link | Plugin `wifi_toolkit` |
| BLE scan (T-Embed) | ✅ ESP32-S3 BLE | v0.16+ | TEH-Link | Plugin `ble_toolkit` |
| Wardriving + GPS | ✅ CC1101 + teléfono GPS | v0.16+ | TEH-Link + Location | microSD recomendada |
| NFC read/emulate | ✅ PN532 | v0.15+ | TEH-Link | Capability `nfc` |
| IR RX/TX | ✅ IR LED/receiver | v0.15+ | TEH-Link | Capability `ir` |
| OTA + SHA256 verify | ✅ CC1101 Plus | v0.17.1+ | TEH-Link USB | Obligatorio antes de reboot |
| Evil Portal | ✅ ESP32-S3 WiFi | **v0.18.0** | TEH-Link | Plugin `evil_portal` + microSD |
| Beacon Spam | ✅ ESP32-S3 WiFi | **v0.18.0** | TEH-Link | Plugin `beacon_spam` |
| BLE AD Spam | ✅ ESP32-S3 BLE | **v0.18.0** | TEH-Link | Plugin `ble_ad_spam` + Auditoría |
| WiFi Deauth + Probe | ✅ ESP32-S3 WiFi | **v0.18.0** | TEH-Link | Plugin `wifi_offensive` + Auditoría |
| Mousejack NRF24 | ⚠️ Requiere módulo nRF24 | **v0.18.0** | TEH-Link | Plugin `mousejack` |
| Sub-GHz Spectrum | ✅ CC1101 | **v0.18.0** | TEH-Link | Plugin `subghz_tools` |
| NFC Clone/Write | ✅ PN532 | **v0.18.0** | TEH-Link | Plugin `nfc_clone` + Mifare/NTAG |
| WiFi/BLE TEH-Link | ⚠️ Experimental | v0.17+ | WiFi/BLE | Usar USB para uso diario |
| Espectro waterfall live | ❌ Pendiente firmware | — | — | Stub app listo |

**Si el Dashboard muestra flags SIM:** el firmware no corresponde al perfil CC1101 Plus real — re-flashea v0.18.0 Iron Shield.

---

## 6. microSD

| Aspecto | Recomendación |
|---------|---------------|
| **Formato** | FAT32 (obligatorio) |
| **Tamaño máx.** | 32 GB |
| **Etiqueta** | `XIBALBA` (opcional) |
| **Uso** | Plugins, wardriving, biblioteca Sub-GHz, Evil Portal templates |
| **Extracción** | Desmonta desde menú T-Embed antes de sacarla |

---

## 7. Solución de problemas

| Problema | Solución |
|----------|----------|
| Flash muestra v0.17.1 en lugar de v0.18.0 | Actualiza app a v4.4.0 · Abre Map & Tools (catálogo embebido) · Pulsa Buscar releases |
| GitHub no lista releases | Importa `.bin` custom v0.18.0 · Catálogo embebido sigue mostrando v0.18.0 |
| App no detecta USB | Cable solo carga → prueba OTG con datos |
| LINK rojo | Reconecta USB · Re-empareja TEH-Link (GPIO6 2 s) |
| Script ofensivo bloqueado | Activa Modo Auditoría en Ajustes |
| OTA sin SHA256 | **NO reinicies** · Re-flashea OTA o USB |
| Acción TEH-Link falla | Verifica plugin en Dashboard → XIBALBA PLUGINS · Firmware ≥ v0.18.0 para tools ofensivas |

---

## 8. Siguientes pasos

- [Manual de Usuario](./MANUAL_USUARIO.md) completo.
- [Guía de Hardware](./GUIA_HARDWARE.md) — cables, OTG, checklist detallado.
- Explora **Scripts**, **Spectrum**, **NFC Clone**, **Wardriving**.
- Canal GIOSÁNBLAS para firmware y releases.

---

*Documentación v4.4.0 · EmbedSuite · GIOSÁNBLAS*
