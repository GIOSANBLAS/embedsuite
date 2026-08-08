# 🚀 GUÍA DÍA 1 — Puesta en marcha de EmbedSuite + Xibalba

**EmbedSuite v4.4.0 · Firmware Xibalba v0.18.0 / 0.17+ · T-Embed CC1101 Plus**

Esta guía te lleva desde cero hasta tener tu T-Embed funcionando con la app en menos de una hora.

---

## 1. Qué necesitas

| Elemento | Detalle |
|----------|---------|
| **Teléfono Android** | Android 8.0 (API 26) o superior, con soporte USB OTG |
| **LilyGO T-Embed CC1101 Plus** | ESP32-S3 + CC1101 + PN532 + IR + microSD |
| **Cable USB-C** | Con **datos + OTG** (no solo carga). Ver [GUIA_HARDWARE](./GUIA_HARDWARE.md) |
| **microSD** | FAT32, máximo 32 GB (recomendado 8-16 GB) |
| **EmbedSuite APK** | Compilado con `./gradlew assembleDebug` o release |

---

## 2. Flasheo del firmware Xibalba

### Opción A — Desde el PC (recomendada la primera vez)

1. Clona el firmware:
   ```bash
   git clone https://github.com/GIOSANBLAS/te-embed-xibalba.git
   cd te-embed-xibalba
   ```
2. Compila y flashea:
   ```bash
   idf.py build
   idf.py -p COMx flash monitor
   ```
3. Verifica que el dispositivo responda al puerto serie.

### Opción B — Desde la app (OTA USB)

> ⚠️ Solo **después** de que el T-Embed tenga ya un firmware Xibalba operativo vía USB.

1. Conecta el T-Embed por **USB OTG** al teléfono.
2. Abre la app → **Tools → Firmware / OTA**.
3. Pulsa **Buscar releases Xibalba** (o selecciona un `.bin` custom).
4. Acepta el disclaimer y pulsa **Flash OTA (TEH-Link)**.
5. Espera a que la app muestre **✅ SHA256 VERIFIED**.
6. **Reinicia el T-Embed** para aplicar la actualización.

---

## 3. Primeros pasos con el dashboard

1. **Conecta el T-Embed** por USB OTG al teléfono.
2. Abre **EmbedSuite**. Automáticamente la app:
   - Detecta el firmware (Xibalba / Bruce / VARSYS).
   - Inicia el emparejamiento TEH-Link (mantén pulsado el botón lateral ~2 s).
   - Muestra el estado LINK en verde.
3. Revisa en el **Dashboard**:
   - **LINK**: estado de la conexión (USB / WiFi / BLE).
   - **SISTEMA**: uptime, memoria, batería, firmware.
   - **HARDENING**: 6 flags de seguridad (TWDT, BOD, Secure Boot, Flash Enc, NVS Enc, Canarios).
   - **Última señal**: capturas Sub-GHz recientes.
4. Prueba una captura:
   - Pulsa **RX 15s** en Acciones rápidas.
   - Apunta el T-Embed hacia un mando de garage o puerta (solo en entorno autorizado).
   - La señal aparecerá en **Última señal** y en la biblioteca.

---

## 4. Activación del Modo Auditoría

> 🔒 Las herramientas ofensivas (BLE Spam, WiFi Deauth, Mousejack, etc.) requieren **Modo Auditoría**.

1. Abre **Ajustes → Seguridad**.
2. Activa **Modo Auditoría**.
3. Vuelve al **Dashboard**; verás la sección **OFENSIVE TOOLS · AUDIT MODE**.
4. Usa responsablemente: **solo en tus dispositivos o con autorización explícita**.

**Qué hace el gating:**
- Cada plugin ofensivo verifica `settings_plugin_is_allowed` en el firmware.
- Si el modo está desactivado, la acción se bloquea y la app muestra el aviso.
- El desbloqueo es **por plugin** y se revoca al quitar el modo.

---

## 5. Consejos para la microSD

| Aspecto | Recomendación |
|---------|---------------|
| **Formato** | FAT32 (obligatorio para Xibalba) |
| **Tamaño máx.** | 32 GB (compatibilidad máxima con ESP32-S3 + FAT) |
| **Etiqueta** | `XIBALBA` (opcional pero ayuda) |
| **Estructura** | El firmware crea `/sdcard/plugins/`, `/sdcard/wardriving/` automáticamente |
| **Extracción** | Siempre desmonta desde el menú del T-Embed antes de sacarla |

---

## 6. Solución de problemas rápidos

| Problema | Solución |
|----------|----------|
| La app no detecta el T-Embed | Verifica cable OTG con datos · Prueba otro cable puerto |
| LINK en rojo / Error | Desconecta y reconecta USB · Reinicia la app |
| Pairing TEH-Link falla | Mantén pulsado el botón lateral **2 s** y reconecta |
| OTA no verifica SHA256 | **NO reinicies**. Flashea de nuevo por USB con `esptool.py` |
| SD no montada | Revisa formato FAT32 · Reinicia el T-Embed |
| Modo Auditoría no desbloquea | Reinicia la app tras activarlo · Verifica firmware 0.17+ |

---

## 7. Siguientes pasos

- Lee el [Manual de Usuario](./MANUAL_USUARIO.md) completo.
- Revisa la [Guía de Hardware](./GUIA_HARDWARE.md) para cables/OTG.
- Explora WAR-DRIVE, NFC/IR y el script explorer.
- Únete al canal de GIOSÁNBLAS para el firmware.

---

*Documentación v4.4.0 · EmbedSuite · GIOSÁNBLAS*