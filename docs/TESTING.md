# Testing — EmbedSuite 1.0.0

## Ejecutar tests

```bash
./gradlew testDebugUnitTest
```

## Áreas cubiertas

| Área | Prioridad |
|------|-----------|
| Orquestador / intents | P0 |
| Transporte y CLI | P0 |
| Parsers RF/IR | P1 |
| Room / repositorios | P1 |
| UI ViewModels críticos | P2 |

## Checklist manual (dispositivo)

- [ ] Conexión USB → `info`
- [ ] Captura Sub-GHz → biblioteca → replay
- [ ] BadUSB bloque → upload → run
- [ ] Búsqueda IRDB → transmit
- [ ] Flash USB (opcional, hardware de prueba)

## CI local

```bash
./gradlew clean assembleDebug testDebugUnitTest validateChangelog
```
