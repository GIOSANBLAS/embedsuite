# EmbedSuite

Companion Android para **LilyGO T-Embed CC1101 Plus** con firmware **[Bruce](https://github.com/BruceDevices/firmware)**.

**Versión:** 1.0.0 · **Package:** `com.embedsuite.app` · **Web:** [bruce.computer](https://bruce.computer/)

El móvil es el cerebro (UI, biblioteca, análisis). El T-Embed es el músculo (RF, IR, BadUSB). **Sin espejo de pantalla** — solo CLI Bruce documentada y archivos estándar (`.sub`, `.ir`, `.txt`).

---

## Características

| Área | Descripción |
|------|-------------|
| **Inicio** | Estado del dispositivo, transportes, accesos companion |
| **Tools** | Sub-GHz, BadUSB, IRDB, captura IR |
| **Biblioteca** | Señales RF, botones IR, dumps NFC |
| **Forja** | Payloads y plantillas |
| **Flash USB** | Actualizar firmware Bruce vía esptool (Modo desarrollador → Mapa) |

---

## Transportes

| Transporte | Uso |
|------------|-----|
| **USB** | CLI serial + flasheo |
| **BLE** | CLI vía GATT (activa BLE API en Config Bruce) |
| **WiFi** | WebUI `POST /cm` + upload de archivos |

1. Flashea Bruce en el T-Embed.
2. Activa **BLE API** si usas Bluetooth.
3. Conecta USB, BLE o WiFi AP del dispositivo.
4. Abre EmbedSuite → tab **Tools** o Dashboard.

---

## Compilación

```bash
./gradlew clean assembleDebug testDebugUnitTest
```

Ver [BUILD_INSTRUCTIONS.md](BUILD_INSTRUCTIONS.md).

---

## Repositorios relacionados

| Proyecto | URL |
|----------|-----|
| EmbedSuite | https://github.com/GIOSANBLAS/embedsuite |
| Bruce firmware | https://github.com/BruceDevices/firmware |

---

## Licencia

[GNU GPL v3.0](LICENSE)
