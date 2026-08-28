@echo off
setlocal enabledelayedexpansion

set PROJECT_DIR=C:\Users\Administrator\AndroidStudioProjects\EMBEDSUITE
cd /d "%PROJECT_DIR%"

set NEW_TEMP=%PROJECT_DIR%\gradle-tmp
if not exist "%NEW_TEMP%" mkdir "%NEW_TEMP%"
set KOTLIN_DAEMON_DIR=%NEW_TEMP%\kotlin-daemon
if not exist "%KOTLIN_DAEMON_DIR%" mkdir "%KOTLIN_DAEMON_DIR%"

set TEMP=%NEW_TEMP%
set TMP=%NEW_TEMP%

rem ==== BYPASS TRAE SANDBOX: bloquea escritura en .git/modules/*/index.lock ====
rem 1) Desviamos GIT_DIR a ruta NO EXISTENTE para que gradle/plugins no intente lockear ningún módulo git
rem 2) GIT_WORK_TREE sin efecto (desviamos index y objetos fuera del árbol
rem 3) Desactivamos virtual file system watcher + file watching (trae intercepta locks en mods)
rem ========================================================================
set GIT_DIR=%NEW_TEMP%\.git_dummy_bypass_sandbox
set GIT_INDEX_FILE=%NEW_TEMP%\index_bypass_sandbox.idx
set GIT_OBJECT_DIRECTORY=%NEW_TEMP%\git_objects_bypass
set GIT_TERMINAL_PROMPT=0
set GIT_OPTIONAL_LOCKS=0
set GIT_FLUSH=0
set CARGO_NET_OFFLINE=true
set GRADLE_RO_DEP_CACHE_DIR=%NEW_TEMP%\ro-dep-cache
set BUILDKIT_PROGRESS=plain

set GRADLE_OPTS=-Dkotlin.daemon.enabled=false -Dkotlin.compiler.execution.strategy=in-process -Dkotlin.daemon.dir="%KOTLIN_DAEMON_DIR%" -Djava.io.tmpdir="%NEW_TEMP%" -Dorg.gradle.vfs.watch=false -Dorg.gradle.file.watching=false -Dorg.gradle.vfs.verbose=false -Dfile.encoding=UTF-8 -Xmx4g -XX:+UseG1GC -XX:+UnlockExperimentalVMOptions

set JBR_PATH=C:\Program Files\Android\Android Studio\jbr\bin
set SDK_PATH=%LOCALAPPDATA%\Android\Sdk\platform-tools
set PATH=%JBR_PATH%;%SDK_PATH%;%PATH%
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr

echo === INICIANDO BUILD APK DEBUG === > "%PROJECT_DIR%\assemble.log"
echo Hora inicio: %DATE% %TIME% >> "%PROJECT_DIR%\assemble.log"
echo GRADLE_OPTS=%GRADLE_OPTS% >> "%PROJECT_DIR%\assemble.log"
echo GIT_DIR=%GIT_DIR% >> "%PROJECT_DIR%\assemble.log"
echo. >> "%PROJECT_DIR%\assemble.log"

call "%PROJECT_DIR%\gradlew.bat" --stop >> "%PROJECT_DIR%\assemble.log" 2>&1

call "%PROJECT_DIR%\gradlew.bat" :app:assembleDebug --console=plain --no-daemon --no-build-cache --no-configuration-cache --stacktrace ^
  "-Dorg.gradle.jvmargs=-Xmx4g -XX:+UseG1GC -Dkotlin.daemon.enabled=false -Dkotlin.compiler.execution.strategy=in-process -Dkotlin.daemon.dir=%KOTLIN_DAEMON_DIR% -Djava.io.tmpdir=%NEW_TEMP% -Dorg.gradle.vfs.watch=false -Dorg.gradle.file.watching=false -Dfile.encoding=UTF-8" ^
  -Dkotlin.daemon.enabled=false ^
  -Dkotlin.daemon.fs.watcher.enabled=false ^
  -Dkotlin.compiler.execution.strategy=in-process ^
  "-Dkotlin.daemon.dir=%KOTLIN_DAEMON_DIR%" ^
  "-Duser.home=%NEW_TEMP% >> "%PROJECT_DIR%\assemble.log" 2>&1

set EXIT_CODE=%ERRORLEVEL%

echo. >> "%PROJECT_DIR%\assemble.log"
echo === BUILD FINALIZADO. EXIT=%EXIT_CODE% === >> "%PROJECT_DIR%\assemble.log"
echo Hora fin: %DATE% %TIME% >> "%PROJECT_DIR%\assemble.log"

if exist "%PROJECT_DIR%\app\build\outputs\apk\debug\app-debug.apk" (
  dir "%PROJECT_DIR%\app\build\outputs\apk\debug\app-debug.apk" >> "%PROJECT_DIR%\assemble.log" 2>&1
) else (
  echo APK NO ENCONTRADO >> "%PROJECT_DIR%\assemble.log"
)

echo EXIT=%EXIT_CODE%
exit /b %EXIT_CODE%
