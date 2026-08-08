# 🚀 GUÍA DÍA 1 — Puesta en marcha de EmbedSuite + Xibalba

**EmbedSuite v4.5.0 · Firmware Xibalba-0.19.0 Maya · T-Embed CC1101 Plus**

Esta guía te lleva desde cero hasta tener tu T-Embed funcionando con la app en menos de una hora.

---

## 1. Qué necesitas

| Elemento | Detalle |
|----------|---------|
| **Teléfono Android** | Android 8.0 (API 26) o superior, con soporte USB OTG |
| **LilyGO T-Embed CC1101 Plus** | ESP32-S3 + CC1101 + PN532 + IR + microSD |
| **Cable USB-C** | Con **datos + OTG** (no solo carga). Ver [GUIA_HARDWARE](./GUIA_HARDWARE.md) |
| **microSD** | FAT32, máximo 32 GB (recomendado 8–16 GB) — recomendada para wardriving y biblioteca RF |
| **Firmware objetivo** | **Xibalba-0.19.0 Maya** (runtime Bruce + TEH-Link v3, UI español) |
| **EmbedSuite APK** | v4.5.0 — `./gradlew assembleDebug` o release |

---

## 2. Flasheo del firmware Xibalba-0.19.0 Maya

> **Versión actual:** Xibalba-0.19.0 Maya — runtime [Bruce](https://github.com/pr3y/Bruce) + TEH-Link v3 USB, UI español Maya/cyber, simbiosis EmbedSuite para CC1101 Plus.

### Opción A — Desde el PC (primera vez o recuperación)

1. Obtén el `.bin` desde el release de [xibalba-bruce](https://github.com/GIOSANBLAS/xibalba-bruce/releases/tag/v0.19.0): **`xibalba-t-embed-cc1101.bin`** (merged @ 0x0).
2. **Instalación limpia (recomendada):**
   ```bash
   esptool.py --chip esp32s3 -p COMx -b 460800 write_flash 0x0 xibalba-t-embed-cc1101.bin
   ```
3. Modo download (T-Embed CC1101): **Encoder + RST** (igual que [Bruce Flasher](https://bruce.computer/flasher)).
4. Inserta microSD FAT32 antes del primer arranque completo.

> **Nota:** El binario oficial es **merged @ 0x0** (bootloader + particiones + app). No uses el flasheo @ 0x10000 salvo que sepas que ya tienes particiones compatibles.

### Opción B — Desde la app (OTA USB o Flash USB)

1. Conecta el T-Embed por **USB OTG** al teléfono.
2. Abre **Map & Tools** → sección **Firmware / OTA**.
3. La app carga automáticamente el catálogo embebido; debe aparecer **Xibalba-0.19.0 Maya ★ RECOMENDADO**.
4. Si GitHub no responde (repo privado / sin red): pulsa **Importar .bin custom** y selecciona `xibalba-t-embed-cc1101.bin` descargado en PC.
5. **OTA TEH-Link:** requiere Xibalba ya operativo + SHA256 verificado → pulsa **Flash OTA**.
6. **Flash USB (bootloader):** mantén **Encoder + RST** al conectar (como Bruce) → pulsa **Flash USB**.
   - La app detecta automáticamente **merged @ 0x0**.
   - Instalación limpia desde stock Bruce u otro firmware: usa el **merged .bin** del release v0.19.0.
7. Tras OTA exitosa: espera **✅ SHA256 VERIFIED** antes de reiniciar.

> ⚠️ **v0.18.0 Iron Shield** y builds anteriores (Spark, Glow) aparecen como **[LEGACY]** en el selector — solo para rollback explícito, no recomendados.

---

## 3. Primeros pasos con el Dashboard

1. **Conecta el T-Embed** por USB OTG.
2. Abre **EmbedSuite**. La app:
   - Detecta el firmware: **XIBALBA** si responde TEH-Link (`ping` / `proto: teh-link`); **UNKNOWN** si es stock Bruce sin TEH-Link.
   - Inicia emparejamiento TEH-Link (mantén botón lateral GPIO6 ~2 s).
   - Muestra **LINK** en verde.
3. Revisa en el **Dashboard**:
   - **LINK / SISTEMA:** uptime, memoria, batería, versión firmware (debe ser **0.19.0+**).
   - **HARDENING 0.19.0+:** TWDT, BOD, Secure Boot, Flash/NVS Encryption, Stack Canaries.
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
| TEH-Link USB + pairing | ✅ CC1101 Plus | v0.19.0 Maya | USB OTG | Prioritario |
| Sub-GHz RX/TX/Replay | ✅ CC1101 | v0.19.0 | TEH-Link | 315/433/868/915 MHz |
| WiFi scan (T-Embed) | ✅ ESP32-S3 | v0.19.0 | TEH-Link | Menú Bruce + TEH-Link |
| BLE scan (T-Embed) | ✅ ESP32-S3 BLE | v0.19.0 | TEH-Link | Menú Bruce + TEH-Link |
| Wardriving + GPS | ✅ CC1101 + teléfono GPS | v0.19.0 | TEH-Link + Location | microSD recomendada |
| NFC read/emulate | ✅ PN532 | v0.19.0 | TEH-Link | Capability `nfc` |
| IR RX/TX | ✅ IR LED/receiver | v0.19.0 | TEH-Link | Capability `ir` |
| OTA + SHA256 verify | ✅ CC1101 Plus | v0.19.0 | TEH-Link USB | Obligatorio antes de reboot |
| UI dispositivo español | ✅ Pantalla T-Embed | v0.19.0 | — | Menú Maya/cyber en ES |
| Evil Portal / Beacon Spam | ✅ ESP32-S3 WiFi | v0.19.0 / legacy 0.18 | TEH-Link | Según capabilities del firmware |
| BLE AD Spam | ✅ ESP32-S3 BLE | v0.19.0 | TEH-Link | Modo Auditoría |
| WiFi Deauth + Probe | ✅ ESP32-S3 WiFi | v0.19.0 | TEH-Link | Modo Auditoría |
| Mousejack NRF24 | ⚠️ Requiere módulo nRF24 | v0.19.0 | TEH-Link | Según hardware |
| Sub-GHz Spectrum | ✅ CC1101 | v0.19.0 | TEH-Link | TEH-Link / menú Bruce |
| NFC Clone/Write | ✅ PN532 | v0.19.0 | TEH-Link | Modo Auditoría |
| WiFi/BLE TEH-Link | ⚠️ Experimental | v0.19.0 | WiFi/BLE | Usar USB para uso diario |
| Stock Bruce sin TEH-Link | ⚠️ Perfil UNKNOWN | — | — | Flashea Xibalba-0.19.0 Maya |

**Si el Dashboard muestra flags SIM:** el firmware no corresponde al perfil CC1101 Plus real — re-flashea **Xibalba-0.19.0 Maya**.

---

## 6. microSD

| Aspecto | Recomendación |
|---------|---------------|
| **Formato** | FAT32 (obligatorio) |
| **Tamaño máx.** | 32 GB |
| **Etiqueta** | `XIBALBA` (opcional) |
| **Uso** | Wardriving, biblioteca Sub-GHz, scripts |
| **Extracción** | Desmonta desde menú T-Embed antes de sacarla |

---

## 7. Solución de problemas

| Problema | Solución |
|----------|----------|
| Flash muestra v0.18.0 Iron Shield en lugar de v0.19.0 | Actualiza app a v4.5.0 · Abre Map & Tools (catálogo embebido) · Pulsa Buscar releases |
| Perfil UNKNOWN (stock Bruce) | Flashea `xibalba-t-embed-cc1101.bin` merged @ 0x0 desde xibalba-bruce |
| GitHub no lista releases | Importa `.bin` custom v0.19.0 · Catálogo embebido sigue mostrando v0.19.0 |
| App no detecta USB | Cable solo carga → prueba OTG con datos |
| LINK rojo | Reconecta USB · Re-empareja TEH-Link (GPIO6 2 s) |
| Script ofensivo bloqueado | Activa Modo Auditoría en Ajustes |
| OTA sin SHA256 | **NO reinicies** · Re-flashea OTA o USB |
| Acción TEH-Link falla | Verifica capabilities en Dashboard · Firmware ≥ v0.19.0 Maya |

---

## 8. Siguientes pasos

- [Manual de Usuario](./MANUAL_USUARIO.md) completo.
- [Guía de Hardware](./GUIA_HARDWARE.md) — cables, OTG, checklist detallado.
- Explora **Scripts**, **Spectrum**, **NFC Clone**, **Wardriving**.
- Repo firmware: [GIOSANBLAS/xibalba-bruce](https://github.com/GIOSANBLAS/xibalba-bruce)

---

*Documentación v4.5.0 · EmbedSuite · GIOSÁNBLAS*
