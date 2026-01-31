@echo off
setlocal
REM https://github.com/dkxce/NSSM
REM SERVICE_NAME ห้ามมี space
set SERVICE_NAME=DemoJetty12
set SERVICE_DISPLAYNAME=Demo Jetty Service
REM ต้องรัน Admin
net session >nul 2>&1
if %errorlevel% neq 0 (
    echo Please run as Administrator
    pause
    exit /b
)

set APP_DIR=%~dp0
if "%APP_DIR:~-1%"=="\" set APP_DIR=%APP_DIR:~0,-1%

cd /d "%APP_DIR%"

if /I "%~1"=="remove" (
    echo Uninstalling service...

    nssm stop "%SERVICE_NAME%" >nul 2>&1
    nssm remove "%SERVICE_NAME%" confirm

    echo Done.
    goto :END
)

echo Installing service...

nssm install "%SERVICE_NAME%" "%APP_DIR%\serviceScript.bat"
nssm set "%SERVICE_NAME%" AppDirectory "%APP_DIR%"
nssm set "%SERVICE_NAME%" DisplayName "%SERVICE_DISPLAYNAME%"
nssm set "%SERVICE_NAME%" AppRestartDelay 5000
nssm set "%SERVICE_NAME%" AppStopMethodConsole 3000
REM nssm set "%SERVICE_NAME%" AppStdout "%APP_DIR%\logs\%SERVICE_NAME%.out.log"
nssm set "%SERVICE_NAME%" AppStderr "%APP_DIR%\logs\%SERVICE_NAME%.err.log"

nssm start "%SERVICE_NAME%"

echo Done.

:END
pause
endlocal