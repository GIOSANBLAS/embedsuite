# EMBED SUITE — Manual de Usuario

**Companion Android para LilyGO T-Embed CC1101 Plus · Firmware Bruce**  
Versión documentada: **3.8.1** · GIOSÁNBLAS + Cursor

> 📘 **Versión interactiva:** abre [`docs/manual/index.html`](manual/index.html) en tu navegador (búsqueda, índice lateral, filtros por pantalla).

---

## 1. Hardware y conexión

### 1.1 Qué es qué

| Componente | Función |
|------------|---------|
| **Teléfono Android** | UI, GPS war-driving, escaneo WiFi/BLE del teléfono, biblioteca local |
| **T-Embed CC1101 Plus** | ESP32-S3 + CC1101 Sub-GHz + PN532 NFC + IR + SD |
| **Firmware Bruce** | Interpreta comandos CLI (`subghz`, `ir`, `storage`, …) |

### 1.2 Tres formas de enlazar (LINK)

| Transporte | Requisito físico | Cuándo usarlo |
|----------|------------------|---------------|
| **USB OTG** | Cable OTG + permiso USB | Uso diario, flash, push `.sub`, máxima estabilidad |
| **WiFi BruceNet** | AP `BruceNet` / host `192.168.4.1` | OTA firmware, CLI sin cable (HTTP LAN) |
| **BLE** | Emparejar T-Embed | Experimental como CLI Bruce |

**Sin LINK OK** las acciones 🔴 fallarán o quedarán deshabilitadas.

### 1.3 Flujo recomendado (primera vez)

1. Onboarding → **acepta el checkbox legal**.
2. Conecta T-Embed por USB OTG → concede permiso USB.
3. Tab **CLI** → `info` → debe responder uptime/firmware.
4. Tab **RF** → **433.92 MHz** → **CAPTURAR RAW 15s**.

---

## 2. Navegación principal

| Tab | Nombre | Hardware T-Embed |
|-----|--------|------------------|
| INICIO | Dashboard / campo / favoritos TX | Parcial |
| RF | Sub-GHz, biblioteca ★, análisis | **Sí** |
| WiFi | Scanner del **teléfono** + WAR-DRIVE | No (GPS opcional) |
| NFC | NFC menú T-Embed / IR vía Bruce | IR sí; NFC CLI no documentado |
| CLI | Terminal Bruce (comandos validados) | **Sí** |
| AI | LOCAL / Gemini / Ollama (auto-ejecutar **OFF** por defecto) | Si ejecuta: 🔴 |
| Tools | Mapas, export, flash, sync SD | Mixto |

---

## 3. Pantalla INICIO (Dashboard)

| Control | Qué hace | Hardware |
|---------|----------|----------|
| **RX 15s** | Captura RAW | 🔴 CC1101 |
| **TX ÚLTIMA / favoritos** | Replay (RAW necesita USB para push `.sub`) | 🔴 |
| **MODO CAMPO** | FGS + loop RX + GPS + reporte HTML al detener | 🔴 + GPS |
| **REPORT HTML/PDF** | Informe de sesión | Local |

---

## 4. Tab RF

### 4.1 Spectrum / Live
Captura RAW documentada: `subghz rx raw <s>`. TX decodificado: `subghz tx {key} {Hz} {te} {count}`.

### 4.2 Biblioteca
- ★ Favoritos + filtro FAV.
- **TX RAW:** si no hay archivo en SD (`device:…`), la app hace `storage write` por **USB** a `BruceRF/embed_<id>.sub` y luego `tx_from_file`.
- Sin USB: TX RAW bloqueado con mensaje claro.

### 4.3 Análisis
Comparador A/B + atajos AI (sin TX automático).

---

## 5. Tab WiFi (teléfono)

> Escaneo WiFi/BLE usa radios del **Android**, no el CC1101.

| Control | Función |
|---------|---------|
| **WAR-DRIVE** | Pide confirmación legal; guarda APs/BLE con GPS |
| Escaneos | Se detienen al cambiar de tab o al ir a segundo plano |

---

## 6. Tab NFC / IR

- **IR:** `ir rx raw` / `ir tx PROTOCOL ADDR CMD` (sin `0x`).
- **NFC:** no hay CLI oficial en la wiki Bruce; usa el menú del T-Embed. La app guarda dumps si llegan por consola.

---

## 7. Tab CLI

- Todo comando pasa por `BruceCommandValidator` (largo, una línea, bloqueo de `reboot`, `storage rm`, `rm -rf`, etc.).
- La consola **redacta** RAW/hex largos (códigos de apertura sensibles).
- Chips seguros: `info`, `subghz rx raw`, `ir`, `storage list /`, …

---

## 8. Tab AI

| Modo | Notas |
|------|--------|
| LOCAL | Reglas offline |
| GEMINI | API key en SecureStore (campo oculto); HTTPS |
| OLLAMA | HTTP LAN (mejor en build DEBUG o host permitido) |
| Auto-ejecutar | **Desactivado por defecto** en 3.8.1 |

---

## 9. Widget

| Botón | Acción |
|-------|--------|
| RF | Abre app en RF |
| RX 15s | Captura (requiere app viva + token) |
| TX ★ | TX del favorito RF #1 (LINK + USB si RAW) |

Los botones RX/TX no aceptan broadcasts externos sin el token interno del widget.

---

## 10. Seguridad y privacidad (resumen)

- Gemini en EncryptedSharedPreferences.
- Backup cloud **no** incluye Room ni SecureStore.
- Política de privacidad y licencias OSS en **Acerca de**.
- Tema visual **dark-only** (AMOLED cyberpunk) a propósito.

---

## 11. Uso legal

Solo investigación autorizada / tus dispositivos / pentest con contrato.  
No interferir con comunicaciones ajenas ni retransmitir señales de terceros sin permiso.  
Wardriving y TX RF pueden estar regulados en tu jurisdicción.
