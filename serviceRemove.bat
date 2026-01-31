@echo off
set APP_DIR=%~dp0
cd /d "%APP_DIR%"
call serviceInstall.bat remove
