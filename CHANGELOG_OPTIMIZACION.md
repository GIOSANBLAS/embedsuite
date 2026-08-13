# CHANGELOG — Optimización EmbedSuite

Fecha: 2026-08-13

## FASE 2 / 3

- `get_info` parser lee `hardware` y `firmware` (criterio: `lilygo-t-embed-c1101-plus` / `Xibalba v0.19.2`).
- Perfil de dispositivo: T-Embed CC1101 Plus es el único hardware de primera clase. M5Stack ya no se detecta como target.
- Mock TEH-Link alineado a `hardware` / `firmware`.
- Textos de producto sin “stock Bruce” como firmware soportado.
- `sd.list` en firmware apunta a `/xibalba` (capturas Sub-GHz/IR/wardrive en la microSD del T-Embed).

## Pruebas manuales

1. Emparejar GPIO6 + USB OTG.
2. `get_info` debe mostrar hardware `lilygo-t-embed-c1101-plus`.
3. `sd.status` / `sd.list` path `/xibalba`.
4. Captura Sub-GHz → archivo en `/xibalba/subghz`.
5. `audio.tone` → pitido en NS4168.
