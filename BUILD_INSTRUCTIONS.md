# Instrucciones de Compilación - EmbedSuite v4.5.0

## Requisitos previos

1. **Android Studio Hedgehog** (2023.1.1) o superior
2. **JDK 17** (OpenJDK o Oracle)
3. **Gradle 8.2+** (incluido en el proyecto)
4. **Git** (para clonar el repositorio)

## Pasos para compilar

### Opción 1: Compilación Debug (recomendada para pruebas)

```bash
# Navega al directorio del proyecto
cd c:\Users\Administrator\AndroidStudioProjects\EMBEDSUITE

# Compila el APK debug
.\gradlew.bat assembleDebug
```

El APK se generará en: `app/build/outputs/apk/debug/app-debug.apk`

### Opción 2: Compilación Release (firmada)

1. Crea un archivo `keystore.properties` en la raíz del proyecto:
```properties
storeFile=../ruta/a/tu/keystore.jks
storePassword=tu_password
keyAlias=tu_alias
keyPassword=tu_password
```

2. Compila:
```bash
.\gradlew.bat assembleRelease
```

El APK se generará en: `app/build/outputs/apk/release/app-release.apk`

## Instalación en dispositivo

### Con ADB (recomendado)
```bash
# Instala la app
adb install -r app/build/outputs/apk/debug/app-debug.apk

# O para release
adb install -r app/build/outputs/apk/release/app-release.apk
```

### Sin ADB
1. Transfiere el APK al teléfono
2. En Android: Ajustes → Seguridad → Fuentes desconocidas → Activar
3. Toca el APK para instalar

## Verificación de la versión

1. Abre la app
2. Ve a **Ajustes → Acerca de**
3. Debería mostrar **EmbedSuite v4.5.0**

## Solución de problemas

### Error: "No se reconoce gradlew.bat"
- Verifica que estás en el directorio correcto: `cd EMBEDSUITE`
- Usa `.\gradlew.bat` (con punto y barra invertida)

### Error: "Fallo en la compilación"
```bash
# Limpia y recompila
.\gradlew.bat clean assembleDebug
```

### La app no detecta v0.19.0 Maya
- Verifica que el dispositivo tenga internet
- Revisa los logs en Logcat con el filtro: `FirmwareRepository`
- Deberías ver líneas como:
  ```
  D/FirmwareRepository: Fetching releases from GitHub...
  D/FirmwareRepository: GitHub API response code: 200
  D/FirmwareRepository: Processing release: v0.19.0 (prerelease=false)
  D/FirmwareRepository:   Asset: xibalba-t-embed-cc1101.bin
  D/FirmwareRepository:   ✓ Added: v0.19.0, SHA256: f19a06cb...
  ```
- Si GitHub falla, el catálogo embebido sigue mostrando **Xibalba-0.19.0 Maya** como recomendado.

## Características de v4.5.0

- ✅ Resync con firmware **Xibalba-0.19.0 Maya** ([xibalba-bruce](https://github.com/GIOSANBLAS/xibalba-bruce))
- ✅ Catálogo OTA / assets: `xibalba-t-embed-cc1101.bin` merged @ 0x0
- ✅ Detección perfil **XIBALBA** (Bruce + TEH-Link) vs **UNKNOWN** (stock Bruce)
- ✅ Legacy v0.18.0 Iron Shield (te-embed-xibalba) como rollback, no recomendado
- ✅ Guías y strings alineados con UI español del dispositivo
- ✅ Flasheo USB robusto con múltiples intentos de sync
- ✅ Integración TEH-Link v3 (Evil Portal, Beacon Spam, Offensive Toolkit)

## Soporte

- GitHub app: https://github.com/GIOSANBLAS/EmbedSuite
- Firmware (recomendado): https://github.com/GIOSANBLAS/xibalba-bruce/releases/tag/v0.19.0
- Firmware legacy (rollback): https://github.com/GIOSANBLAS/te-embed-xibalba/releases/tag/v0.18.0
