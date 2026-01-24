@echo off
setlocal enabledelayedexpansion
set APP_PATH=%~dp0
cd /d "%APP_PATH%"
set WK_PATH=%cd%
set SERVICE_NAME=demojetty12
set EXE_PATH=%WK_PATH%\demojetty12.exe
set JAR_FILE=demojetty12.jar;./lib/*
set START_CLASS=com.example.Main

REM === ensure log directory exists ===
if not exist "%WK_PATH%\logs" (
    mkdir "%WK_PATH%\logs"
)

echo Installing %SERVICE_NAME% ...

"%EXE_PATH%" //IS//%SERVICE_NAME% ^
        --DisplayName="Demo Jetty 12" ^
        --Description="Jetty embedded running via Apache Procrun" ^
        --Install="%EXE_PATH%" ^
        --Jvm="%WK_PATH%\jre\bin\server\jvm.dll" --StartPath="%WK_PATH%" ^
        --JvmOptions=-Xmx512m;-Xrs ^
        --Startup=auto --StartMode=jvm --StopMode=jvm ^
        --Classpath="%JAR_FILE%" ^
        --StartClass=%START_CLASS% --StartMethod startApp ^
        --StopClass=%START_CLASS% --StopMethod stopApp ^
        --LogPath="%WK_PATH%/logs" --LogPrefix=service --LogLevel=Info
echo.
echo Service installation complete.
pause
        