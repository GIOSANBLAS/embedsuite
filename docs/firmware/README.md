# Xibalba 0.20 — parche de firmware (TEH-Link v3 extendido)

Este directorio contiene el trabajo de firmware de la rama
`cursor/xibalba-optimization-5cca` del repo
[**GIOSANBLAS/xibalba-bruce**](https://github.com/GIOSANBLAS/xibalba-bruce),
empaquetado para aplicar sobre `main` (052029e) porque el agente de Cursor
no tiene permisos de escritura en ese repo (token con scope solo a embedsuite).

## Contenido

| Archivo | Qué es |
|---------|--------|
| `xibalba-0.20-tehlink.bundle` | Bundle git con los **5 commits reales** (autoría, mensajes y borrado de `media/` intactos) |
| `xibalba-0.20-tehlink.patch` | Diff plano para review rápida (excluye `media/`; ver instrucciones abajo) |

## Aplicar (recomendado: bundle → conserva commits)

```bash
cd xibalba-bruce
git fetch /ruta/a/xibalba-0.20-tehlink.bundle cursor/xibalba-optimization-5cca:xibalba-0.20
git checkout xibalba-0.20        # incluye el borrado de media/ (commit 1)
# compilar: python -m platformio run -e lilygo-t-embed-cc1101
git push origin xibalba-0.20
```

## Aplicar (alternativa: parche plano)

El `.patch` excluye `media/` a propósito (41MB de binarios harían ilegible la
review); hay que borrarlo a mano:

```bash
cd xibalba-bruce
git apply /ruta/a/xibalba-0.20-tehlink.patch
git rm -r media/
git commit -m "feat: Xibalba 0.20 — TEH-Link v3 extendido + limpieza"
```

## Qué incluye (32 archivos, +1452/−3400)

1. **Limpieza**: fuera `tururururu` (Megalodon), `clicker`, `u2f`, `ibutton`
   y `media/` (41MB de docs Bruce). `firmware.bin`: 0x3FFC10 B.
2. **Core nuevo**: `time_sync` (reloj desde Android), `power_manager`
   (doze seguro sin romper USB CDC), `language` (ES/EN en NVS),
   `audio_feedback` (beeps semánticos NS4168).
3. **TEH-Link v3 extendido**: plugins `rf_scanner` (barrido RSSI CC1101
   headless con eventos `rf.scan.sample`), `rf_jammer` (continuo/ráfaga,
   cutoff de seguridad), `sd_storage` (mount/list/save), `audio` (beep);
   `nfc_toolkit` real (read/reader/write NDEF); `device` con
   `set_language`/`set_mode`/`power_status`; comando `time_sync`.
4. **Particiones OTA dual-slot** (`custom_16Mb.csv`): otadata + ota_0/ota_1
   de 0x440000. **Migrar desde ≤0.19 requiere reflasheo USB y borra NVS**
   (re-emparejar TEH-Link).
5. **Build robusto**: `build.py` lee el offset de factory del CSV;
   `patch.py` cae al toolchain unificado y recupera `libnet80211.a` si un
   build anterior quedó a medias.

Verificado: `platformio run -e lilygo-t-embed-cc1101` → **SUCCESS**
(RAM 41.3%, Flash 94.1% del nuevo slot OTA).

## Permisos para futuras sesiones

Para que el agente pueda pushear a `xibalba-bruce` directamente:
añadir un secret en Cursor Dashboard → Cloud Agents → Secrets
(p. ej. `GITHUB_TOKEN` con scope `repo` para GIOSANBLAS/xibalba-bruce),
o invitar al bot como collaborator con write.
