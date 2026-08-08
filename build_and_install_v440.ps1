$ErrorActionPreference = 'Continue'
$PROJECT = "C:\Users\Administrator\AndroidStudioProjects\EMBEDSUITE"
Set-Location $PROJECT

$TMP = Join-Path $PROJECT "gradle-tmp"
New-Item -ItemType Directory -Force -Path $TMP | Out-Null

[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Android\Android Studio\jbr", "Process")
[Environment]::SetEnvironmentVariable("PATH", "$env:JAVA_HOME\bin;$env:LOCALAPPDATA\Android\Sdk\platform-tools;$env:PATH", "Process")
[Environment]::SetEnvironmentVariable("GRADLE_USER_HOME", (Join-Path $TMP "gradle-user-home"), "Process")
[Environment]::SetEnvironmentVariable("TEMP", $TMP, "Process")
[Environment]::SetEnvironmentVariable("TMP", $TMP, "Process")
[Environment]::SetEnvironmentVariable("GIT_DIR", (Join-Path $TMP ".git_bypass"), "Process")
[Environment]::SetEnvironmentVariable("GIT_INDEX_FILE", (Join-Path $TMP "git_idx_bypass.idx"), "Process")
[Environment]::SetEnvironmentVariable("GIT_OPTIONAL_LOCKS", "0", "Process")

$STD  = Join-Path $TMP "bg_assemble_std.log"
$ERR  = Join-Path $TMP "bg_assemble_err.log"
$MARK = Join-Path $TMP "bg_assemble_done.mark"
Remove-Item $STD,$ERR,$MARK -Force -ErrorAction SilentlyContinue

$cmdLine = "& '.\gradlew.bat' :app:assembleDebug --no-daemon --console=plain --no-build-cache '-Dorg.gradle.vfs.watch=false' '-Dorg.gradle.file.watching=false' --stacktrace *> '$STD'; `$LASTEXITCODE | Out-File -FilePath '$MARK' -Encoding utf8"

$bytes = [System.Text.Encoding]::Unicode.GetBytes($cmdLine)
$encoded = [Convert]::ToBase64String($bytes)

$job = Start-Job -ScriptBlock {
  param($proj, $encodedCmd)
  Set-Location $proj
  $decoded = [System.Text.Encoding]::Unicode.GetString([Convert]::FromBase64String($encodedCmd))
  Invoke-Expression $decoded
} -ArgumentList $PROJECT, $encoded -Name "EMBED_APK_V440"

Write-Host "[JOB STARTED] id=$($job.Id) name=$($job.Name)" -ForegroundColor Cyan
Write-Host "[JOB STD  ] $STD"
Write-Host "[JOB MARK ] $MARK (aparece al terminar con exit code)"

$stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
$timeoutSec = 900
$apkPath = Join-Path $PROJECT "app\build\outputs\apk\debug\app-debug.apk"
$lastLines = 0

while ($stopwatch.Elapsed.TotalSeconds -lt $timeoutSec) {
  Start-Sleep -Seconds 30
  $state = (Get-Job -Id $job.Id -ErrorAction SilentlyContinue).State
  if (Test-Path $STD) {
    $total = (Get-Content $STD -ErrorAction SilentlyContinue | Measure-Object -Line).Lines
    if ($total -gt $lastLines) {
      Write-Host "--- t=$([int]$stopwatch.Elapsed.TotalSeconds)s state=$state lines=$total ---" -ForegroundColor DarkCyan
      Get-Content $STD -Tail 20
      $lastLines = $total
    }
  }
  if (Test-Path $MARK) {
    Write-Host ">>> JOB FINALIZADO. mark found" -ForegroundColor Green
    break
  }
  if (Test-Path $apkPath) {
    Write-Host ">>> APK YA EXISTE: $apkPath" -ForegroundColor Green
    break
  }
  if ($state -in @('Completed','Failed','Stopped','Suspended','Disconnected')) {
    Write-Host ">>> JOB STATE=$state" -ForegroundColor Yellow
    break
  }
}

Write-Host "--- Recibir Job $($job.Id) ---"
Receive-Job -Id $job.Id -Keep -ErrorAction SilentlyContinue
Remove-Job -Id $job.Id -Force -ErrorAction SilentlyContinue

Write-Host "--- STD TAIL 120 ---"
Get-Content $STD -Tail 120 -ErrorAction SilentlyContinue
Write-Host "--- ERR TAIL 60 ---"
Get-Content $ERR -Tail 60 -ErrorAction SilentlyContinue

$apk = Get-ChildItem (Join-Path $PROJECT "app\build\outputs\apk\debug") -Filter "*.apk" -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $apk) {
  Write-Warning "!!! APK NO ENCONTRADO EN app\build\outputs\apk\debug !!!"
  Get-ChildItem app\build -Recurse -Filter "*.apk" -ErrorAction SilentlyContinue | Select-Object FullName,Length
  exit 2
}

Write-Host "[FINAL APK OK] $($apk.FullName)" -ForegroundColor Green
Write-Host "             Size=$($apk.Length) LastWrite=$($apk.LastWriteTime)" -ForegroundColor Green

$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
Write-Host "--- adb devices ---"
& $adb -s 8c00247c devices
Write-Host "--- uninstall old ---"
& $adb -s 8c00247c uninstall com.embedsuite.app 2>&1
Write-Host "--- install -r ---"
& $adb -s 8c00247c install -r $apk.FullName 2>&1
Write-Host "--- verify version ---"
& $adb -s 8c00247c shell cmd package dump com.embedsuite.app 2>&1 |
  Where-Object { $_ -match "versionCode|versionName" } |
  Select-Object -First 3
exit 0
