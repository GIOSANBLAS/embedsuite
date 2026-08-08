$ErrorActionPreference = 'Continue'
$project = "C:\Users\Administrator\AndroidStudioProjects\EMBEDSUITE"
Set-Location $project
$TMP = Join-Path $project "gradle-tmp"
New-Item -ItemType Directory -Force -Path $TMP | Out-Null

[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Android\Android Studio\jbr", "Process")
[Environment]::SetEnvironmentVariable("PATH", "$env:JAVA_HOME\bin;$env:LOCALAPPDATA\Android\Sdk\platform-tools;$env:PATH", "Process")
[Environment]::SetEnvironmentVariable("GRADLE_USER_HOME", (Join-Path $TMP "gradle-user-home"), "Process")
[Environment]::SetEnvironmentVariable("GIT_DIR", (Join-Path $TMP ".git_bypass"), "Process")
[Environment]::SetEnvironmentVariable("GIT_INDEX_FILE", (Join-Path $TMP "git_idx_bypass.idx"), "Process")
[Environment]::SetEnvironmentVariable("GIT_OPTIONAL_LOCKS", "0", "Process")
[Environment]::SetEnvironmentVariable("TEMP", $TMP, "Process")
[Environment]::SetEnvironmentVariable("TMP", $TMP, "Process")

$GRADLE_BAT = Join-Path $project "gradle-tmp\gradle-user-home\wrapper\dists\gradle-9.6.1-bin\4ticwg1pgcbps2hj28r8so764\gradle-9.6.1\bin\gradle.bat"
if (-not (Test-Path $GRADLE_BAT)) {
  Write-Error "Gradle unpacked missing: $GRADLE_BAT"
  exit 3
}

$LOG = Join-Path $TMP "gradle_direct.log"
$ERR = Join-Path $TMP "gradle_direct.err"
$MARK = Join-Path $TMP "gradle_direct.mark"
$APK = Join-Path $project "app\build\outputs\apk\debug\app-debug.apk"
Remove-Item $LOG,$ERR,$MARK -Force -ErrorAction SilentlyContinue
Remove-Item $APK -Force -ErrorAction SilentlyContinue

$ARGV = @(
  ":app:assembleDebug",
  "--no-daemon",
  "--console=plain",
  "--no-build-cache",
  "--stacktrace",
  "--project-dir",
  "`"$project`""
)

Write-Host "[LAUNCH] $(Get-Date -Format 'HH:mm:ss') DIRECT gradle.bat (no wrapper .bat) en $project" -ForegroundColor Cyan
Write-Host "  GRADLE=$GRADLE_BAT"
Write-Host "  LOG=$LOG"
Write-Host "  ERR=$ERR"

$p = Start-Process -FilePath $GRADLE_BAT -ArgumentList $ARGV `
   -WorkingDirectory $project -WindowStyle Hidden -PassThru `
   -RedirectStandardOutput $LOG -RedirectStandardError $ERR

Write-Host "  PID=$($p.Id)"

$sw = [System.Diagnostics.Stopwatch]::StartNew()
$printed = 0
$timeout = 2100

while ($sw.Elapsed.TotalSeconds -lt $timeout -and -not $p.HasExited) {
  Start-Sleep -Seconds 30
  $cur = (Get-Content $LOG -ErrorAction SilentlyContinue | Measure-Object -Line).Lines
  if ($cur -gt $printed) {
    Write-Host "--- t=$([int]$sw.Elapsed.TotalSeconds)s lines=$cur alive=$(-not $p.HasExited) ---" -ForegroundColor DarkCyan
    Get-Content $LOG -Tail 20
    $printed = $cur
  }
  if (Test-Path $APK) { Write-Host "  APK provisional" -ForegroundColor Green }
}

[int]$ec = if ($p.HasExited) { $p.ExitCode } else { 999 }
$ec | Set-Content $MARK
Write-Host "[END] exit=$ec t=$([int]$sw.Elapsed.TotalSeconds)s" -ForegroundColor $(if($ec -eq 0){"Green"}else{"Red"})

Write-Host ""
Write-Host "==== GRADLE LOG tail 500 ===="
Get-Content $LOG -ErrorAction SilentlyContinue -Tail 500
Write-Host ""
Write-Host "==== GRADLE ERR tail 200 ===="
Get-Content $ERR -ErrorAction SilentlyContinue -Tail 200

$adb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
$apkFile = Get-Item $APK -ErrorAction SilentlyContinue
if ($apkFile -and $ec -eq 0) {
  Write-Host "[APK OK] $($apkFile.FullName) size=$([math]::Round($apkFile.Length/1MB,2))MB $($apkFile.LastWriteTime)" -ForegroundColor Green
  Write-Host "--- adb devices ---" ; & $adb -s 8c00247c devices | Out-Host
  Write-Host "--- adb uninstall com.embedsuite.app ---" ; & $adb -s 8c00247c uninstall com.embedsuite.app 2>&1 | Out-Host
  Write-Host "--- adb install -r APK ---" ; & $adb -s 8c00247c install -r $apkFile.FullName 2>&1 | Out-Host
  Write-Host "--- dumpsys verify ---"
  & $adb -s 8c00247c shell pm dump com.embedsuite.app 2>&1 |
    Where-Object { $_ -match "versionCode=|versionName=|targetSdkVersion" } |
    Select-Object -First 6 | Out-Host
  exit 0
} elseif ($apkFile) {
  Write-Warning "APK existe pero exit=$ec. Revisa logs."
  exit $ec
} else {
  Write-Warning "APK no encontrado. Revisa gradle_direct.log."
  exit $ec
}
