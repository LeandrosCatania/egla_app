@echo off
echo.
echo ==============================================
echo  EGLA Location Tracker - IP Address Finder
echo ==============================================
echo.
echo This script helps you find your computer's IP address
echo for configuring physical Android devices.
echo.

echo Finding your network interfaces...
echo.

for /f "tokens=2 delims=:" %%i in ('ipconfig ^| findstr /i "IPv4"') do (
    set ip=%%i
    set ip=!ip: =!
    if not "!ip!"=="127.0.0.1" (
        echo Found IP Address: !ip!
        echo.
        echo To configure your Android app:
        echo 1. Open: app\src\main\java\com\example\eglatracker\utils\DatabaseLogger.kt
        echo 2. Change this line:
        echo    private val baseUrl = "http://10.0.2.2:3000"
        echo    to:
        echo    private val baseUrl = "http://!ip!:3000"
        echo.
        echo 3. Rebuild and run your Android app
        echo.
    )
)

echo Your API server will be accessible at:
for /f "tokens=2 delims=:" %%i in ('ipconfig ^| findstr /i "IPv4"') do (
    set ip=%%i
    set ip=!ip: =!
    if not "!ip!"=="127.0.0.1" (
        echo   http://!ip!:3000
    )
)

echo.
echo ==============================================
echo Make sure both your computer and Android device
echo are connected to the same WiFi network!
echo ==============================================
echo.
pause 