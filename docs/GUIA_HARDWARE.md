# 🔧 GUÍA DE HARDWARE — T-Embed CC1101 Plus

**EmbedSuite v4.5.0 · Firmware Xibalba-0.19.0 Maya · GIOSÁNBLAS**

Guía completa para el hardware **LilyGO T-Embed CC1101 Plus** y su conexión con la app Android EmbedSuite.

---

## 1. Especificaciones técnicas

| Componente | Especificación |
|------------|----------------|
| **SoC** | ESP32-S3 (Xtensa dual-core 240 MHz, WiFi + BLE 5) |
| **Flash** | 16 MB (doble slot OTA) |
| **PSRAM** | 8 MB OPI DRAM |
| **Pantalla** | 1.9" TFT LCD (170×320) |
| **Radio Sub-GHz** | CC1101 (315/433/868/915 MHz) |
| **NFC** | PN532 (ISO14443A/B, Mifare Classic) |
| **IR** | Transmisor + receptor IR |
| **microSD** | Slot tarjeta (FAT32) — recomendada para wardriving y biblioteca RF |
| **Batería** | LiPo 1S con PMIC BQ25896 |
| **Conector** | USB-C (OTG) |
| **Botón lateral** | GPIO6 — pairing TEH-Link / navegación |
| **Firmware objetivo** | **Xibalba-0.19.0 Maya** (Bruce runtime + TEH-Link v3) |

---

## 2. Firmware Xibalba-0.19.0 Maya

### Runtime oficial vs legacy

| Característica | v0.19.0 Maya (xibalba-bruce) | v0.18.0 Iron Shield (legacy ESP-IDF) |
|----------------|------------------------------|--------------------------------------|
| Base runtime | **Bruce** | ESP-IDF nativo |
| TEH-Link v3 | ✅ | ✅ |
| Perfil EmbedSuite | **XIBALBA** | XIBALBA (legacy) |
| UI dispositivo | **Español Maya/cyber** | Español ESP-IDF |
| Binario merged @ 0x0 | ✅ `xibalba-t-embed-cc1101.bin` | `te-embed-xibalba.bin` |
| Repo | [xibalba-bruce](https://github.com/GIOSANBLAS/xibalba-bruce) | [te-embed-xibalba](https://github.com/GIOSANBLAS/te-embed-xibalba) |
| Recomendado | **★ Sí** | Solo rollback |

> **Stock Bruce** (sin parche TEH-Link): la app lo detecta como perfil **UNKNOWN** — no hay simbiosis EmbedSuite hasta flashear Xibalba-0.19.0 Maya.

### Flasheo recomendado

1. **App EmbedSuite v4.5.0** → Map & Tools → Firmware.
2. Selecciona **Xibalba-0.19.0 Maya ★**.
3. OTA (si ya tienes Xibalba operativo) o Flash USB (primera vez / bootloader).
4. SHA256 embebido en catálogo: `f19a06cb…c9f58c9`.

Si la descarga GitHub falla, importa `xibalba-t-embed-cc1101.bin` manualmente — el catálogo embebido sigue mostrando v0.19.0 como recomendado.

**Desde PC (esptool):**
```bash
esptool.py --chip esp32s3 -p COMx -b 460800 write_flash 0x0 xibalba-t-embed-cc1101.bin
```

---

## 3. microSD

> **Recomendada** para wardriving, biblioteca Sub-GHz y almacenamiento en SD del dispositivo.

| Aspecto | Requisito |
|---------|-----------|
| **Formato** | FAT32 |
| **Tamaño máximo** | 32 GB |
| **Recomendado** | 8–16 GB Clase 10 |
| **Etiqueta** | `XIBALBA` (opcional) |

### Formatear en Windows
1. Administración de discos o **Rufus** → FAT32, 32 KB allocation unit.

### Formatear en Android
1. Ajustes → Almacenamiento → Formatear como portable → FAT32 si disponible.

---

## 4. Conexión OTG

| Tipo de cable | ¿Funciona? |
|---------------|------------|
| USB-C a USB-C (datos) | ✅ Si el teléfono soporta OTG |
| USB-C + adaptador OTG USB-A | ✅ Recomendado |
| Cable solo carga | ❌ |

**VID/PID T-Embed Xibalba (USB CDC):** `0x303A:0x303A` — la app lo prioriza automáticamente.

### Android
1. Opciones de desarrollador → permisos USB persistentes.
2. Al conectar: notificación USB → conceder a EmbedSuite.
3. Dashboard → **LINK verde**.

---

## 5. Emparejamiento TEH-Link

1. Conecta USB OTG.
2. Mantén **GPIO6 (botón lateral)** ~2 segundos.
3. Ventana de pairing: 120 s.
4. Token cifrado en teléfono — no re-emparejar salvo cambio de teléfono/firmware.

---

## 6. Checklist funcional CC1101 Plus (v0.19.0 Maya)

Ejecuta con Xibalba-0.19.0 Maya + Modo Auditoría activo:

| # | Prueba | Dónde en app | Resultado esperado |
|---|--------|--------------|-------------------|
| 1 | Conexión USB | Dashboard LINK | Verde, perfil XIBALBA, firmware 0.19.0+ |
| 2 | get_info | Dashboard / CLI | `product: T-Embed Xibalba`, `proto: teh-link` |
| 3 | UI español | Pantalla T-Embed | Menú Maya/cyber en español |
| 4 | Hardening flags | Dashboard | 6 flags visibles |
| 5 | Sub-GHz RX 15s | Dashboard RX 15s | Señal en Última señal |
| 6 | Sub-GHz TX replay | RF → Retransmitir | TX vía TEH-Link (autorizado) |
| 7 | WiFi scan T-Embed | Dashboard WiFi scan | APs listados |
| 8 | Wardriving | Dashboard Start/Stop | Sesión GPS + WiFi en mapa |
| 9 | NFC read | NFC/IR tab | UID leído vía PN532 |
| 10 | IR RX 10s | NFC/IR tab | Código capturado |
| 11 | OTA check | Map & Tools | v0.19.0 recomendado, no downgrade a legacy |
| 12 | BLE Spam | Dashboard / Scripts | Campaña start/stop (Auditoría ON) |
| 13 | WiFi Probe | Probe Sniffer screen | Probes con RSSI/OUI |
| 14 | Spectrum | Spectrum screen | Heatmap 433 MHz |
| 15 | NFC Clone | NFC Clone screen | Read Mifare 1K |

---

## 7. Energía

| Situación | Recomendación |
|-----------|---------------|
| Uso USB diario | Alimentación desde teléfono OTG — consume batería del móvil |
| Campo sin USB | Carga LiPo previa (BQ25896) |
| Almacenamiento | No dejar batería al 0% prolongado |
| Indicador | Dashboard muestra % batería vía TEH-Link |

---

## 8. Solución de problemas

| Problema | Causa | Solución |
|----------|-------|----------|
| Perfil UNKNOWN | Stock Bruce sin TEH-Link | Flashea Xibalba-0.19.0 Maya (xibalba-bruce) |
| Flash muestra v0.18.0 legacy | Catálogo antiguo / GitHub falló | App v4.5.0+ carga v0.19.0 embebido automáticamente |
| Flags SIM en Dashboard | Firmware simulación | Re-flashea Xibalba-0.19.0 Maya |
| Plugin no responde | Auditoría OFF o capability ausente | Activa Auditoría · Verifica `get_status` |
| Mousejack sin resultados | Sin módulo nRF24 conectado | Verificar hardware nRF24 en T-Embed |
| NFC Clone falla | Tag no Mifare/NTAG o keys incorrectas | Usar keys default del script |
| OTA interrumpida | Cable inestable | Cable corto de calidad, no mover durante OTA |
| CC1101 no RX | Frecuencia incorrecta | Verificar 433.92 MHz, antena conectada |

---

## 9. Diagrama de conexión

```
┌─────────────────┐      USB-C (datos)      ┌──────────────────────────┐
│   Teléfono      │  ◄────────────────────► │  T-Embed CC1101 Plus     │
│   Android       │      OTG + cable        │  Xibalba-0.19.0 Maya     │
│   (EmbedSuite)  │                         │  CC1101 · PN532 · IR     │
└─────────────────┘                         └──────────────────────────┘
         │                                              │
         │ GPS / WiFi / BLE del teléfono                │ microSD FAT32
         ▼                                              ▼
   Wardriving / scans phone                     Biblioteca RF · wardriving
```

---

*Documentación v4.5.0 · EmbedSuite · GIOSÁNBLAS*
