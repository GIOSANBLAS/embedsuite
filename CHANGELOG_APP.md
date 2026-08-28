# EmbedSuite — Changelog

## v1.1.0 — Companion overhaul

> 📅 2026-08-28 · T-Embed CC1101 Plus + firmware [Bruce](https://github.com/BruceDevices/firmware)

### ✨ Nuevo

- **BadUSB Forge** — Editor visual de bloques DuckyScript con plantillas, preview de script y pipeline automático (subir WiFi → `badusb run_from_file`)
- **Sub-GHz Analyzer** — Captura async con slider de frecuencia/duración, forma de onda y replay desde la app
- **IR Search** — Buscador IRDB integrado con sync remoto y envío vía `ir tx_from_file`
- **IR Finder** — Escucha IR (`ir rx`) y guardado en biblioteca local
- **Editor Mifare / NFC** — Biblioteca NFC con dumps, clonado y gestión desde companion
- **Telemetría en top bar** — Batería, temperatura y transporte activo del T-Embed en tiempo real (BLE poller + `info` CLI)
- **Temas Diurnal / Obscuro** — Toggle en Ajustes: modo hacker AMOLED (Obscuro) o terminal legible de día (Diurnal); tipografía mono en acentos + sans en cuerpo
- **Firmware Flash** — Flasheo USB (esptool) de Bruce oficial o `.bin` custom con disclaimer y progreso OTA/USB
- **UI glass overhaul** — Gradientes, bordes con brillo, elevación en cards/botones y fondo animado con intensidad configurable

### 🔧 Mejoras

- **Ajustes reorganizados** — Audio, conexión USB, UI (tema Diurnal/Obscuro, scanlines, glass), modo campo; idioma automático del dispositivo; acceso developer (7 taps en versión)
- **Orquestador de intenciones** — Preparar → upload WiFi → disparar CLI Bruce sin escribir comandos manualmente
- **Biblioteca local** — Señales Sub-GHz, IR y NFC con export Flipper `.zip`
- **Modo desarrollador** — Consola CLI, archivos SD Bruce, Bruce Config sync, Fleet y Ops Center

### 📦 v1.0.0 — Lanzamiento inicial

- Orquestador de intenciones: preparar → upload WiFi → disparar CLI Bruce
- Herramientas: Sub-GHz async, BadUSB (bloques visuales), buscador IRDB, captura IR
- Conexión USB / BLE / WiFi, wizards de emparejamiento, flash firmware USB
- Biblioteca local de señales, IR y NFC
- Modo desarrollador (consola CLI, archivos SD, RF hub)
