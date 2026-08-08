$ErrorActionPreference = 'Continue'
$PROJECT = "C:\Users\Administrator\AndroidStudioProjects\EMBEDSUITE"
Set-Location $PROJECT
$TMP = Join-Path $PROJECT "gradle-tmp"
New-Item -ItemType Directory -Force -Path $TMP | Out-Null

[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Android\Android Studio\jbr", "Process")
[Environment]::SetEnvironmentVariable("PATH", "$env:JAVA_HOME\bin;$env:LOCALAPPDATA\Android\Sdk\platform-tools;$env:PATH", "Process")
[Environment]::SetEnvironmentVariable("GRADLE_USER_HOME", (Join-Path $TMP "gradle-user-home"), "Process")
[Environment]::SetEnvironmentVariable("GIT_DIR", (Join-Path $TMP ".git_bypass"), "Process")
[Environment]::SetEnvironmentVariable("GIT_INDEX_FILE", (Join-Path $TMP "git_idx_bypass.idx"), "Process")
[Environment]::SetEnvironmentVariable("GIT_OPTIONAL_LOCKS", "0", "Process")
[Environment]::SetEnvironmentVariable("TEMP", $TMP, "Process")
[Environment]::SetEnvironmentVariable("TMP", $TMP, "Process")

$LOG  = Join-Path $TMP "gradle_build.log"
$ERR  = Join-Path $TMP "gradle_err.log"
$MARK = Join-Path $TMP "gradle_done.mark"
$APK  = Join-Path $PROJECT "app\build\outputs\apk\debug\app-debug.apk"
Remove-Item $LOG,$ERR,$MARK -Force -ErrorAction SilentlyContinue
Remove-Item $APK -Force -ErrorAction SilentlyContinue

$ARGV = @(
  ":app:assembleDebug",
  "--no-daemon",
  "--console=plain",
  "--no-build-cache",
  "--stacktrace"
)

Write-Host "[LAUNCH] $(Get-Date -Format 'HH:mm:ss') gradlew.bat en modo WindowStyle Hidden (sin TTY) en $PROJECT" -ForegroundColor Cyan
Write-Host "  LOG = $LOG"
Write-Host "  ERR = $ERR"
Write-Host "  APK = $APK"
Write-Host ""

$p = Start-Process -FilePath (Join-Path $PROJECT "gradlew.bat") `
   -ArgumentList $ARGV `
   -WorkingDirectory $PROJECT `
   -WindowStyle Hidden `
   -RedirectStandardOutput $LOG `
   -RedirectStandardError $ERR `
   -PassThru
Write-Host "  Process ID: $($p.Id)"

$sw = [System.Diagnostics.Stopwatch]::StartNew()
$printed = 0
$timeoutSec = 2100

while ($sw.Elapsed.TotalSeconds -lt $timeoutSec -and -not $p.HasExited) {
  Start-Sleep -Seconds 30
  $lines = (Get-Content $LOG -ErrorAction SilentlyContinue | Measure-Object -Line).Lines
  if ($lines -gt $printed) {
    Write-Host "--- t=$([int]$sw.Elapsed.TotalSeconds)s PID=$($p.Id) alive log_lines=$lines ---" -ForegroundColor DarkCyan
    Get-Content $LOG -ErrorAction SilentlyContinue -Tail 18
    $printed = $lines
  }
  if (Test-Path $APK) {
    Write-Host "  + APK provisional detected at t=$([int]$sw.Elapsed.TotalSeconds)s" -ForegroundColor Green
  }
}

if (-not $p.HasExited) {
  Write-Warning "[TIMEOUT] matando proceso Gradle PID=$($p.Id)"
  Stop-Process -Id $p.Id -Force -ErrorAction SilentlyContinue
}
[int]$exitCode = if ($p.HasExited) { $p.ExitCode } else { 99 }
$exitCode | Set-Content -Path $MARK -Encoding ASCII
Write-Host ""
Write-Host "[FINISHED] exit=$exitCode  t=$([int]$sw.Elapsed.TotalSeconds)s" -ForegroundColor $(if($exitCode -eq 0){"Green"}else{"Red"})

Write-Host ""
Write-Host "==== FINAL GRADLE LOG tail 500 ===="
Get-Content $LOG -ErrorAction SilentlyContinue -Tail 500
Write-Host ""
Write-Host "==== FINAL ERROR LOG tail 200 ===="
Get-Content $ERR -ErrorAction SilentlyContinue -Tail 200

$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
$apkFile = Get-Item $APK -ErrorAction SilentlyContinue
if ($apkFile -and $exitCode -eq 0) {
  Write-Host ""
  Write-Host "[APK OK] $($apkFile.FullName) size=$([math]::Round($apkFile.Length / 1MB, 2)) MB  modified=$($apkFile.LastWriteTime)" -ForegroundColor Green
  Write-Host ""
  Write-Host "--- adb devices ---"
  & $adb -s 8c00247c devices | Out-Host
  Write-Host "--- adb uninstall com.embedsuite.app ---"
  & $adb -s 8c00247c uninstall com.embedsuite.app 2>&1 | Out-Host
  Write-Host "--- adb install -r APK ---"
  & $adb -s 8c00247c install -r $apkFile.FullName 2>&1 | Out-Host
  Write-Host "--- dumpsys package version ---"
  & $adb -s 8c00247c shell pm dump com.embedsuite.app 2>&1 |
    Where-Object { $_ -match "versionCode=|versionName=|targetSdkVersion" } |
    Select-Object -First 6 | Out-Host
  exit 0
}
elseif ($apkFile) {
  Write-Warning "[APK] Existe, pero exit code Gradle=$exitCode. Revisa log."
  exit $exitCode
}
else {
  Write-Warning "[NO APK ENCONTRADO] $APK."
  Get-ChildItem (Join-Path $PROJECT "app\build") -Recurse -Filter "*.apk" -ErrorAction SilentlyContinue |
    Select-Object -First 5 FullName, Length | Out-Host
  exit $exitCode
}
