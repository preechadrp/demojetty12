@echo off
setlocal EnableExtensions

REM =========================
REM Get app directory
REM =========================
set "APP_HOME=%~dp0"

REM Remove trailing backslash safely
if "%APP_HOME:~-1%"=="\" set "APP_HOME=%APP_HOME:~0,-1%"

cd /d "%APP_HOME%"

REM =========================
REM Ensure log directory
REM =========================
if not exist "%APP_HOME%\logs" (
    mkdir "%APP_HOME%\logs"
)

REM =========================
REM Java (Bundled JRE)
REM =========================
set "JAVA_HOME=%APP_HOME%\jre"
set "JAVA_BIN=%JAVA_HOME%\bin\java.exe"

REM =========================
REM Paths
REM =========================
set "LOG_DIR=%APP_HOME%\logs"

REM =========================
REM JVM Options
REM =========================
set "JAVA_OPTS=-Xms1g -Xmx2g"
set "JAVA_OPTS=%JAVA_OPTS% -XX:+HeapDumpOnOutOfMemoryError"
rem บรรทัดนี้ต้องเขียนแตกต่างจากบรรทัดอื่น
set JAVA_OPTS=%JAVA_OPTS% -XX:HeapDumpPath="%LOG_DIR%"
set "JAVA_OPTS=%JAVA_OPTS% -Dfile.encoding=UTF-8"
set "JAVA_OPTS=%JAVA_OPTS% -XX:+ExitOnOutOfMemoryError"

REM =========================
REM Run
REM =========================
"%JAVA_BIN%" %JAVA_OPTS% -jar "%APP_HOME%\demojetty12.jar"

endlocal