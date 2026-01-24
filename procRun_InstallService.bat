@echo off
setlocal enabledelayedexpansion
set APP_PATH=%~dp0
cd "%APP_PATH%"
set WK_PATH=%cd%
set JAVA_HOME=%WK_PATH%\jre21
set SERVICE_NAME=demojetty12
set EXE_PATH=%WK_PATH%\demojetty12.exe
set JAR_FILE=demojetty12.jar

echo Installing %SERVICE_NAME% ...

"%EXE_PATH%" //IS//%SERVICE_NAME% ^
  --DisplayName="Demo Jetty 12" ^
  --Description="Jetty embedded running via Apache Procrun" ^
  --Startup=auto ^
  --StartMode=exe ^
  --StartPath="%WK_PATH%" ^
  --StartImage="%JAVA_HOME%\bin\java.exe" ^
  --StartParams=-Xmx512m;-jar;%JAR_FILE% ^
  --StopMode=jvm ^
  --LogPath="%WK_PATH%\logs" ^
  --LogPrefix=service ^
  --LogLevel=Info

echo.
echo Service installation complete.
pause
