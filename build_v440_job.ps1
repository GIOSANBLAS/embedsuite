$ErrorActionPreference = 'Continue'
$PROJECT = "C:\Users\Administrator\AndroidStudioProjects\EMBEDSUITE"
Set-Location $PROJECT
$TMP = Join-Path $PROJECT "gradle-tmp"
New-Item -ItemType Directory -Force -Path $TMP | Out-Null
$OUT = Join-Path $TMP "job_std.log"
$ERR = Join-Path $TMP "job_err.log"
$MARK = Join-Path $TMP "job_done.mark"
$APK_PATH = Join-Path $PROJECT "app\build\outputs\apk\debug\app-debug.apk"
Remove-Item $OUT,$ERR,$MARK -Force -ErrorAction SilentlyContinue
Remove-Item $APK_PATH -Force -ErrorAction SilentlyContinue

$sb = {
  param($proj, $out, $err, $mark)
  Set-Location $proj
  & ".\gradlew_run.cmd" $out $err $mark ":app:assembleDebug" "--no-daemon" "--console=plain" "--no-build-cache" "--stacktrace"
  return $LASTEXITCODE
}

Write-Host "[Start-Job begin $(Get-Date -Format 'HH:mm:ss')] assembleDebug (sala for APK $APK_PATH" -ForegroundColor Cyan
$job = Start-Job -ScriptBlock $sb -Name "EMBED_V440" -ArgumentList $PROJECT,$OUT,$ERR,$MARK
Write-Host "  Job.Id=$($job.Id)"

$sw = [System.Diagnostics.Stopwatch]::StartNew()
$printed = 0
$timeoutSec = 1200
while ($sw.Elapsed.TotalSeconds -lt $timeoutSec) {
  Start-Sleep -Seconds 30
  $j = Get-Job -Id $job.Id
  Write-Host "  t=$([int]$sw.Elapsed.TotalSeconds)s state=$($j.State)" -ForegroundColor DarkCyan
  if (Test-Path $OUT) {
    $cur = (Get-Content $OUT -ErrorAction SilentlyContinue | Measure-Object -Line).Lines
    if ($cur -gt $printed) {
      Get-Content $OUT -Tail 18
      $printed = $cur
    }
  }
  if (Test-Path $MARK) { break }
  if (Test-Path $APK_PATH) { Write-Host "  APK detected!" -ForegroundColor Green; break }
  if ($j.State -in @('Completed','Failed','Stopped')) { break }
}

Write-Host "--- collect job $($job.Id) state=$((Get-Job -Id $job.Id).State" -ForegroundColor Cyan
Receive-Job -Id $job.Id -Keep -ErrorAction SilentlyContinue
Remove-Job -Id $job.Id -Force -ErrorAction SilentlyContinue

Write-Host "--- STD tail 220 ---"
Get-Content $OUT -ErrorAction SilentlyContinue -Tail 220
Write-Host "--- ERR tail 120 ---"
Get-Content $ERR -ErrorAction SilentlyContinue -Tail 120
Write-Host "--- MARK ---"
if (Test-Path $MARK) { Write-Host "exit=$(Get-Content $MARK)" } else { Write-Host "(not found)" }

$apk = Get-ChildItem (Join-Path $PROJECT "app\build\outputs\apk\debug") -Filter "*.apk" -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $apk) {
  Write-Warning "APK NOT FOUND"
  exit 2
}
Write-Host "APK: $($apk.FullName) size=$($apk.Length) date=$($apk.LastWriteTime)" -ForegroundColor Green

$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
Write-Host ">>> adb devices"
& $adb -s 8c00247c devices
Write-Host ">>> adb uninstall com.embedsuite.app"
& $adb -s 8c00247c uninstall com.embedsuite.app 2>&1 | Out-Host
Write-Host ">>> adb install"
& $adb -s 8c00247c install -r $apk.FullName 2>&1 | Out-Host
Write-Host ">>> adb dumpsys package"
& $adb -s 8c00247c shell pm dump com.embedsuite.app 2>&1 |
  Where-Object { $_ -match "versionCode|versionName|targetSdkVersion" } |
  Select-Object -First 6 | Out-Host
exit 0
