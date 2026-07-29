@echo off
echo Building Gesture Drawing Studio Native Bundle...

REM 1. Compile and package fat JAR
call "C:\Users\Amanda\.gemini\antigravity\tools\apache-maven-3.9.9\bin\mvn.cmd" clean package

REM 2. Prepare staging directory
if exist "target\installer-input" rmdir /s /q "target\installer-input"
if exist "target\dist" rmdir /s /q "target\dist"
mkdir "target\installer-input"

copy "target\gesture-drawing-app-1.0.0-SNAPSHOT.jar" "target\installer-input\gesture-drawing-studio.jar"

REM 3. Package zero-dependency native Windows bundle with embedded JRE
jpackage --type app-image ^
  --name "Gesture Drawing Studio" ^
  --input target/installer-input ^
  --main-jar gesture-drawing-studio.jar ^
  --main-class com.gesturedrawing.Launcher ^
  --dest target/dist

echo Native Windows standalone bundle created in target/dist/Gesture Drawing Studio!
