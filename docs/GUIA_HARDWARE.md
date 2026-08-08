# 🔧 GUÍA DE HARDWARE — T-Embed CC1101 Plus

**EmbedSuite v4.4.0 · Firmware Xibalba · GIOSÁNBLAS**

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
| **microSD** | Slot tarjeta (FAT32) |
| **Batería** | LiPo 1S con PMIC BQ25896 |
| **Conector** | USB-C (OTG) |
| **Botón lateral** | Modo pairing TEH-Link / navegación |

---

## 2. Requisitos de la tarjeta microSD

> **IMPORTANTE:** La microSD es **obligatoria** para plugins como Sub-GHz library, Evil Portal y wardriving.

| Aspecto | Requisito |
|---------|-----------|
| **Formato** | FAT32 (obligatorio) |
| **Tamaño máximo** | 32 GB |
| **Tamaño recomendado** | 8 - 16 GB Clase 10 |
| **Etiqueta** | `XIBALBA` (opcional) |
| **Estructura** | Se crea automática en el primer arranque |

### Formatear en Windows
1. Conecta la tarjeta al PC.
2. Abre **Administración de discos** o usa **Rufus**.
3. Selecciona **FAT32** y **32 KB** de unidad de asignación.
4. Formatea y verifica que no tenga sectores dañados.

### Formatear en Android
1. Inserta la tarjeta en el teléfono.
2. Ve a **Ajustes → Almacenamiento**.
3. Selecciona la tarjeta → **Formatear como portable**.
4. Elige **FAT32** si el sistema lo permite (algunos Android solo exFAT; en ese caso usa PC).

> ⚠️ Si usas una tarjeta >32 GB, particiona a 32 GB FAT32 o usa exFAT (con menor compatibilidad).

---

## 3. Conexión OTG y configuración USB en Android

### Cable USB-C con datos

El T-Embed usa un **conector USB-C** que funciona como **dispositivo** (device). Para conectarlo al teléfono necesitas:

| Tipo de cable | ¿Funciona? |
|---------------|------------|
| **USB-C a USB-C** (con datos) | ✅ Sí, si el teléfono y el cable soportan OTG/DP alt mode |
| **USB-C a USB-A** (con adaptador OTG) | ✅ Recomendado |
| **Cable de carga solamente** | ❌ No (sin pines de datos) |

**Recomendaciones:**
- Usa un **adaptador OTG** (USB-C hembra → USB-A macho) con cable USB-A a USB-C de datos.
- El cable debe tener los **4 pines** (VBUS, GND, D+, D-).
- Evita cables muy largos (>1 m) para máxima estabilidad.

### Configurar USB en Android

1. Ve a **Ajustes → Sistema → Opciones de desarrollador**.
2. Marca **No preguntar de nuevo** en el diálogo de permiso USB (o mantén la app como predeterminada).
3. Al conectar, debe aparecer la notificación **"T-Embed conectado vía USB"**.

### Verificar la conexión

```bash
# Con adb (terminal PC)
adb devices
# Debe aparecer el dispositivo

# Con la app:
# Dashboard → LINK debe estar en VERDE
```

---

## 4. Emparejamiento TEH-Link

1. Conecta el T-Embed por USB OTG.
2. En la app, ir a **Dashboard → LINK**.
3. Mantén pulsado el **botón lateral** del T-Embed ~2 segundos.
4. La ventana de pairing se abre (120 s).
5. La app muestra **"TEH-Link emparejado correctamente"**.

> 💡 El token se guarda cifrado en el teléfono. No hace falta re-emparejar salvo que cambies de teléfono o firmware.

---

## 5. Recomendaciones de energía

| Situación | Recomendación |
|-----------|---------------|
| **Uso diario USB** | El T-Embed se alimenta desde el teléfono OTG. Batería del teléfono baja más rápido. |
| **Uso de campo (sin USB)** | Carga el T-Embed con un cargador LiPo antes. El PMIC BQ25896 gestiona la carga. |
| **Almacenamiento** | No dejes la batería al 0% por tiempo prolongado. |
| **Indicador de carga** | El LED / pantalla muestra el estado de carga. La app muestra batería en el Dashboard. |

---

## 6. Solución de problemas de hardware

| Problema | Causa probable | Solución |
|----------|---------------|----------|
| La app no detecta el USB | Cable solo carga | Prueba otro cable con datos |
| LINK rojo / Error USB | Permisos USB revocados | Ajustes → Apps → EmbedSuite → Forzar detención y reconectar |
| SD no aparece como OK | Formato incorrecto | Formatea FAT32 en PC |
| Pairing falla siempre | Botón pulsado fuera de ventana | Mantén 2 s y espera la notificación antes de soltar |
| OTA se interrumpe | Cable inestable | Usa cable corto de calidad · No toques el cable durante la OTA |
| Sub-GHz no recibe | Antena / frecuencia | Verifica frecuencia 433.92 · Alejate de fuentes RF |

---

## 7. Diagrama de conexión

```
┌─────────────────┐      USB-C (datos)      ┌──────────────────┐
│   Teléfono      │  ◄────────────────────► │  T-Embed CC1101  │
│   Android       │      OTG + cable        │  Plus            │
│   (EmbedSuite)  │                         │  (Xibalba)       │
└─────────────────┘                         └──────────────────┘
         │                                          │
         │ GPS / WiFi / BLE del teléfono             │ microSD (FAT32)
         ▼                                          ▼
   Wardriving / capturas                      Plugins / wardriving / library
```

---

*Documentación v4.4.0 · EmbedSuite · GIOSÁNBLAS*