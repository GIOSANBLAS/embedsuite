# 🔧 GUÍA DE HARDWARE — T-Embed CC1101 Plus

**EmbedSuite v4.4.0 · Firmware Xibalba v0.18.0 Iron Shield · GIOSÁNBLAS**

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
| **microSD** | Slot tarjeta (FAT32) — **obligatoria para plugins v0.18+** |
| **Batería** | LiPo 1S con PMIC BQ25896 |
| **Conector** | USB-C (OTG) |
| **Botón lateral** | GPIO6 — pairing TEH-Link / navegación |
| **Firmware objetivo** | **Xibalba v0.18.0 Iron Shield** |

---

## 2. Firmware Xibalba v0.18.0 Iron Shield

### Qué incluye respecto a v0.17.1

| Característica | v0.17.1 Spark | v0.18.0 Iron Shield |
|----------------|---------------|---------------------|
| TEH-Link v3 | ✅ | ✅ |
| OTA SHA256 verify | ✅ | ✅ |
| Hardening dashboard | ✅ | ✅ ampliado |
| Evil Portal | ❌ | ✅ |
| Beacon Spam | ❌ | ✅ |
| BLE AD Spam | ❌ | ✅ |
| WiFi Deauth + Probe Sniffer | ❌ | ✅ |
| Mousejack NRF24 | Parcial | ✅ plugin dedicado |
| Sub-GHz Spectrum + Auto-decoder | Parcial | ✅ |
| NFC Clone/Write Mifare+NTAG | Parcial | ✅ |
| Modo Auditoría (gating plugins TX) | ❌ | ✅ |

### Flasheo recomendado

1. **App EmbedSuite v4.4.0** → Map & Tools → Firmware.
2. Selecciona **v0.18.0 Iron Shield ★**.
3. OTA (si ya tienes Xibalba) o Flash USB (primera vez / bootloader).
4. SHA256 embebido en catálogo: `76fa3ed1…cc2a31dd`.

Si la descarga GitHub falla, importa el `.bin` manualmente — el catálogo embebido sigue mostrando v0.18.0 como recomendado.

---

## 3. microSD

> **IMPORTANTE:** Obligatoria para Evil Portal, wardriving, biblioteca Sub-GHz y plugins en SD.

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

## 6. Checklist funcional CC1101 Plus (v0.18.0)

Ejecuta con Xibalba v0.18.0 + Modo Auditoría activo:

| # | Prueba | Dónde en app | Resultado esperado |
|---|--------|--------------|-------------------|
| 1 | Conexión USB | Dashboard LINK | Verde, firmware 0.18.0+ |
| 2 | Hardening flags | Dashboard | 6 flags visibles |
| 3 | Lista plugins | Dashboard XIBALBA PLUGINS | ≥10 plugins incl. evil_portal, ble_ad_spam |
| 4 | Sub-GHz RX 15s | Dashboard RX 15s | Señal en Última señal |
| 5 | Sub-GHz TX replay | RF → Retransmitir | TX vía TEH-Link (autorizado) |
| 6 | WiFi scan T-Embed | Dashboard WiFi scan | APs listados en terminal |
| 7 | Wardriving | Dashboard Start/Stop | Sesión GPS + WiFi en mapa |
| 8 | NFC read | NFC/IR tab | UID leído vía PN532 |
| 9 | IR RX 10s | NFC/IR tab | Código capturado |
| 10 | OTA check | Map & Tools | v0.18.0 recomendado, no downgrade |
| 11 | BLE Spam | Dashboard / Scripts | Campaña AppleJuice start/stop |
| 12 | WiFi Probe | Probe Sniffer screen | Probes con RSSI/OUI |
| 13 | Spectrum | Spectrum screen | Heatmap 433 MHz |
| 14 | NFC Clone | NFC Clone screen | Read Mifare 1K |
| 15 | Evil Portal | Scripts → Evil Portal | SoftAP + status (Auditoría ON) |

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
| Flash muestra v0.17.1 | Catálogo antiguo / GitHub falló | App v4.4.0+ carga v0.18.0 embebido automáticamente |
| Flags SIM en Dashboard | Firmware simulación | Re-flashea v0.18.0 Iron Shield |
| Plugin no responde | Firmware < v0.18.0 o Auditoría OFF | Actualiza firmware + activa Auditoría |
| Mousejack sin resultados | Sin módulo nRF24 conectado | Verificar hardware nRF24 en T-Embed |
| NFC Clone falla | Tag no Mifare/NTAG o keys incorrectas | Usar keys default del script |
| OTA interrumpida | Cable inestable | Cable corto de calidad, no mover durante OTA |
| CC1101 no RX | Frecuencia incorrecta | Verificar 433.92 MHz, antena conectada |

---

## 9. Diagrama de conexión

```
┌─────────────────┐      USB-C (datos)      ┌──────────────────────────┐
│   Teléfono      │  ◄────────────────────► │  T-Embed CC1101 Plus     │
│   Android       │      OTG + cable        │  Xibalba v0.18.0         │
│   (EmbedSuite)  │                         │  CC1101 · PN532 · IR     │
└─────────────────┘                         └──────────────────────────┘
         │                                              │
         │ GPS / WiFi / BLE del teléfono                │ microSD FAT32
         ▼                                              ▼
   Wardriving / scans phone                     Plugins · wardriving · portal
```

---

*Documentación v4.4.0 · EmbedSuite · GIOSÁNBLAS*
