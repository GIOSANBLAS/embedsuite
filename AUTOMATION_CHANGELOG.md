# 📋 Automatización de Changelog - EMBEDSUITE

Este proyecto tiene **3 capas de automatización** para mantener el changelog actualizado:

## 1️⃣ Git Hook Pre-Commit (Local)

**Archivo:** `.git/hooks/pre-commit`

### Qué hace:
- ✅ Valida **antes de cada commit** que CHANGELOG_APP.md esté actualizado
- 🛑 Bloquea commits si detecta cambios importantes sin changelog actualizado
- ⚠️ Muestra avisos claros sobre qué actualizar

### Palabras clave detectadas:
```
feat:, fix:, refactor:, perf:, docs:, style:, test:, chore:
Evil Portal, Beacon Spam, Auditoría, etc.
```

### Cambios que activan validación:
- `build.gradle.kts` (versión, dependencias)
- `gradle.properties`
- `libs.versions.toml`
- Cualquier archivo con palabras clave

### Cómo funciona:
```bash
git add .
git commit -m "feat(auth): add login"  # ❌ Fallará si changelog no está actualizado
```

Si falla, debes:
```bash
# 1. Actualizar CHANGELOG_APP.md
# 2. Hacer:
git add CHANGELOG_APP.md
git commit --amend --no-edit
```

---

## 2️⃣ GitHub Actions (Equipo)

**Archivo:** `.github/workflows/changelog-sync.yml`

### Qué hace:
- ✅ Valida automáticamente en **cada PR y push** a main/develop
- 💬 Comenta en PRs si el changelog está desactualizado
- 🚫 Bloquea merge si falta changelog
- 📊 Valida formato de Conventional Commits

### Cuándo se ejecuta:
```
- Push a main o develop
- Pull Request a main o develop
- Cambios en: build.gradle.kts, gradle.properties, libs.versions.toml, CHANGELOG_APP.md
```

### Validaciones:
1. ✅ Version en `build.gradle.kts` coincide con `CHANGELOG_APP.md`
2. ✅ Formato: `## vX.Y.Z — Descripción`
3. ✅ Commits siguen Conventional Commits (feat:, fix:, etc.)

### Si falla en PR:
```
GitHub comenta automáticamente con:
⚠️ Changelog no actualizado
Versión esperada: v4.3.0
```

Debes actualizar el changelog localmente y hacer push.

---

## 3️⃣ Gradle Task (Build)

**Archivo:** `build.gradle.kts`

### Qué hace:
- ✅ Valida changelog **durante el build** (gradle build)
- 🛑 Detiene el build si changelog está desactualizado
- ⚠️ Muestra versión esperada vs actual

### Tarea disponible:
```bash
# Ejecutar validación manual
./gradlew validateChangelog

# Ver si falla antes de compilar
./gradlew build  # Fallará en preBuild si changelog no está actualizado
```

### Error típico:
```
❌ CHANGELOG_APP.md is out of sync!

📦 Version in build.gradle.kts: v4.3.0 (code: 31)
📝 Missing in CHANGELOG_APP.md

Please update CHANGELOG_APP.md with:
## v4.3.0 — [Descripción de cambios]
```

---

## 📝 Flujo de Trabajo Recomendado

### Cuando agregues nuevas features:

1. **Desarrolla la feature**
   ```bash
   git checkout -b feature/evil-portal
   # ... desarrolla ...
   git add src/
   ```

2. **Actualiza el changelog PRIMERO**
   ```bash
   # Edita CHANGELOG_APP.md
   # Agrega sección: ## vX.Y.Z — Descripción
   git add CHANGELOG_APP.md
   ```

3. **Actualiza versión en build.gradle.kts**
   ```gradle
   versionCode = 32
   versionName = "4.4.0"
   git add app/build.gradle.kts
   ```

4. **Haz commit (el hook lo validará)**
   ```bash
   git commit -m "feat(evil-portal): add detection and logging"
   # ✅ Pre-commit hook valida automáticamente
   ```

5. **Push y PR**
   ```bash
   git push origin feature/evil-portal
   # GitHub Actions valida automáticamente
   ```

6. **Merge a main**
   - GitHub Actions validará antes de permitir merge
   - Gradle build también validará durante compilación

---

## 🔧 Configuración Adicional

### Para nuevos developers (después de git clone):

El hook ya existe en `.git/hooks/pre-commit`, pero si necesitas reinstalarlo:

```bash
chmod +x .git/hooks/pre-commit
```

### Saltar validación (SOLO en emergencias):

```bash
git commit --no-verify -m "emergency fix"  # Salta pre-commit hook
```

**Nota:** GitHub Actions aún validará en el servidor.

---

## 📊 Formato de Changelog Esperado

```markdown
## vX.Y.Z — Descripción Corta
> 📅 2026-08-06 · Resumen de cambios principales

### ✨ Nuevas Funcionalidades
- Feature 1
- Feature 2

### 🔧 Correcciones
- Fix 1
- Fix 2

### 📚 Documentación
- Doc update 1

---
```

### Emojis recomendados:
- 🆕 Nuevas características
- ✨ Mejoras
- 🔧 Correcciones/Fixes
- 📚 Documentación
- 🛡️ Seguridad
- ⚡ Performance
- 🐛 Bug fixes
- 📝 Cambios menores

---

## ✅ Checklist para cambios

Antes de hacer commit:

- [ ] Edité CHANGELOG_APP.md con nueva sección v X.Y.Z
- [ ] Actualicé versionCode y versionName en build.gradle.kts
- [ ] Mi commit message sigue Conventional Commits (feat:, fix:, etc.)
- [ ] Pasó pre-commit hook ✅
- [ ] El formato del changelog es correcto

---

## 🆘 Troubleshooting

### "Pre-commit hook bloqueó mi commit"
Solución: Actualiza CHANGELOG_APP.md y vuelve a intentar

### "GitHub Actions falla en PR"
Solución: Los comentarios automáticos te dirán qué falta

### "Gradle build falla"
Solución: Ejecuta `./gradlew validateChangelog` para ver el error

### "Quiero ver qué detectó el hook"
Ejecuta: `git diff --cached` para ver cambios staged
