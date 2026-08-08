$ErrorActionPreference = 'Continue'
$PROJECT = "C:\Users\Administrator\AndroidStudioProjects\EMBEDSUITE"
Set-Location $PROJECT

$TMP = Join-Path $PROJECT "gradle-tmp"
New-Item -ItemType Directory -Force -Path $TMP, (Join-Path $TMP "kotlin-daemon"), (Join-Path $TMP "gradle-user-home"), (Join-Path $TMP "user-home") | Out-Null

[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Android\Android Studio\jbr", "Process")
[Environment]::SetEnvironmentVariable("PATH", "$env:JAVA_HOME\bin;$env:LOCALAPPDATA\Android\Sdk\platform-tools;$env:PATH", "Process")
[Environment]::SetEnvironmentVariable("TEMP", $TMP, "Process")
[Environment]::SetEnvironmentVariable("TMP", $TMP, "Process")
[Environment]::SetEnvironmentVariable("TMPDIR", $TMP, "Process")
[Environment]::SetEnvironmentVariable("GRADLE_USER_HOME", (Join-Path $TMP "gradle-user-home"), "Process")
[Environment]::SetEnvironmentVariable("USERPROFILE", (Join-Path $TMP "user-home"), "Process")
[Environment]::SetEnvironmentVariable("HOME", (Join-Path $TMP "user-home"), "Process")

# ======================================================
# 1) BYPASS PRINCIPAL · TRAE BLOQUEA .git/modules/*/index.lock
#   → redirigimos GIT_DIR fuera del arbol del proyecto (no existe, gradle no encontrará ningún submodulo para lockear)
# ======================================================
[Environment]::SetEnvironmentVariable("GIT_DIR", (Join-Path $TMP ".git_bypass"), "Process")
[Environment]::SetEnvironmentVariable("GIT_INDEX_FILE", (Join-Path $TMP "git_idx_bypass.idx"), "Process")
[Environment]::SetEnvironmentVariable("GIT_OBJECT_DIRECTORY", (Join-Path $TMP "git_objects_bypass"), "Process")
[Environment]::SetEnvironmentVariable("GIT_COMMON_DIR", (Join-Path $TMP ".git_bypass"), "Process")
[Environment]::SetEnvironmentVariable("GIT_TERMINAL_PROMPT", "0", "Process")
[Environment]::SetEnvironmentVariable("GIT_OPTIONAL_LOCKS", "0", "Process")
[Environment]::SetEnvironmentVariable("GIT_FLUSH", "0", "Process")

# 2) Desactivar VFS watcher / file watcher / daemon fs watcher
$env:GRADLE_OPTS = "-Xmx4g -XX:+UseG1GC -XX:+UnlockExperimentalVMOptions " + `
                   "-Dkotlin.daemon.enabled=false " + `
                   "-Dkotlin.daemon.fs.watcher.enabled=false " + `
                   "-Dkotlin.compiler.execution.strategy=in-process " + `
                   "-Dkotlin.daemon.dir=$(Join-Path $TMP 'kotlin-daemon') " + `
                   "-Djava.io.tmpdir=$TMP " + `
                   "-Dorg.gradle.vfs.watch=false " + `
                   "-Dorg.gradle.file.watching=false " + `
                   "-Dorg.gradle.vfs.verbose=false " + `
                   "-Dfile.encoding=UTF-8"

$ARGS = @(
  ":app:assembleDebug",
  "--no-daemon",
  "--no-build-cache",
  "--no-configuration-cache",
  "--stacktrace",
  "--console=plain",
  "-Dorg.gradle.parallel=true"
)

Write-Host ">>> START $(Get-Date -Format 'HH:mm:ss')" -ForegroundColor Cyan
$stdout = Join-Path $TMP "gradle_stdout.log"
$stderr = Join-Path $TMP "gradle_stderr.log"

$p = Start-Process -FilePath (Join-Path $PROJECT "gradlew.bat") -ArgumentList $ARGS `
   -WorkingDirectory $PROJECT -RedirectStandardOutput $stdout -RedirectStandardError $stderr `
   -Wait -PassThru -NoNewWindow

Write-Host ">>> END $(Get-Date -Format 'HH:mm:ss') EXIT=$($p.ExitCode)" -ForegroundColor ($(if($p.ExitCode -eq 0){"Green"}else{"Red"}))
Write-Host "---STDOUT tail---"
Get-Content $stdout -Tail 80
Write-Host "---STDERR tail---"
Get-Content $stderr -Tail 60

$apk = Get-ChildItem (Join-Path $PROJECT "app\build\outputs\apk\debug") -Filter "*.apk" -ErrorAction SilentlyContinue | Select-Object -First 1
if ($apk) {
  Write-Host "---APK OK $($apk.FullName) size=$($apk.Length)---" -ForegroundColor Green
  $adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
  & $adb -s 8c00247c install -r $apk.FullName
  & $adb -s 8c00247c shell cmd package dump com.embedsuite.app 2>&1 | Select-String "versionCode|versionName" | Select-Object -First 3
} else {
  Write-Host "---APK NOT FOUND in app\build\outputs\apk\debug---" -ForegroundColor Red
}
exit $p.ExitCode
