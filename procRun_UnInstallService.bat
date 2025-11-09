@echo off
setlocal enabledelayedexpansion

::---------------------------------------------
:: ตั้งค่าชื่อ service และ path
::---------------------------------------------
set SERVICE_NAME=demojetty12
set EXE_PATH=%~dp0demojetty12.exe

echo remove service %SERVICE_NAME% ...
"%EXE_PATH%" //DS//%SERVICE_NAME%

echo.
echo Service uninstallation complete.
pause