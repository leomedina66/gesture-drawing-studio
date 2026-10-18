@echo off
setlocal
cd /d "%~dp0"
echo Building Gesture Drawing Studio Native Bundle...

set "APP_NAME=Gesture Drawing Studio"
set "APP_VERSION=1.1.0"
REM Modules the app needs (from: jdeps --ignore-missing-deps --multi-release 21 --print-module-deps <jar>)
set "APP_MODULES=java.base,java.desktop,java.prefs,jdk.jfr,jdk.unsupported"

REM Pick the jpackage that also provides the bundled runtime: JPACKAGE_JDK if set, else the one on PATH
set "JPACKAGE=jpackage"
if defined JPACKAGE_JDK set "JPACKAGE=%JPACKAGE_JDK%\bin\jpackage.exe"
for /f "delims=" %%v in ('"%JPACKAGE%" --version') do set "JPACKAGE_VERSION=%%v"
if not defined JPACKAGE_VERSION (echo jpackage not found. Install a JDK 21+ or set JPACKAGE_JDK. & exit /b 1)
echo Using jpackage %JPACKAGE_VERSION% (its JDK becomes the bundled runtime)

REM 1. Compile, test and package fat JAR (mvn must be on PATH)
call mvn -B clean package || (echo Maven build failed. & exit /b 1)

REM 2. Stage the jar
if exist "target\installer-input" rmdir /s /q "target\installer-input"
if exist "target\jpackage-out" rmdir /s /q "target\jpackage-out"
mkdir "target\installer-input"
copy /y "target\gesture-drawing-studio.jar" "target\installer-input\gesture-drawing-studio.jar" >nul

REM 3. Package a native Windows app with a trimmed embedded runtime (into target first)
"%JPACKAGE%" --type app-image ^
  --name "%APP_NAME%" ^
  --app-version %APP_VERSION% ^
  --vendor "%APP_NAME%" ^
  --description "Timed gesture drawing practice with reference images" ^
  --icon packaging\app-icon.ico ^
  --input target\installer-input ^
  --main-jar gesture-drawing-studio.jar ^
  --main-class com.gesturedrawing.Launcher ^
  --add-modules %APP_MODULES% ^
  --jlink-options "--strip-native-commands --strip-debug --no-man-pages --no-header-files" ^
  --dest target\jpackage-out || (echo jpackage failed. The previous app in dist\ was left untouched. & exit /b 1)

REM 4. Only now replace the installed copy in dist\ (the desktop shortcut points there)
if exist "dist\%APP_NAME%" rmdir /s /q "dist\%APP_NAME%"
if exist "dist\%APP_NAME%" (echo Could not replace dist\%APP_NAME% - close the running app and try again. New build: target\jpackage-out & exit /b 1)
if not exist dist mkdir dist
move "target\jpackage-out\%APP_NAME%" "dist\" >nul || (echo Could not move the new build into dist\. & exit /b 1)

echo Native Windows standalone bundle created in dist\%APP_NAME%
