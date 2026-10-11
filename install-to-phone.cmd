@echo off
REM ============================================================
REM  D-Tox - build the debug APK and install it on your phone
REM  Usage: connect the phone with USB debugging on, then
REM         double-click this file.
REM ============================================================
setlocal
set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
set "ADB=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
cd /d "%~dp0"

echo [1/3] Building debug APK...
call gradlew.bat :app:assembleDebug --console=plain
if errorlevel 1 goto :buildfail

echo.
echo [2/3] Connected devices:
"%ADB%" devices

echo.
echo [3/3] Installing on phone...
"%ADB%" install -r "app\build\outputs\apk\debug\app-debug.apk"
if errorlevel 1 goto :installfail

echo.
echo DONE - open D-Tox on your phone.
goto :end

:buildfail
echo.
echo BUILD FAILED - see the messages above.
goto :end

:installfail
echo.
echo INSTALL FAILED - is the phone connected and USB debugging allowed?
echo Check that the list above shows your phone as "device" (not "unauthorized").
goto :end

:end
pause
