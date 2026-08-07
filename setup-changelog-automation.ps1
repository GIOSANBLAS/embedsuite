#!/usr/bin/env powershell
# Setup script para configurar changelog automation en Windows

Write-Host "🔧 Configurando automatización de changelog..." -ForegroundColor Green

# 1. Copiar hook pre-commit desde scripts/git-hooks a .git/hooks
Write-Host "`n📝 Instalando git hooks..." -ForegroundColor Yellow

if (Test-Path "scripts\git-hooks\pre-commit") {
    Copy-Item "scripts\git-hooks\pre-commit" ".git\hooks\pre-commit" -Force
    Write-Host "✅ Hook pre-commit instalado en: .git\hooks\pre-commit" -ForegroundColor Green
} else {
    Write-Host "❌ No se encontró scripts\git-hooks\pre-commit" -ForegroundColor Red
}

# 2. Validar que estamos en el root del proyecto
if (-not (Test-Path ".git")) {
    Write-Host "❌ Error: No estamos en el directorio raíz del proyecto" -ForegroundColor Red
    exit 1
}

# 3. Verificar GitHub Actions
if (Test-Path ".github\workflows\changelog-sync.yml") {
    Write-Host "✅ GitHub Actions workflow en: .github\workflows\changelog-sync.yml" -ForegroundColor Green
} else {
    Write-Host "⚠️  GitHub Actions workflow no encontrado" -ForegroundColor Yellow
}

# 4. Verificar Gradle task
$buildGradleContent = Get-Content "build.gradle.kts" -Raw
if ($buildGradleContent -match "validateChangelog") {
    Write-Host "✅ Gradle task 'validateChangelog' configurada" -ForegroundColor Green
    Write-Host "   Ejecuta: ./gradlew validateChangelog" -ForegroundColor Cyan
} else {
    Write-Host "⚠️  Gradle task no encontrada" -ForegroundColor Yellow
}

# 5. Información de uso
Write-Host "`n📚 Guía de uso:" -ForegroundColor Yellow
Write-Host "   1. Lee: AUTOMATION_CHANGELOG.md" -ForegroundColor Cyan
Write-Host "   2. Cada cambio importante debe incluir:" -ForegroundColor Cyan
Write-Host "      - CHANGELOG_APP.md actualizado" -ForegroundColor Cyan
Write-Host "      - versionCode/versionName en build.gradle.kts" -ForegroundColor Cyan
Write-Host "   3. El hook pre-commit validará automáticamente" -ForegroundColor Cyan

Write-Host "`n✅ Automatización configurada correctamente!" -ForegroundColor Green
Write-Host "`n💡 Próximo paso: Lee AUTOMATION_CHANGELOG.md para entender el flujo" -ForegroundColor Cyan
