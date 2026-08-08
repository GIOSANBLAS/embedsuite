# Instrucciones de compilación — EmbedSuite v1.0.0

Compilación desde cero del companion Android para Xibalba.

---

## 1. Requisitos

| Herramienta | Versión |
|-------------|---------|
| Android Studio | Hedgehog (2023.1.1) o superior |
| JDK | 17 |
| Gradle | 8.2+ (wrapper incluido) |
| Git | Cualquier versión reciente |

**Target device:** Android 12 o superior (API 31+).

---

## 2. Clonar e importar

```bash
git clone https://github.com/GIOSANBLAS/embedsuite.git
cd embedsuite
```

Abre la carpeta raíz en Android Studio. Espera sync de Gradle.

---

## 3. Build debug

```bash
# Windows
.\gradlew.bat clean assembleDebug

# Linux / macOS
./gradlew clean assembleDebug
```

**Salida:** `app/build/outputs/apk/debug/app-debug.apk`

---

## 4. Build release

### 4.1 Keystore

Crea `keystore.properties` en la raíz del proyecto:

```properties
storeFile=../ruta/a/tu/keystore.jks
storePassword=tu_password
keyAlias=tu_alias
keyPassword=tu_password
```

### 4.2 Compilar

```bash
.\gradlew.bat assembleRelease
```

**Salida:** `app/build/outputs/apk/release/app-release.apk`

---

## 5. Instalación

### ADB

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Manual

Transfiere el APK al teléfono e instala desde el gestor de archivos (fuentes desconocidas habilitadas).

---

## 6. Verificación

1. Abre EmbedSuite.
2. **Ajustes → Acerca de** → confirma **v1.0.0**.
3. Conecta T-Embed con Xibalba v1.0.0.
4. Dashboard debe mostrar perfil **XIBALBA**.

---

## 7. Tests

```bash
.\gradlew.bat test
.\gradlew.bat connectedAndroidTest   # requiere dispositivo/emulador API 31+
```

Plan completo: [docs/TESTING.md](docs/TESTING.md)

---

## 8. Solución de problemas

| Error | Solución |
|-------|----------|
| `gradlew` no reconocido | Ejecuta desde la raíz del repo; usa `.\gradlew.bat` en Windows |
| Sync falla | `./gradlew --stop`; invalidar caché en Android Studio |
| compileSdk mismatch | Actualiza Android Studio y SDK Platform 34+ |
| USB permission en runtime | Normal en dispositivo físico; no aplica en emulador sin OTG |

---

## 9. Referencias

| Documento | Contenido |
|-----------|-----------|
| [README.md](README.md) | Visión general v1.0.0 |
| [ARCHITECTURE.md](ARCHITECTURE.md) | Capas y TEH-Link |
| [docs/GUIA_HARDWARE.md](docs/GUIA_HARDWARE.md) | Hardware y flasheo |

---

*Build instructions · EmbedSuite v1.0.0*
