@echo off
title Gesture Drawing Studio
cd /d "%~dp0"
set "APP_JAR=target\gesture-drawing-app-1.0.0-SNAPSHOT.jar"
if not exist "%APP_JAR%" (
    echo Building application jar...
    call mvn -q -B package -DskipTests || (echo Build failed. & pause & exit /b 1)
)
start "" javaw -jar "%APP_JAR%"
