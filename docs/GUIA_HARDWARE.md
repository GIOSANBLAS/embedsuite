# Guía de hardware — T-Embed CC1101 Plus

## Dispositivo

| Componente | Detalle |
|------------|---------|
| MCU | ESP32-S3 |
| RF | CC1101 (Sub-GHz) |
| Pantalla | T-Embed integrada |
| Botón lateral | Pairing / acciones en firmware Bruce |

---

## Firmware

Usa **[Bruce](https://github.com/BruceDevices/firmware)** oficial para T-Embed CC1101 Plus.

EmbedSuite se comunica por **Bruce CLI** (líneas de texto) y upload WiFi — no espejo de pantalla.

---

## Conexión con el móvil

| Transporte | Requisito |
|------------|-----------|
| USB OTG | Cable USB-C, permiso serial |
| BLE | BLE API activada en Config Bruce |
| WiFi | AP del T-Embed o misma LAN |

---

## Checklist de campo

- [ ] Bruce flasheado y arranca
- [ ] Transporte activo (USB/BLE/WiFi)
- [ ] EmbedSuite muestra dispositivo conectado
- [ ] Prueba CLI: `info` desde consola (modo desarrollador)

---

## Recovery

- **Bootloop:** Flash USB con `.bin` Bruce + modo recovery (Encoder + RST)
- **Sin respuesta CLI:** Reconectar transporte, revisar Link Debug en ajustes
