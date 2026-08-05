# EMBED SUITE — Manual de Usuario

**Companion Android para LilyGO T-Embed CC1101 Plus · Firmware Xibalba (TEH-Link)**  
Versión documentada: **4.0.7** · GIOSÁNBLAS + Cursor

> 📘 **Fuente de verdad in-app:** Acerca de → Manual interactivo (`assets/manual/index.html`).  
> Este archivo en `docs/` es un espejo resumido para revisión externa.

---

## 1. Hardware y conexión

| Componente | Función |
|------------|---------|
| **Teléfono Android** | UI, GPS wardriving, escaneo WiFi/BLE del teléfono, biblioteca local |
| **T-Embed CC1101 Plus** | ESP32-S3 + CC1101 Sub-GHz + PN532 NFC + IR + SD |
| **Firmware Xibalba** | Protocolo **TEH-Link** (JSON NDJSON por USB CDC) |

### Transportes

| Transporte | Estado | Uso |
|----------|--------|-----|
| **USB OTG** | **Recomendado / prioritario** | Uso diario, captura, TX, OTA |
| **WiFi TEH-Link** | Experimental | Solo pruebas; la app avisa en UI |
| **BLE TEH-Link** | Experimental | Solo pruebas; preferir USB |

Al arrancar y al auto-reconnect, la app **prioriza USB**.

**Emparejamiento TEH-Link:** mantén pulsado el botón lateral del T-Embed ~2 s (ventana ~120 s).

---

## 2. Navegación

| Tab | Función |
|-----|---------|
| INICIO | Dashboard, modo campo, favoritos TX, OTA banner |
| RF | Captura TEH-Link, biblioteca ★, análisis |
| WiFi | Scanner del **teléfono** + WAR-DRIVE (con aviso legal) |
| NFC | NFC/IR vía plugins `nfc_toolkit` / `ir_toolkit` |
| CLI | Consola JSON TEH-Link |
| AI | LOCAL / Gemini / Ollama (auto-ejecutar OFF por defecto) |
| Tools | Mapas, flash OTA USB, Link Debug, conexión |

---

## 3. Sub-GHz

- **Captura:** `capture_start` (USB).
- **TX / Replay:** `subghz_tx` / `subghz_replay` (requiere LINK + Xibalba).
- **Espectro / waterfall FFT:** aún **no** en TEH-Link → la app muestra **telemetría de captura** (paquetes RX / tiempo restante) vía `get_action_state`.

---

## 4. NFC / IR / Wardriving / OTA

- NFC/IR: plugins TEH-Link (capabilities firmware).
- Wardriving: plugin + GPS del teléfono.
- **OTA:** solo **USB** TEH-Link + catálogo Xibalba / custom `.bin` con disclaimer.

---

## 5. Widget

- RX 15s / TX ★ favorito: requieren app viva + LINK + token anti-broadcast.
- Preferible USB conectado.

---

## 6. Uso legal

Solo investigación autorizada / tus dispositivos. No interferir con comunicaciones ajenas.  
Wardriving y TX RF pueden estar regulados en tu jurisdicción. Política de privacidad in-app (Acerca de).
