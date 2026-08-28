# Manual de usuario — EmbedSuite 1.0.0

**Companion Android · LilyGO T-Embed CC1101 Plus · Firmware [Bruce](https://github.com/BruceDevices/firmware)**

---

## 1. Requisitos

- Android 12+ (API 31+)
- T-Embed CC1101 Plus con firmware Bruce flasheado
- Cable USB OTG (opcional BLE/WiFi)

---

## 2. Primera conexión

1. Flashea Bruce en el T-Embed (Modo desarrollador → Mapa → Flash USB, o herramienta externa).
2. En Bruce: activa **BLE API** si usarás Bluetooth.
3. Conecta por **USB**, **BLE** o **WiFi AP** del dispositivo.
4. Abre EmbedSuite → revisa estado en **Inicio**.

---

## 3. Navegación

| Tab | Uso |
|-----|-----|
| **Inicio** | Estado, transporte, atajos companion |
| **Tools** | Sub-GHz, BadUSB, buscador IR, captura IR |
| **Biblioteca** | Señales RF, IR, NFC guardadas |
| **Forja** | Payloads y plantillas |

**Modo desarrollador** (Ajustes): consola Bruce CLI, explorador SD, RF hub, mapa/flash.

---

## 4. Tools

### Sub-GHz
Captura async, recorte de silencio, replay desde biblioteca.

### BadUSB
Editor por bloques visuales; sube `.txt` y ejecuta en el dispositivo.

### IR
Búsqueda en IRDB local, transmisión vía Bruce CLI.

---

## 5. Transportes

| Medio | Cuándo usarlo |
|-------|----------------|
| USB | CLI estable, flash firmware |
| BLE | Sin cable; requiere BLE API en Bruce |
| WiFi | Upload de archivos grandes, WebUI `/cm` |

---

## 6. Flash firmware

1. Modo desarrollador → **Mapa** → **Flash USB**
2. Selecciona `.bin` Bruce compatible T-Embed CC1101 Plus
3. Recovery hardware: Encoder + RST si el bootloop persiste

---

## 7. Soporte

- Repositorio: https://github.com/GIOSANBLAS/embedsuite
- Firmware: https://github.com/BruceDevices/firmware
