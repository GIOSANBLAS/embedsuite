setlocal enableextensions disabledelayedexpansion
if "%~1"=="" goto usage
set "MARK_OUT=%~1"
set "MARK_ERR=%~2"
set "MARK_DONE=%~3"
shift /3
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
set PATH=%JAVA_HOME%\bin;%PATH%
set TEMP=%~dp0gradle-tmp
set TMP=%~dp0gradle-tmp
set GRADLE_USER_HOME=%~dp0gradle-tmp\gradle-user-home
set GIT_DIR=%~dp0gradle-tmp\.git_bypass
set GIT_INDEX_FILE=%~dp0gradle-tmp\git_idx_bypass.idx
set GIT_OPTIONAL_LOCKS=0
if not exist "%TEMP%" mkdir "%TEMP%"
if not exist "%GRADLE_USER_HOME%" mkdir "%GRADLE_USER_HOME%"
cd /d "%~dp0"
call "%~dp0gradlew.bat" %* > "%MARK_OUT%" 2> "%MARK_ERR%"
echo %ERRORLEVEL% > "%MARK_DONE%"
exit /b %ERRORLEVEL%
:usage
echo USAGE: gradlew_run.cmd out.log err.log done.mark [gradlew args...]
exit /b 2
